package com.dat.backend_v2_1.controller.Report;

import com.dat.backend_v2_1.dto.Report.LeaderboardDTO;
import com.dat.backend_v2_1.service.Report.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/leaderboards")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    /**
     * API Lấy bảng xếp hạng theo Quý
     * VD: GET /api/v1/leaderboards/quarter?year=2026&quarter=2&limit=50
     */
    @GetMapping("/quarter")
    @PreAuthorize("isAuthenticated()") // Hoặc phân quyền theo rule của bạn
    public ResponseEntity<LeaderboardDTO.Response> getQuarterLeaderboard(
            @RequestParam int year,
            @RequestParam int quarter,
            @PageableDefault(size = 50) Pageable pageable // Tự động parse ?page=0&size=50
    ) {
        LeaderboardDTO.Response response = leaderboardService.getQuarterLeaderboard(year, quarter, null, pageable);
        return ResponseEntity.ok(response);
    }
}
