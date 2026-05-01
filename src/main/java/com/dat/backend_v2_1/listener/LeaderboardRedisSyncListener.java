package com.dat.backend_v2_1.listener;

import com.dat.backend_v2_1.dto.Report.YearlySummaryDTO;
import com.dat.backend_v2_1.event.ScoreRecalculateEvent;
import com.dat.backend_v2_1.service.Report.StudentSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class LeaderboardRedisSyncListener {

    // Dùng StringRedisTemplate để xài cấu trúc ZSET một cách tối ưu nhất
    private final StringRedisTemplate stringRedisTemplate;
    private final StudentSummaryService summaryService;

    @Async // Bắt buộc phải có để chạy ngầm (Non-blocking)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleScoreRecalculation(ScoreRecalculateEvent event) {
        try {
            // 1. Chọc xuống DB tính lại ĐÚNG tổng điểm của quý đó
            YearlySummaryDTO.QuarterSummary summary = summaryService.getQuarterSummary(
                    event.getStudentCode(),
                    event.getYear(),
                    event.getQuarter()
            );

            double finalScore = summary.getTotalQuarterScore();

            // 2. Định nghĩa Key của bảng xếp hạng (VD: leaderboard:2026:Q2)
            String redisKey = String.format("leaderboard:%d:Q%d", event.getYear(), event.getQuarter());

            // 3. GHI ĐÈ điểm mới vào Redis (ZADD)
            // Nếu studentCode chưa có -> Thêm mới
            // Nếu đã có -> Cập nhật điểm và Redis tự động sắp xếp lại vị trí
            stringRedisTemplate.opsForZSet().add(
                    redisKey,
                    event.getStudentCode(),
                    finalScore
            );

            log.info("🏆 [Leaderboard Sync] Updated {} for {} - New Score: {}",
                    redisKey, event.getStudentCode(), finalScore);

        } catch (Exception e) {
            log.error("❌ [Leaderboard Sync] Failed for studentCode: {}", event.getStudentCode(), e);
        }
    }
}