package com.dat.backend_v2_1.service.Report;

import com.dat.backend_v2_1.domain.Core.Student;
import com.dat.backend_v2_1.dto.Core.StudentResDTO;
import com.dat.backend_v2_1.dto.Report.LeaderboardDTO;
import com.dat.backend_v2_1.dto.Report.YearlySummaryDTO;
import com.dat.backend_v2_1.enums.Core.StudentStatus;
import com.dat.backend_v2_1.mapper.Report.LeaderboardMapper;
import com.dat.backend_v2_1.mapper.Report.YearlySummaryMapper;
import com.dat.backend_v2_1.repository.Core.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class LeaderboardService {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, Object> redisTemplate; // Dùng để lưu/đọc JSON Object phẳng
    private final StudentRepository studentRepository;
    private final StudentSummaryService studentSummaryService;
    private final YearlySummaryMapper yearlySummaryMapper;
    private final LeaderboardMapper leaderboardMapper;

    /**
     * Lấy Bảng xếp hạng từ Redis
     * Kết hợp: ZSET (Thứ tự) + HASH (Dữ liệu chi tiết phẳng) + DB (Thông tin cá nhân rút gọn)
     */
    public LeaderboardDTO.Response getQuarterLeaderboard(int year, int quarter, List<String> scheduleIds, Pageable pageable) {
        String redisKey = String.format("leaderboard:%d:Q%d", year, quarter);
        String redisDataKey = String.format("leaderboard_data:%d:Q%d", year, quarter);

        long start = pageable.getOffset();
        long end = start + pageable.getPageSize() - 1;

        // 1. Lấy danh sách mã học viên từ ZSET (Sắp xếp theo score giảm dần)
        Set<String> studentCodes = stringRedisTemplate.opsForZSet().reverseRange(redisKey, start, end);

        // 2. CƠ CHẾ TỰ PHỤC HỒI (SELF-HEALING)
        if (studentCodes == null || studentCodes.isEmpty()) {
            log.warn("⚠️ Redis Leaderboard rỗng! Đang tiến hành đồng bộ lại từ Database...");
            boolean isRebuilt = rebuildLeaderboardCache(year, quarter, redisKey, redisDataKey);
            if (isRebuilt) {
                studentCodes = stringRedisTemplate.opsForZSet().reverseRange(redisKey, start, end);
            }
        }

        if (studentCodes == null || studentCodes.isEmpty()) {
            return LeaderboardDTO.Response.builder()
                    .year(year).quarter(quarter).totalStudents(0).rankings(Collections.emptyList())
                    .build();
        }

        List<String> codesList = new ArrayList<>(studentCodes);

        // 3. TỐI ƯU TRUY VẤN:
        // - Lấy Tên, Đai từ DB bằng DTO Projection (Chỉ lấy cột cần thiết)
        // - Lấy Stats chi tiết (JSON phẳng) từ Redis Hash bằng MultiGet (Chỉ 1 request)
        Map<String, StudentResDTO.StudentRankInfo> studentMap = studentRepository.findRankInfoByStudentCodeIn(codesList)
                .stream().collect(Collectors.toMap(StudentResDTO.StudentRankInfo::getStudentCode, Function.identity()));

        List<Object> rawSummaries = redisTemplate.opsForHash().multiGet(redisDataKey, new ArrayList<>(codesList));

        // 4. MAP dồn dữ liệu vào RankItemForRedis (Sử dụng MapStruct để trải phẳng)
        List<LeaderboardDTO.RankItemForRedis> rankings = new ArrayList<>();
        int currentRank = (int) start + 1;

        for (int i = 0; i < codesList.size(); i++) {
            String code = codesList.get(i);
            StudentResDTO.StudentRankInfo info = studentMap.get(code);
            YearlySummaryDTO.QuarterSummaryForRedis summary = (YearlySummaryDTO.QuarterSummaryForRedis) rawSummaries.get(i);

            if (info != null && summary != null) {
                rankings.add(leaderboardMapper.toRankItemForRedis(currentRank++, info, summary));
            }
        }

        Long totalStudents = stringRedisTemplate.opsForZSet().zCard(redisKey);

        return LeaderboardDTO.Response.builder()
                .year(year)
                .quarter(quarter)
                .totalStudents(totalStudents != null ? totalStudents.intValue() : 0)
                .rankings(leaderboardMapper.toRankItemList(rankings))
                .build();
    }

    /**
     * Hàm tính lại toàn bộ bảng xếp hạng
     * Sử dụng GROUP BY trong SQL để xử lý hàng loạt 1 lần duy nhất
     */
    private boolean rebuildLeaderboardCache(int year, int quarter, String redisKey, String redisDataKey) {
        List<Student> activeStudents = studentRepository.findAllByStudentStatus(StudentStatus.ACTIVE);
        if (activeStudents.isEmpty()) return false;

        // Xóa cache cũ để đồng bộ mới hoàn toàn
        stringRedisTemplate.delete(redisKey);
        redisTemplate.delete(redisDataKey);

        // Tính toán hàng loạt (1 câu SQL duy nhất nhờ GROUP BY)
        Map<String, YearlySummaryDTO.QuarterSummary> batchSummaries =
                studentSummaryService.calculateBatchQuarterSummary(activeStudents, year, quarter);

        Map<String, Double> scoresMap = new HashMap<>();
        Map<String, Object> detailsMap = new HashMap<>();

        for (Student student : activeStudents) {
            String code = student.getStudentCode();
            YearlySummaryDTO.QuarterSummary summary = batchSummaries.get(code);

            if (summary != null) {
                // MapStruct biến đổi từ cấu trúc lồng ở DB sang cấu trúc phẳng cho Redis
                YearlySummaryDTO.QuarterSummaryForRedis flatSummary = yearlySummaryMapper.toQuarterSummaryForRedis(summary);

                scoresMap.put(code, flatSummary.getTotalQuarterScore());
                detailsMap.put(code, flatSummary);
            }
        }

        // Lưu vào Redis (Sử dụng Pipeline hoặc Bulk Update)
        if (!scoresMap.isEmpty()) {
            // Nạp ZSET
            scoresMap.forEach((code, score) -> stringRedisTemplate.opsForZSet().add(redisKey, code, score));
            // Nạp HASH (JSON phẳng)
            redisTemplate.opsForHash().putAll(redisDataKey, detailsMap);
        }

        log.info("✅ Đã khôi phục dữ liệu cho {} học viên (Batch SQL + Flat JSON).", activeStudents.size());
        return true;
    }
}