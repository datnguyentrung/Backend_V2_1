package com.dat.backend_v2_1.service.Core;

import com.dat.backend_v2_1.domain.Core.Coach;
import com.dat.backend_v2_1.dto.Core.CoachReqDTO;
import com.dat.backend_v2_1.dto.Core.CoachResDTO;
import com.dat.backend_v2_1.enums.Core.CoachStatus;
import com.dat.backend_v2_1.mapper.Core.CoachMapper;
import com.dat.backend_v2_1.repository.Core.CoachRepository;
import com.dat.backend_v2_1.service.Security.UserService;
import com.dat.backend_v2_1.util.AccountUtil;
import com.dat.backend_v2_1.util.converter.NameConverter;
import com.dat.backend_v2_1.util.error.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CoachService {
    private final CoachRepository coachRepository;
    private final CoachMapper coachMapper;
    private final UserService userService;

    public Coach getCoachById(String coachId){
        return coachRepository.findById(UUID.fromString(coachId))
                .orElseThrow(() -> new BusinessException("Không tìm thấy huấn luyện viên với ID: " + coachId));
    }

    public Coach getCoachById(UUID coachId){
        return coachRepository.findById(coachId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy huấn luyện viên với ID: " + coachId));
    }

    /**
     * Lấy thông tin chi tiết Coach bao gồm cả thông tin từ User
     * @param userId ID của huấn luyện viên
     * @return CoachDetail DTO chứa đầy đủ thông tin
     */
    public CoachResDTO.CoachDetail getCoachDetail(UUID userId){
        Coach coach = getCoachById(userId);
        return coachMapper.toCoachDetail(coach);
    }

    /**
     * Tạo huấn luyện viên mới
     * - Validate dữ liệu đầu vào
     * - Kiểm tra trùng lặp
     * - Tự động sinh mã nhân viên
     * - Thiết lập tài khoản đăng nhập
     *
     * @param createDTO DTO chứa thông tin tạo mới
     * @return Mã nhân viên của huấn luyện viên mới tạo
     */
    @Transactional(rollbackFor = Exception.class)
    public String createCoach(CoachReqDTO.CoachCreate createDTO) {
        // BƯỚC 1: Validate Business (Check trùng lặp)
        if (coachRepository.existsByPhoneNumber(createDTO.getPhoneNumber())) {
            throw new BusinessException("Số điện thoại này đã được đăng ký!");
        }

        // BƯỚC 2: Mapping DTO -> Entity
        Coach newCoach = new Coach();

        // --- Thông tin cơ bản ---
        newCoach.setFullName(NameConverter.formatVietnameseName(createDTO.getFullName()));
        newCoach.setPhoneNumber(createDTO.getPhoneNumber());
        newCoach.setBirthDate(createDTO.getBirthDate());
        newCoach.setBelt(createDTO.getBelt());

        // --- Thông tin chuyên môn ---
        newCoach.setPosition(createDTO.getPosition());
        newCoach.setCoachStatus(createDTO.getCoachStatus() != null ? createDTO.getCoachStatus() : CoachStatus.ACTIVE);

        // BƯỚC 3: Enrich Data (Tự động sinh dữ liệu hệ thống)
        // Tạo mã nhân viên: HLV_datnt_311005
        String generatedCode = "HLV_" + AccountUtil.getUserCode(createDTO.getFullName(), createDTO.getBirthDate());

        // Check trùng mã sinh ra (Trường hợp hiếm gặp 2 người trùng tên trùng ngày sinh)
        if (coachRepository.existsByStaffCode(generatedCode)) {
            generatedCode = generatedCode + "_" + RandomStringUtils.secure().nextNumeric(2);
        }
        newCoach.setStaffCode(generatedCode);

        // BƯỚC 4: Thiết lập User Base (Tài khoản đăng nhập)
        // Logic này sẽ encode password, set Role COACH
        userService.setupBaseUser(newCoach, "COACH_TRAINEE");

        // BƯỚC 5: Save
        coachRepository.save(newCoach);

        log.info("Created coach successfully with code: {}", generatedCode);
        return generatedCode;
    }

    /**
     * Cập nhật thông tin Coach một cách chuyên nghiệp
     * - Chỉ cập nhật các field không null
     * - Validate business logic
     * - Log thay đổi
     *
     * @param updateDTO DTO chứa thông tin cần cập nhật
     * @return CoachDetail sau khi cập nhật
     */
    @Transactional(rollbackFor = Exception.class)
    public CoachResDTO.CoachDetail updateCoach(CoachReqDTO.CoachUpdate updateDTO) {
        // BƯỚC 1: Lấy entity hiện tại
        Coach coach = getCoachById(updateDTO.getUserId());

        // BƯỚC 2: Validate Business Logic
        // 2.1. Kiểm tra số điện thoại trùng (nếu có thay đổi)
        if (updateDTO.getPhoneNumber() != null &&
            !updateDTO.getPhoneNumber().equals(coach.getPhoneNumber())) {
            if (coachRepository.existsByPhoneNumber(updateDTO.getPhoneNumber())) {
                throw new BusinessException("Số điện thoại này đã được đăng ký bởi huấn luyện viên khác!");
            }
        }

        // BƯỚC 3: Cập nhật các field từ User (Parent)
        if (updateDTO.getPhoneNumber() != null) {
            log.info("Updated phone number for coach {}: {} -> {}",
                    coach.getStaffCode(), coach.getPhoneNumber(), updateDTO.getPhoneNumber());
            coach.setPhoneNumber(updateDTO.getPhoneNumber());
        }

        if (updateDTO.getBirthDate() != null) {
            coach.setBirthDate(updateDTO.getBirthDate());
            log.info("Updated birth date for coach {}: {}",
                    coach.getStaffCode(), updateDTO.getBirthDate());
        }

        if (updateDTO.getBelt() != null) {
            log.info("Updated belt for coach {}: {} -> {}",
                    coach.getStaffCode(), coach.getBelt(), updateDTO.getBelt());
            coach.setBelt(updateDTO.getBelt());
        }

        // BƯỚC 4: Cập nhật các field từ Coach (Child)
        if (updateDTO.getFullName() != null) {
            String formattedName = NameConverter.formatVietnameseName(updateDTO.getFullName());
            log.info("Updated full name for coach {}: {} -> {}",
                    coach.getStaffCode(), coach.getFullName(), formattedName);
            coach.setFullName(formattedName);
        }

        if (updateDTO.getPosition() != null) {
            log.info("Updated position for coach {}: {} -> {}",
                    coach.getStaffCode(), coach.getPosition(), updateDTO.getPosition());
            coach.setPosition(updateDTO.getPosition());
        }

        if (updateDTO.getCoachStatus() != null) {
            log.info("Updated coach status for coach {}: {} -> {}",
                    coach.getStaffCode(), coach.getCoachStatus(), updateDTO.getCoachStatus());
            coach.setCoachStatus(updateDTO.getCoachStatus());
        }

        // BƯỚC 5: Lưu thay đổi
        Coach updatedCoach = coachRepository.save(coach);

        log.info("Successfully updated coach with code: {}", updatedCoach.getStaffCode());

        // BƯỚC 6: Trả về CoachDetail
        return getCoachDetail(updatedCoach.getUserId());
    }

    /**
     * Xóa huấn luyện viên (Soft Delete)
     * - Không xóa vật lý khỏi database
     * - Chỉ cập nhật status thành DEACTIVATED
     * - Có thể khôi phục lại sau này
     *
     * @param userId ID của huấn luyện viên cần xóa
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteCoach(UUID userId) {
        // BƯỚC 1: Lấy entity hiện tại
        Coach coach = getCoachById(userId);

        // BƯỚC 2: Kiểm tra trạng thái hiện tại
        if (coach.getStatus() == com.dat.backend_v2_1.enums.Security.UserStatus.DEACTIVATED) {
            log.warn("Coach {} is already deactivated", coach.getStaffCode());
            throw new BusinessException("Huấn luyện viên này đã bị vô hiệu hóa trước đó!");
        }

        // BƯỚC 3: Soft Delete - Cập nhật status thành DEACTIVATED
        coach.setStatus(com.dat.backend_v2_1.enums.Security.UserStatus.DEACTIVATED);
        coach.setCoachStatus(CoachStatus.INACTIVE); // Cập nhật trạng thái công việc thành INACTIVE

        // BƯỚC 4: Lưu thay đổi
        coachRepository.save(coach);

        log.info("Successfully deactivated coach with code: {} (userId: {})",
                coach.getStaffCode(), userId);
    }

    /**
     * Xóa vật lý huấn luyện viên khỏi database (Hard Delete)
     * ⚠️ CẢNH BÁO: Hành động này không thể hoàn tác!
     * Chỉ nên dùng cho mục đích quản trị hoặc tuân thủ GDPR
     *
     * @param userId ID của huấn luyện viên cần xóa vĩnh viễn
     */
    @Transactional(rollbackFor = Exception.class)
    public void permanentlyDeleteCoach(UUID userId) {
        // BƯỚC 1: Kiểm tra tồn tại
        Coach coach = getCoachById(userId);

        log.warn("⚠️ PERMANENTLY DELETING coach: {} (userId: {})",
                coach.getStaffCode(), userId);

        // BƯỚC 2: Hard Delete
        coachRepository.delete(coach);

        log.info("Successfully permanently deleted coach with code: {}", coach.getStaffCode());
    }
}
