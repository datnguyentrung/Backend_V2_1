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

    /**
     * Giữ lại biến cũ để tương thích config hiện tại.
     * Dùng như thời gian chuẩn bị/mở lớp sớm trước giờ học.
     *
     * Ví dụ:
     * - Lớp học 18:00
     * - ATTENDANCE_GRACE_PERIOD_MINUTES = 30
     * - Từ 17:30 hệ thống đã active lớp để HLV chuẩn bị điểm danh.
     */
    @Value("${ATTENDANCE_GRACE_PERIOD_MINUTES:30}")
    private int attendanceGracePeriodMinutes;

    /**
     * Một ca dạy thực tế dao động 90 - 120 phút.
     * Để tránh chốt sớm các ca học 120 phút, lấy 120 phút làm ngưỡng an toàn.
     */
    @Value("${CLASS_SESSION_MAX_DURATION_MINUTES:120}")
    private int classSessionMaxDurationMinutes;

    /**
     * Sau khi hết ca học tối đa, cho phép HLV có thêm thời gian chỉnh/sửa điểm danh.
     *
     * Ví dụ:
     * - Lớp bắt đầu 18:00
     * - Ca tối đa 120 phút -> kết thúc khoảng 20:00
     * - Cho grace 30 phút -> 20:30 mới auto close attendance
     */
    @Value("${ATTENDANCE_CLOSE_AFTER_END_MINUTES:30}")
    private int attendanceCloseAfterEndMinutes;

    @Scheduled(cron = "0 00 03 * * *")
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

    /**
     * Active lớp trước giờ học.
     *
     * Chạy mỗi 5 phút, giây 00.
     * Không nên chạy cùng giây với các job khác để tránh dồn DB/WebSocket.
     */
    @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional(rollbackFor = Exception.class)
    public void autoActivateClassSessionsJob() {
        LocalDateTime now = LocalDateTime.now();

        /**
         * Ví dụ:
         * now = 17:30
         * attendanceGracePeriodMinutes = 30
         * thresholdTime = 18:00
         *
         * Các lớp có giờ học <= 18:00 trong ngày hôm nay sẽ được ACTIVE.
         */
        LocalTime thresholdTime = now
                .plusMinutes(attendanceGracePeriodMinutes)
                .toLocalTime();

        try {
            int updatedCount = classSessionRepository.activateScheduledSessions(
                    now.toLocalDate(),
                    thresholdTime
            );

            if (updatedCount > 0) {
                log.info(
                        "Successfully activated {} class sessions at {} with prep threshold {} minutes",
                        updatedCount,
                        now,
                        attendanceGracePeriodMinutes
                );

                broadcastAfterCommit(
                        "SESSIONS_ACTIVATED",
                        Map.of("count", updatedCount)
                );
            }
        } catch (Exception e) {
            log.error("Failed to execute autoActivateClassSessionsJob", e);
            throw e;
        }
    }

    /**
     * Auto complete lớp sau khi đã qua thời lượng học tối đa.
     *
     * Vì 1 ca dạy từ 90 - 120 phút, không dùng 90 phút để tránh chốt sớm.
     * Ngưỡng hợp lý: startTime + 120 phút.
     *
     * Chạy mỗi 5 phút, giây 20.
     */
    @Scheduled(cron = "20 */5 * * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional(rollbackFor = Exception.class)
    public void autoCompleteClassSessionsJob() {
        LocalDateTime now = LocalDateTime.now();

        /**
         * Ví dụ:
         * now = 20:00
         * classSessionMaxDurationMinutes = 120
         * thresholdDateTime = 18:00
         *
         * Các lớp bắt đầu <= 18:00 sẽ được COMPLETE.
         */
        LocalDateTime thresholdDateTime = now.minusMinutes(classSessionMaxDurationMinutes);
        LocalDate thresholdDate = thresholdDateTime.toLocalDate();
        LocalTime thresholdTime = thresholdDateTime.toLocalTime();

        try {
            int updatedCount = classSessionRepository.completeScheduledSessions(
                    thresholdDate,
                    thresholdTime
            );

            if (updatedCount > 0) {
                log.info(
                        "Successfully completed {} class sessions at {} with max duration {} minutes",
                        updatedCount,
                        now,
                        classSessionMaxDurationMinutes
                );

                broadcastAfterCommit(
                        "SESSION_COMPLETED",
                        Map.of("count", updatedCount)
                );
            }
        } catch (Exception e) {
            log.error("Failed to execute autoCompleteClassSessionsJob", e);
            throw e;
        }
    }

    /**
     * Auto đóng điểm danh sau khi lớp đã kết thúc một khoảng thời gian.
     *
     * Công thức:
     * close threshold = now - (classSessionMaxDurationMinutes + attendanceCloseAfterEndMinutes)
     *
     * Ví dụ:
     * - Lớp bắt đầu 18:00
     * - Ca học tối đa 120 phút
     * - Cho sửa điểm danh thêm 30 phút
     * - 20:30 mới đóng điểm danh
     *
     * Chạy mỗi 5 phút, giây 40.
     */
    @Scheduled(cron = "40 */5 * * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional(rollbackFor = Exception.class)
//    @Caching(evict = {
//            // Chốt tự động cũng phải xóa cache vì data thay đổi ngầm
//            //@CacheEvict(value = "studentDetail", allEntries = true),
//            //@CacheEvict(value = "classScheduleDetail", allEntries = true)
//    })
    public void autoCloseAttendanceJob() {
        LocalDateTime now = LocalDateTime.now();

        int closeAfterStartMinutes =
                classSessionMaxDurationMinutes + attendanceCloseAfterEndMinutes;

        LocalDateTime thresholdDateTime = now.minusMinutes(closeAfterStartMinutes);
        LocalDate thresholdDate = thresholdDateTime.toLocalDate();
        LocalTime thresholdTime = thresholdDateTime.toLocalTime();

        List<ClassSession> sessionsToClose = classSessionRepository
                .findClassSessionToClose(thresholdDate, thresholdTime);

        if (sessionsToClose.isEmpty()) {
            log.debug("No class sessions found that require attendance closure at {}", now);
            return;
        }

        int successCount = 0;
        int failedCount = 0;

        for (ClassSession session : sessionsToClose) {
            try {
                studentAttendanceService.processMissingAttendances(session);

                session.setAttendanceClosed(true);
                classSessionRepository.save(session);

                successCount++;

                log.info(
                        "Closed attendance for class session {} on date {}",
                        session.getSessionId(),
                        session.getSessionDate()
                );

                broadcastAfterCommit(
                        "SESSION_UPDATED",
                        Map.of("sessionId", session.getSessionId())
                );
            } catch (Exception e) {
                failedCount++;

                log.error(
                        "Failed to close attendance for class session {}: {}",
                        session.getSessionId(),
                        e.getMessage(),
                        e
                );
            }
        }

        log.info(
                "Auto close attendance finished at {}. Success: {}, Failed: {}, Threshold: {} minutes after start",
                now,
                successCount,
                failedCount,
                closeAfterStartMinutes
        );
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
    public ClassSessionDTO.SessionResponse updateClassSession(UUID sessionId,
                                                              ClassSessionDTO.SessionUpdateRequest request) {
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

    public PageResponse<ClassSessionDTO.SessionResponse> filterClassSessions(String search, LocalDate sessionDate,
                                                                             Boolean isAttendanceClosed,
                                                                             List<String> scheduleIds,
                                                                             Pageable pageable) {
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
