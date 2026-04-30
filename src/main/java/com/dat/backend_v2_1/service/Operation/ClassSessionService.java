package com.dat.backend_v2_1.service.Operation;

import com.dat.backend_v2_1.domain.Core.ClassSchedule;
import com.dat.backend_v2_1.domain.Operation.ClassSession;
import com.dat.backend_v2_1.dto.Operation.ClassSessionDTO;
import com.dat.backend_v2_1.dto.PageResponse;
import com.dat.backend_v2_1.enums.Core.ScheduleStatus;
import com.dat.backend_v2_1.enums.Core.Weekday;
import com.dat.backend_v2_1.mapper.Operation.ClassSessionMapper;
import com.dat.backend_v2_1.repository.Core.ClassScheduleRepository;
import com.dat.backend_v2_1.repository.Operation.ClassSessionRepository;
import com.dat.backend_v2_1.socket.ClassSessionWebSocketHandler;
import com.dat.backend_v2_1.specification.ClassSessionSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClassSessionService {
    private final ClassSessionRepository classSessionRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final ClassSessionMapper classSessionMapper;
    private final StudentAttendanceService studentAttendanceService;
    private final ClassSessionWebSocketHandler wsHandler;

    @Value("${ATTENDANCE_GRACE_PERIOD_MINUTES:30}")
    private int attendanceGracePeriodMinutes;

    @Scheduled(cron = "0 10 0 * * *")
    @Transactional(rollbackFor = Exception.class)
    public void generateClassSessions() {
        LocalDate today = LocalDate.now();
        Weekday currentWeekday = Weekday.valueOf(today.getDayOfWeek().name());

        log.info("Bắt đầu tiến trình sinh buổi học tự động cho ngày: {} ({})", today, currentWeekday);

        // Mọi logic so sánh đã được đẩy xuống DB, trả về đúng những lịch cần tạo
        List<ClassSchedule> schedulesNeedingSession = classScheduleRepository
                .findSchedulesNeedingSession(currentWeekday, ScheduleStatus.ACTIVE, today);

        if (schedulesNeedingSession.isEmpty()) {
            log.info("Không có lịch học nào cần sinh buổi học mới cho ngày {}.", today);
            return;
        }

        // Sinh buổi học cho mỗi lịch học phù hợp
        List<ClassSession> newSessions = schedulesNeedingSession.stream()
                .map(schedule -> ClassSession.builder()
                        .classSchedule(schedule)
                        .build())
                .toList();

        classSessionRepository.saveAll(newSessions);
        log.info("Đã sinh {} buổi học mới cho ngày {}.", newSessions.size(), today);

        broadcastAfterCommit("SESSIONS_GENERATED", Map.of("count", newSessions.size()));
    }

    @Scheduled(cron = "0 */1 * * * *")
    @Transactional(rollbackFor = Exception.class)
    public void autoActivateClassSessionsJob() {
        LocalDateTime now = LocalDateTime.now();

        // CỘNG THÊM thời gian chuẩn bị.
        // Ví dụ: Bây giờ là 17:30, thresholdTime sẽ là 18:00.
        // Lệnh SQL sẽ quét và Active luôn các lớp có giờ học <= 18:00.
        LocalTime thresholdTime = now.plusMinutes(attendanceGracePeriodMinutes).toLocalTime();

        try {
            int updatedCount = classSessionRepository.activateScheduledSessions(
                    now.toLocalDate(), thresholdTime
            );

            if (updatedCount > 0) {
                log.info("Successfully activated {} class sessions (including early prep) at {}", updatedCount, now);

                // Chỉ bắn WebSocket khi thực sự có record được update
                broadcastAfterCommit("SESSIONS_ACTIVATED", Map.of("count", updatedCount));
            }
        } catch (Exception e) {
            log.error("Failed to execute autoActivateClassSessionsJob", e);
        }
    }

    @Scheduled(cron = "0 */5 * * * *")
    @Transactional(rollbackFor = Exception.class)
    public void autoCompleteClassSessionsJob() {
        LocalDateTime now = LocalDateTime.now();

        try {
            int updatedCount = classSessionRepository.completeScheduledSessions(
                    now.toLocalDate(), now.toLocalTime()
            );

            if (updatedCount > 0) {
                log.info("Successfully completed {} class sessions at {}", updatedCount, now);

                // 3. GỌI WEBSOCKET Ở ĐÂY
                // Truyền luôn ID của buổi học vừa chốt xuống FE để FE cập nhật UI ngay lập tức
                broadcastAfterCommit("SESSION_COMPLETED", Map.of("count", updatedCount));
            }
        } catch (Exception e) {
            log.error("Failed to execute autoActivateClassSessionsJob", e);
        }
    }

    @Scheduled(cron = "0 */5 * * * *")
    @Transactional(rollbackFor = Exception.class)
//    @Caching(evict = {
//            // Chốt tự động cũng phải xóa cache vì data thay đổi ngầm
//            @CacheEvict(value = "studentDetail", allEntries = true),
//            @CacheEvict(value = "classScheduleDetail", allEntries = true)
//    })
    public void autoCloseAttendanceJob() {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime thresholdDateTime = now.minusMinutes(attendanceGracePeriodMinutes);
        LocalDate thresholdDate = thresholdDateTime.toLocalDate();
        LocalTime thresholdTime = thresholdDateTime.toLocalTime();

        List<ClassSession> sessionsToClose = classSessionRepository
                .findClassSessionToClose(thresholdDate, thresholdTime);

        if (sessionsToClose.isEmpty()) {
            log.info("No class sessions found that require attendance closure at {}", now);
            return;
        }

        for (ClassSession session : sessionsToClose) {
            try {
                studentAttendanceService.processMissingAttendances(
                        session.getClassSchedule().getScheduleId(),
                        session.getSessionDate()
                );

                session.setAttendanceClosed(true); // Đóng điểm danh để không cho phép sửa sau khi đã chốt
                classSessionRepository.save(session);
                log.info("Closed attendance for class session {} on date {}",
                        session.getSessionId(), session.getSessionDate());

                // THÊM DÒNG NÀY (Vì đây là update từng record nên truyền luôn sessionId)
                broadcastAfterCommit("SESSION_UPDATED", Map.of("sessionId", session.getSessionId()));
            } catch (Exception e) {
                log.error("Failed to close attendance for class session {}: {}",
                        session.getSessionId(), e.getMessage());
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ClassSessionDTO.SessionResponse createClassSession(ClassSessionDTO.SessionCreateRequest request) {
        ClassSchedule schedule = classScheduleRepository.findById(request.getScheduleId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lịch học với ID: " + request.getScheduleId()));

        ClassSession newSession = ClassSession.builder()
                .classSchedule(schedule)
                .sessionDate(request.getSessionDate() != null ? request.getSessionDate() : LocalDate.now())
                .status(request.getStatus())
                .isAttendanceClosed(request.isAttendanceClosed())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .note(request.getNote())
                .build();

        ClassSession savedSession = classSessionRepository.save(newSession);
        // 2. Gọi WebSocket sau khi tạo thành công
        broadcastAfterCommit("SESSION_CREATED", Map.of("sessionId", savedSession.getSessionId()));

        return classSessionMapper.toSessionResponse(savedSession);
    }

    @Transactional
    public ClassSessionDTO.SessionResponse updateClassSession(UUID sessionId, ClassSessionDTO.SessionUpdateRequest request) {
        ClassSession session = classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy buổi học với ID: " + sessionId));

        // MapStruct sẽ tự động map các trường khác null từ request sang session
        classSessionMapper.updateEntityFromRequest(request, session);

        classSessionRepository.saveAndFlush(session);

        // Chỉ cần gọi thế này, cực kỳ sạch đẹp và an toàn tuyệt đối!
        broadcastAfterCommit("SESSION_UPDATED", Map.of("sessionId", sessionId));

        // Dựa vào Dirty Checking của @Transactional, dữ liệu tự được lưu
        return classSessionMapper.toSessionResponse(session);
    }

    @Transactional
    public void deleteClassSession(UUID sessionId) {
        ClassSession existingSession = classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy buổi học với ID: " + sessionId));

        classSessionRepository.delete(existingSession);

        broadcastAfterCommit("SESSION_DELETED", Map.of("sessionId", sessionId));
    }

    public ClassSessionDTO.SessionResponse getClassSessionById(UUID sessionId) {
        ClassSession session = classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy buổi học với ID: " + sessionId));

        return classSessionMapper.toSessionResponse(session);
    }

    public PageResponse<ClassSessionDTO.SessionResponse> filterClassSessions(String search, LocalDate sessionDate, Boolean isAttendanceClosed, List<String> scheduleIds, Pageable pageable) {
        // Lắp ráp các điều kiện bằng phép AND
        Specification<ClassSession> spec = Specification.where(ClassSessionSpecification.hasSearch(search))
                .and(ClassSessionSpecification.hasDate(sessionDate))
                .and(ClassSessionSpecification.hasAttendanceStatus(isAttendanceClosed))
                .and(ClassSessionSpecification.hasScheduleIds(scheduleIds));

        var pageResult = classSessionRepository.findAll(spec, pageable);

        // 3. Trả về luôn! Vừa phân trang vừa map sang DTO trong 1 nốt nhạc
        return PageResponse.of(pageResult, classSessionMapper::toSessionResponse);
    }

    // Thêm hàm tiện ích này vào cuối class
    private void broadcastAfterCommit(String actionType, Map<String, Object> data) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    wsHandler.broadcastSessionChange(actionType, data);
                }
            });
        } else {
            // Nếu không nằm trong Transaction nào, thì bắn luôn
            wsHandler.broadcastSessionChange(actionType, data);
        }
    }
}
