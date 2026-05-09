package com.dat.backend_v2_1.controller.Report;

import com.dat.backend_v2_1.dto.Report.LeaderboardDTO;
import com.dat.backend_v2_1.dto.Skill.FitnessRecordDTO;
import com.dat.backend_v2_1.dto.WebhookPayload;
import com.dat.backend_v2_1.enums.Skill.SkillLevel;
import com.dat.backend_v2_1.service.Report.LeaderboardService;
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
    public ResponseEntity<LeaderboardDTO.Response<Object>> getQuarterLeaderboard(
            @RequestParam int year,
            @RequestParam int quarter,
            @PageableDefault(size = 50) Pageable pageable // Tự động parse ?page=0&size=50
    ) {
        LeaderboardDTO.Response<Object> response = leaderboardService.getQuarterLeaderboard(year, quarter, null, pageable);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sync-batch")
    public ResponseEntity<String> syncBatch(@RequestBody List<WebhookPayload> payloads) {
        leaderboardService.processBatchSync(payloads);
        return ResponseEntity.ok("Đã xử lý Batch thành công");
    }
}
