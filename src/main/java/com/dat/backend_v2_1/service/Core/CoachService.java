package com.dat.backend_v2_1.service.Core;

import com.dat.backend_v2_1.domain.Core.Coach;
import com.dat.backend_v2_1.domain.Operation.CoachAssignment;
import com.dat.backend_v2_1.dto.Core.CoachReqDTO;
import com.dat.backend_v2_1.dto.Core.CoachResDTO;
import com.dat.backend_v2_1.dto.Operation.CoachAssignmentResDTO;
import com.dat.backend_v2_1.enums.Core.CoachStatus;
import com.dat.backend_v2_1.enums.Operation.CoachAssignmentStatus;
import com.dat.backend_v2_1.mapper.Core.CoachMapper;
import com.dat.backend_v2_1.mapper.Operation.CoachAssignmentMapper;
import com.dat.backend_v2_1.repository.Core.CoachRepository;
import com.dat.backend_v2_1.service.Operation.CoachAssignmentService;
import com.dat.backend_v2_1.service.Security.UserService;
import com.dat.backend_v2_1.util.AccountUtil;
import com.dat.backend_v2_1.util.converter.NameConverter;
import com.dat.backend_v2_1.util.error.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CoachService {
    private final CoachRepository coachRepository;
    private final CoachMapper coachMapper;
    private final UserService userService;
    private final CoachAssignmentService coachAssignmentService;
    private final CoachAssignmentMapper coachAssignmentMapper;

    /**
     * Validates coach exists and is in ACTIVE status.
     * <p>
     * Helper method để tránh code duplication trong validation logic.
     *
     * @param coachId ID của HLV cần validate
     * @return Coach entity nếu hợp lệ
     * @throws AccessDeniedException  nếu HLV không ở trạng thái ACTIVE
     * @throws NoSuchElementException nếu không tìm thấy HLV
     */
    public Coach validateCoachAndGetActive(String coachId) {
        Coach coach = getCoachById(coachId);

        if (coach.getCoachStatus() != CoachStatus.ACTIVE) {
            log.warn("Security Alert: Coach {} (Status: {}) attempted unauthorized action",
                    coach.getFullName(), coach.getCoachStatus());
            throw new AccessDeniedException("Tài khoản của bạn đã bị khóa hoặc không hoạt động.");
        }

        return coach;
    }

    public Coach getCoachById(String coachId) {
        return coachRepository.findById(UUID.fromString(coachId))
                .orElseThrow(() -> new BusinessException("Không tìm thấy huấn luyện viên với ID: " + coachId));
    }

    public Coach getCoachById(UUID coachId) {
        return coachRepository.findById(coachId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy huấn luyện viên với ID: " + coachId));
    }

    /**
     * Lấy thông tin chi tiết Coach bao gồm cả thông tin từ User
     *
     * @param userId ID của huấn luyện viên
     * @return CoachDetail DTO chứa đầy đủ thông tin
     */
    public CoachResDTO.CoachDetail getCoachDetail(UUID userId) {
        Coach coach = getCoachById(userId);

        List<CoachAssignmentResDTO.SimpleResponse> coachAssignmentCurrent = coachAssignmentService.findStudentEnrollmentsByCoachId(userId, CoachAssignmentStatus.ACTIVE);

        return coachMapper.toCoachDetailWithAssignments(coach, coachAssignmentCurrent);
    }

    /**
     * Lấy thông tin chi tiết Coach theo staffCode (mã nhân viên)
     *
     * @param staffCode Mã nhân viên của huấn luyện viên
     * @return CoachDetail DTO chứa đầy đủ thông tin
     */
    public CoachResDTO.CoachDetail getCoachDetail(String staffCode) {
        Coach coach = getCoachById(staffCode);

        List<CoachAssignmentResDTO.SimpleResponse> coachAssignmentCurrent = coachAssignmentService.findStudentEnrollmentsByCoachId(coach.getUserId(), CoachAssignmentStatus.ACTIVE);

        return coachMapper.toCoachDetailWithAssignments(coach, coachAssignmentCurrent);
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
    public CoachResDTO.CoachDetail createCoach(CoachReqDTO.CoachCreate createDTO) {
        // BƯỚC 1: Validate Business
        if (coachRepository.existsByPhoneNumber(createDTO.getPhoneNumber())) {
            throw new BusinessException("Số điện thoại này đã được đăng ký!");
        }

        // BƯỚC 2: Mapping DTO -> Entity
        Coach newCoach = new Coach();
        newCoach.setFullName(NameConverter.formatVietnameseName(createDTO.getFullName()));
        newCoach.setPhoneNumber(createDTO.getPhoneNumber());
        newCoach.setBirthDate(createDTO.getBirthDate());
        newCoach.setBelt(createDTO.getBelt());
        newCoach.setEmail(createDTO.getEmail());
        newCoach.setCoachStatus(createDTO.getCoachStatus() != null ? createDTO.getCoachStatus() : CoachStatus.ACTIVE);

        // BƯỚC 3: Enrich Data
        String generatedCode = AccountUtil.getUserCode(createDTO.getFullName(), createDTO.getBirthDate(), "VQT");
        while (coachRepository.existsByStaffCode(generatedCode)) {
            generatedCode = generatedCode + "_" + RandomStringUtils.secure().nextNumeric(2);
        }
        newCoach.setStaffCode(generatedCode);

        // BƯỚC 4: Thiết lập User Base
        String roleCode = StringUtils.hasText(createDTO.getRoleCode()) ? createDTO.getRoleCode() : "COACH_TRAINEE";
        userService.setupBaseUser(newCoach, roleCode);

        // BƯỚC 5: Save
        newCoach = coachRepository.save(newCoach);

        // ================= SỬA TẠI ĐÂY =================
        // Khai báo biến list rỗng ở ngoài khối if
        List<CoachAssignmentResDTO.SimpleResponse> assignmentResponses = new ArrayList<>();

        // BƯỚC 6: Xử lý phân công
        if (createDTO.getAssignmentRequest() != null
                && createDTO.getAssignmentRequest().getScheduleIds() != null
                && !createDTO.getAssignmentRequest().getScheduleIds().isEmpty()) {

            // Gán ID vừa tạo vào request phân công
            createDTO.getAssignmentRequest().setCoachId(String.valueOf(newCoach.getUserId()));

            // Gọi Service phân công
            List<CoachAssignment> coachAssignments = coachAssignmentService.createCoachAssignment(createDTO.getAssignmentRequest());

            // Map sang DTO (Đảm bảo bạn đã inject coachAssignmentMapper vào file Service này)
            assignmentResponses = coachAssignments.stream()
                    .map(coachAssignmentMapper::toSimpleResponse)
                    .toList();
        }

        log.info("Created coach successfully with code: {}", generatedCode);

        // Bây giờ assignmentResponses luôn tồn tại (rỗng nếu không có phân công, có data nếu có phân công)
        // Gọi đúng tên hàm mới có 2 tham số
        return coachMapper.toCoachDetailWithAssignments(newCoach, assignmentResponses);
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

    public List<CoachResDTO.CoachDetail> getAllCoaches() {
        // 1. Query Lần 1: Lấy danh sách toàn bộ HLV
        List<Coach> coaches = coachRepository.findAll();

        if (coaches.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. Lấy ra một danh sách (List) các ID của HLV
        List<UUID> coachIds = coaches.stream()
                .map(Coach::getUserId)
                .toList();

        // 3. Query Lần 2: Lấy TOÀN BỘ phân công của tất cả các HLV này cùng 1 lúc
        List<CoachAssignment> allActiveAssignments = coachAssignmentService.getAllCoachAssignmentsByListCoachIds(coachIds, CoachAssignmentStatus.ACTIVE);

        // 4. Nhóm các phân công lại theo từng ID HLV (Sử dụng Map để tra cứu O(1))
        // Cấu trúc Map: { Coach_UUID_1 : [Lớp A, Lớp B], Coach_UUID_2 : [Lớp C] }
        Map<UUID, List<CoachAssignment>> assignmentsByCoachId = allActiveAssignments.stream()
                .collect(Collectors.groupingBy(ca -> ca.getCoach().getUserId()));

        // 5. Map sang DTO
        return coaches.stream().map(coach -> {
            // Lấy danh sách assignment từ Map (Nếu không có thì trả về list rỗng)
            List<CoachAssignment> myAssignments = assignmentsByCoachId.getOrDefault(coach.getUserId(), new ArrayList<>());

            // Map sang SimpleResponse DTO
            List<CoachAssignmentResDTO.SimpleResponse> assignmentResponses = myAssignments.stream()
                    .map(coachAssignmentMapper::toSimpleResponse)
                    .toList();

            // Gọi hàm mapper 2 tham số (đã tạo ở bài trước)
            return coachMapper.toCoachDetailWithAssignments(coach, assignmentResponses);
        }).toList();
    }
}
