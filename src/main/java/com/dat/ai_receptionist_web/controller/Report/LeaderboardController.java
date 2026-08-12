package com.dat.ai_receptionist_web.controller.Report;

import com.dat.ai_receptionist_web.dto.Report.LeaderboardDTO;
import com.dat.ai_receptionist_web.dto.Report.YearlySummaryDTO;
import com.dat.ai_receptionist_web.dto.Skill.FitnessRecordDTO;
import com.dat.ai_receptionist_web.enums.Skill.SkillLevel;
import com.dat.ai_receptionist_web.service.Report.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

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
        validateQuarter(quarter);
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
        validateQuarter(quarter);
        // Ép kiểu chuẩn xác cho Response
        LeaderboardDTO.Response<YearlySummaryDTO.QuarterSummary> response =
                leaderboardService.getQuarterLeaderboard(year, quarter, pageable);

        return ResponseEntity.ok(response);
    }

    private void validateQuarter(int quarter) {
        if (quarter < 1 || quarter > 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quarter must be between 1 and 4");
        }
    }
}
