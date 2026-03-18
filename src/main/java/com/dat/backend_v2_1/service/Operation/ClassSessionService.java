package com.dat.backend_v2_1.service.Operation;

import com.dat.backend_v2_1.domain.Core.ClassSchedule;
import com.dat.backend_v2_1.domain.Operation.ClassSession;
import com.dat.backend_v2_1.enums.Core.ScheduleStatus;
import com.dat.backend_v2_1.enums.Core.Weekday;
import com.dat.backend_v2_1.repository.Core.ClassScheduleRepository;
import com.dat.backend_v2_1.repository.Operation.ClassSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClassSessionService {
    private final ClassSessionRepository classSessionRepository;
    private final ClassScheduleRepository classScheduleRepository;

    // Tiến trình này sẽ chạy vào lúc 9:32 sáng hàng ngày để sinh buổi học cho ngày hôm đó
    @Scheduled(cron = "0 35 9 * * *")
    @Transactional(rollbackFor = Exception.class)
    public void generateClassSessions() {
        LocalDate today = LocalDate.now();
        DayOfWeek currentDayOfWeek = today.getDayOfWeek();

        log.info("Bắt đầu tiến trình sinh buổi học tự động cho ngày: {} ({})", today, currentDayOfWeek);

        if (classSessionRepository.existsBySessionDate(today)) {
            log.info("Buổi học cho ngày {} đã tồn tại. Bỏ qua sinh buổi học.", today);
            return;
        }

        // Lấy tất cả lịch học có ngày học trùng với ngày hôm nay
        List<ClassSchedule> todaySchedules = classScheduleRepository
                .findByWeekdayAndScheduleStatus(
                        Weekday.valueOf(currentDayOfWeek.name()),
                        ScheduleStatus.ACTIVE
                );

        if (todaySchedules.isEmpty()) {
            log.info("Không có lịch học nào phù hợp để sinh buổi học cho ngày {}.", today);
            return;
        }

        // Sinh buổi học cho mỗi lịch học phù hợp
        List<ClassSession> newSessions = todaySchedules.stream()
                .map(schedule -> ClassSession.builder()
                        .classSchedule(schedule)
                        .sessionDate(today)
                        .build())
                .toList();

        classSessionRepository.saveAll(newSessions);
        log.info("Đã sinh {} buổi học mới cho ngày {}.", newSessions.size(), today);
    }
}
