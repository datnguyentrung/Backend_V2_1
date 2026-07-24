package com.dat.ai_receptionist_web.controller.Report;

import com.dat.ai_receptionist_web.dto.Report.LeaderboardDTO;
import com.dat.ai_receptionist_web.dto.Report.YearlySummaryDTO;
import com.dat.ai_receptionist_web.dto.Skill.FitnessRecordDTO;
import com.dat.ai_receptionist_web.dto.WebhookPayload;
import com.dat.ai_receptionist_web.enums.Skill.SkillLevel;
import com.dat.ai_receptionist_web.service.Report.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/leaderboards")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping("/quarter/fitness")
    public ResponseEntity<LeaderboardDTO.Response<FitnessRecordDTO.Metrics>> getFitnessLeaderboard(
            @RequestParam int year,
            @RequestParam int quarter,
            @RequestParam SkillLevel skillLevel, // Thêm filter bắt buộc như bạn muốn
            @PageableDefault(size = 50) Pageable pageable
    ) {
        // Gọi đúng hàm Fitness
        LeaderboardDTO.Response<FitnessRecordDTO.Metrics> response = leaderboardService.getFitnessLeaderboard(year, quarter, skillLevel, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * API Lấy bảng xếp hạng theo Quý
     * VD: GET /api/v1/leaderboards/quarter?year=2026&quarter=2&limit=50
     */
    @GetMapping("/quarter")
    public ResponseEntity<LeaderboardDTO.Response<YearlySummaryDTO.QuarterSummary>> getQuarterLeaderboard(
            @RequestParam int year,
            @RequestParam int quarter,
            @PageableDefault(size = 50) Pageable pageable
    ) {
        // Ép kiểu chuẩn xác cho Response
        LeaderboardDTO.Response<YearlySummaryDTO.QuarterSummary> response =
                leaderboardService.getQuarterLeaderboard(year, quarter, null, pageable);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/sync-batch")
    public ResponseEntity<String> syncBatch(@RequestBody List<WebhookPayload<FitnessRecordDTO.Metrics>> payloads) {
        leaderboardService.processBatchSync(payloads);
        return ResponseEntity.ok("Đã xử lý Batch thành công");
    }
}
