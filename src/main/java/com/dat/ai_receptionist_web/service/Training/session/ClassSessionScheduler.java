package com.dat.ai_receptionist_web.service.Training.session;

import com.dat.ai_receptionist_web.service.Training.scheduling.CourseSessionPlanningService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Adapter @Scheduled không chứa nghiệp vụ: planning trước, sau đó activation,
 * closure rồi completion.
 */
@Component
@RequiredArgsConstructor
@Order(10)
public class ClassSessionScheduler implements ApplicationRunner {
    private final CourseSessionPlanningService planningService;
    private final ClassSessionLifecycleService lifecycleService;

    @Override
    public void run(ApplicationArguments args) {
        planningService.maintainGenerationHorizon();
        lifecycleService.activateSessions();
        lifecycleService.closeDueSessions();
        lifecycleService.completeSessions();
    }

    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Ho_Chi_Minh")
    public void maintainGenerationHorizon() {
        planningService.maintainGenerationHorizon();
    }

    @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Ho_Chi_Minh")
    public void activateSessions() {
        lifecycleService.activateSessions();
    }

    @Scheduled(cron = "40 */10 * * * *", zone = "Asia/Ho_Chi_Minh")
    public void closeAttendance() {
        lifecycleService.closeDueSessions();
    }

    @Scheduled(cron = "20 */10 * * * *", zone = "Asia/Ho_Chi_Minh")
    public void completeSessions() {
        lifecycleService.completeSessions();
    }
}
