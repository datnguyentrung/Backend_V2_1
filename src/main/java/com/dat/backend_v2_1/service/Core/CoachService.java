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
import com.dat.backend_v2_1.util.error.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

    @Autowired
    @Lazy
    private CoachService self;

    public Coach validateCoachAndGetActive(String coachId) {
        // Bỏ qua proxy, gọi thẳng hàm nội bộ vì hàm gốc đã bỏ Cache
        Coach coach = getCoachById(coachId);

        if (coach.getCoachStatus() != CoachStatus.ACTIVE) {
            log.warn("Security Alert: Coach {} (Status: {}) attempted unauthorized action",
                    coach.getFullName(), coach.getCoachStatus());
            throw new AccessDeniedException("Tài khoản của bạn đã bị khóa hoặc không hoạt động.");
        }

        return coach;
    }

    // ❌ ĐÃ XÓA @Cacheable: Không Cache JPA Entity!
    public Coach getCoachById(UUID coachId) {
        return coachRepository.findById(coachId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy huấn luyện viên với ID: " + coachId));
    }

    public Coach getCoachById(String coachId) {
        return getCoachById(UUID.fromString(coachId)); // Gọi thẳng, không cần self
    }

    // ❌ ĐÃ XÓA @Cacheable: Không Cache JPA Entity!
    public Coach getCoachByStaffCode(String staffCode) {
        return coachRepository.findByStaffCode(staffCode)
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy HLV: " + staffCode));
    }

    // ✅ CHỈ CACHE DTO
    @Cacheable(value = "coachDetail", key = "#userId")
    public CoachResDTO.CoachDetail getCoachDetail(UUID userId) {
        // Dùng hàm bình thường lấy từ DB
        Coach coach = getCoachById(userId);

        List<CoachAssignmentResDTO.SimpleResponse> coachAssignmentCurrent = coachAssignmentService.findCoachAssignmentsByCoachId(userId, CoachAssignmentStatus.ACTIVE);

        return coachMapper.toCoachDetailWithAssignments(coach, coachAssignmentCurrent);
    }

    // ✅ CHỈ CACHE DTO
    @Cacheable(value = "coachDetailByCode", key = "#staffCode")
    @Transactional(readOnly = true)
    public CoachResDTO.CoachDetail getCoachDetail(String staffCode) {
        Coach coach = getCoachByStaffCode(staffCode);
        List<CoachAssignmentResDTO.SimpleResponse> coachAssignmentCurrent = coachAssignmentService.findCoachAssignmentsByCoachId(coach.getUserId(), CoachAssignmentStatus.ACTIVE);
        return coachMapper.toCoachDetailWithAssignments(coach, coachAssignmentCurrent);
    }

    @Caching(put = {
            @CachePut(value = "coachDetail", key = "#result.userId"),
            @CachePut(value = "coachDetailByCode", key = "#result.staffCode")
    })
    @Transactional(rollbackFor = Exception.class)
    public CoachResDTO.CoachDetail createCoach(CoachReqDTO.CoachCreate createDTO) {
        if (coachRepository.existsByPhoneNumber(createDTO.getPhoneNumber())) {
            throw new BusinessException("Số điện thoại này đã được đăng ký!");
        }

        Coach newCoach = new Coach();
        newCoach.setFullName(NameConverter.formatVietnameseName(createDTO.getFullName()));
        newCoach.setPhoneNumber(createDTO.getPhoneNumber());
        newCoach.setBirthDate(createDTO.getBirthDate());
        newCoach.setBelt(createDTO.getBelt());
        newCoach.setEmail(createDTO.getEmail());
        newCoach.setCoachStatus(createDTO.getCoachStatus() != null ? createDTO.getCoachStatus() : CoachStatus.ACTIVE);

        String generatedCode = AccountUtil.getUserCode(createDTO.getFullName(), createDTO.getBirthDate(), "VQT");
        while (coachRepository.existsByStaffCode(generatedCode)) {
            generatedCode = generatedCode + "_" + RandomStringUtils.secure().nextNumeric(2);
        }
        newCoach.setStaffCode(generatedCode);

        String roleCode = StringUtils.hasText(createDTO.getRoleCode()) ? createDTO.getRoleCode() : "COACH_TRAINEE";
        userService.setupBaseUser(newCoach, roleCode);

        newCoach = coachRepository.save(newCoach);

        List<CoachAssignmentResDTO.SimpleResponse> assignmentResponses = new ArrayList<>();

        if (createDTO.getAssignmentRequest() != null
                && createDTO.getAssignmentRequest().getScheduleIds() != null
                && !createDTO.getAssignmentRequest().getScheduleIds().isEmpty()) {

            createDTO.getAssignmentRequest().setCoachId(String.valueOf(newCoach.getUserId()));
            List<CoachAssignment> coachAssignments = coachAssignmentService.createCoachAssignment(createDTO.getAssignmentRequest());

            assignmentResponses = coachAssignments.stream()
                    .map(coachAssignmentMapper::toSimpleResponse)
                    .toList();
        }

        log.info("Created coach successfully with code: {}", generatedCode);
        return coachMapper.toCoachDetailWithAssignments(newCoach, assignmentResponses);
    }

    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
// Đã xóa các key rác (coach, coachByCode), chỉ giữ lại DTO
            @CacheEvict(value = "coachDetail", key = "#updateDTO.userId"),
            @CacheEvict(value = "coachDetailByCode", allEntries = true)
    })
    public CoachResDTO.CoachDetail updateCoach(CoachReqDTO.CoachUpdate updateDTO) {
        Coach coach = coachRepository.findById(updateDTO.getUserId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy huấn luyện viên với ID: " + updateDTO.getUserId()));

        if (updateDTO.getPhoneNumber() != null &&
                !updateDTO.getPhoneNumber().equals(coach.getPhoneNumber())) {
            if (coachRepository.existsByPhoneNumber(updateDTO.getPhoneNumber())) {
                throw new BusinessException("Số điện thoại này đã được đăng ký bởi huấn luyện viên khác!");
            }
        }

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

        Coach updatedCoach = coachRepository.save(coach);
        log.info("Successfully updated coach with code: {}", updatedCoach.getStaffCode());

        List<CoachAssignmentResDTO.SimpleResponse> coachAssignmentCurrent = coachAssignmentService.findCoachAssignmentsByCoachId(updatedCoach.getUserId(), CoachAssignmentStatus.ACTIVE);
        return coachMapper.toCoachDetailWithAssignments(updatedCoach, coachAssignmentCurrent);
    }

    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(value = "coachDetail", key = "#userId"),
            @CacheEvict(value = "coachDetailByCode", allEntries = true)
    })

    public void deleteCoach(UUID userId) {
        Coach coach = coachRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy huấn luyện viên với ID: " + userId));

        if (coach.getStatus() == com.dat.backend_v2_1.enums.Security.UserStatus.DEACTIVATED) {
            log.warn("Coach {} is already deactivated", coach.getStaffCode());
            throw new BusinessException("Huấn luyện viên này đã bị vô hiệu hóa trước đó!");
        }

        coach.setStatus(com.dat.backend_v2_1.enums.Security.UserStatus.DEACTIVATED);
        coach.setCoachStatus(CoachStatus.INACTIVE);

        coachRepository.save(coach);
        log.info("Successfully deactivated coach with code: {} (userId: {})", coach.getStaffCode(), userId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(value = "coachDetail", key = "#userId"),
            @CacheEvict(value = "coachDetailByCode", allEntries = true)
    })

    public void permanentlyDeleteCoach(UUID userId) {
        // ✅ ĐÃ SỬA: Lấy từ Repo thay vì self
        Coach coach = coachRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy huấn luyện viên với ID: " + userId));

        log.warn("⚠️ PERMANENTLY DELETING coach: {} (userId: {})",
                coach.getStaffCode(), userId);

        coachRepository.delete(coach);
        log.info("Successfully permanently deleted coach with code: {}", coach.getStaffCode());
    }

    public List<CoachResDTO.CoachDetail> getAllCoaches() {
        List<Coach> coaches = coachRepository.findAll();

        if (coaches.isEmpty()) {
            return new ArrayList<>();
        }

        List<UUID> coachIds = coaches.stream()
                .map(Coach::getUserId)
                .toList();

        List<CoachAssignment> allActiveAssignments = coachAssignmentService.getAllCoachAssignmentsByListCoachIds(coachIds, CoachAssignmentStatus.ACTIVE);

        Map<UUID, List<CoachAssignment>> assignmentsByCoachId = allActiveAssignments.stream()
                .collect(Collectors.groupingBy(ca -> ca.getCoach().getUserId()));

        return coaches.stream().map(coach -> {
            List<CoachAssignment> myAssignments = assignmentsByCoachId.getOrDefault(coach.getUserId(), new ArrayList<>());

            List<CoachAssignmentResDTO.SimpleResponse> assignmentResponses = myAssignments.stream()
                    .map(coachAssignmentMapper::toSimpleResponse)
                    .toList();

            return coachMapper.toCoachDetailWithAssignments(coach, assignmentResponses);
        }).toList();
    }
}