package com.dat.backend_v2_1.service.Report;

import com.dat.backend_v2_1.domain.Core.Fitness;
import com.dat.backend_v2_1.domain.Core.Student;
import com.dat.backend_v2_1.domain.Skill.FitnessRecord;
import com.dat.backend_v2_1.dto.Core.StudentResDTO;
import com.dat.backend_v2_1.dto.Report.LeaderboardDTO;
import com.dat.backend_v2_1.dto.Report.YearlySummaryDTO;
import com.dat.backend_v2_1.dto.Skill.FitnessRecordDTO;
import com.dat.backend_v2_1.dto.WebhookPayload;
import com.dat.backend_v2_1.enums.Core.StudentStatus;
import com.dat.backend_v2_1.enums.Skill.SkillLevel;
import com.dat.backend_v2_1.mapper.Report.LeaderboardMapper;
import com.dat.backend_v2_1.mapper.Report.YearlySummaryMapper;
import com.dat.backend_v2_1.mapper.Skill.FitnessRecordMapper;
import com.dat.backend_v2_1.repository.Core.StudentRepository;
import com.dat.backend_v2_1.repository.Skill.FitnessRecordRepository;
import com.dat.backend_v2_1.service.Core.FitnessService;
import com.dat.backend_v2_1.util.Helper.SkillCalculator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;

import java.time.Duration;
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
    private final FitnessRecordRepository fitnessRecordRepository;
    private final FitnessService fitnessService;
    private final FitnessRecordMapper fitnessRecordMapper;
    private final SkillCalculator skillCalculator;
    private final ObjectMapper objectMapper;

    /**
     * Lấy Bảng xếp hạng từ Redis
     * Kết hợp: ZSET (Thứ tự) + HASH (Dữ liệu chi tiết phẳng) + DB (Thông tin cá nhân rút gọn)
     */
    public LeaderboardDTO.Response<Object> getQuarterLeaderboard(int year, int quarter, List<String> scheduleIds, Pageable pageable) {
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

    public LeaderboardDTO.Response<FitnessRecordDTO.Metrics> getFitnessLeaderboard(int year, int quarter, SkillLevel skillLevel, Pageable pageable) {
        // 1. Key có thêm SkillLevel để lọc ngay từ đầu
        String redisKey = String.format("leaderboard:fitness:%d:Q%d:%s", year, quarter, skillLevel);
        String redisDataKey = String.format("leaderboard_data:fitness:%d:Q%d:%s", year, quarter, skillLevel);
        String redisHistoryKey = String.format("leaderboard_history:fitness:%d:Q%d:%s", year, quarter, skillLevel);

        long start = pageable.getOffset();
        long end = start + pageable.getPageSize() - 1;

        // 2. Lấy danh sách từ ZSET
        Set<String> studentCodes = stringRedisTemplate.opsForZSet().reverseRange(redisKey, start, end);

        // 3. Tự phục hồi (Self-healing)
        if (studentCodes == null || studentCodes.isEmpty()) {
            log.warn("⚠️ Fitness Leaderboard {} rỗng! Đang đồng bộ từ DB...", skillLevel);
            boolean isRebuilt = rebuildFitnessLeaderboardCache(year, quarter, skillLevel, redisKey, redisDataKey);
            if (isRebuilt) {
                studentCodes = stringRedisTemplate.opsForZSet().reverseRange(redisKey, start, end);
            }
        }

        if (studentCodes == null || studentCodes.isEmpty()) {
            // SỬA Ở ĐÂY: Chỉ định rõ Generic Type cho Builder
            return LeaderboardDTO.Response.<FitnessRecordDTO.Metrics>builder()
                    .year(year)
                    .quarter(quarter)
                    .rankings(Collections.emptyList())
                    .build();
        }

        List<String> codesList = new ArrayList<>(studentCodes);

        // 4. Tối ưu truy vấn: Map Student Info (DB) + Fitness Stats (Redis Hash)
        Map<String, StudentResDTO.StudentRankInfo> studentMap = studentRepository.findRankInfoByStudentCodeIn(codesList)
                .stream().collect(Collectors.toMap(StudentResDTO.StudentRankInfo::getStudentCode, Function.identity()));

        // MultiGet từ Hash để lấy object Response đã lưu sẵn
        List<Object> rawRecords = redisTemplate.opsForHash().multiGet(redisDataKey, new ArrayList<>(codesList));
        List<Object> rawHistories = stringRedisTemplate.opsForHash().multiGet(redisHistoryKey, new ArrayList<>(codesList));

        // 5. Build kết quả trả về
        List<LeaderboardDTO.RankItem<FitnessRecordDTO.Metrics>> rankings = new ArrayList<>();
        int currentRank = (int) start + 1;

        for (int i = 0; i < codesList.size(); i++) {
            String code = codesList.get(i);
            StudentResDTO.StudentRankInfo info = studentMap.get(code);

            Object rawData = rawRecords.get(i);
            FitnessRecordDTO.Metrics fitnessData =
                    objectMapper.convertValue(rawData, FitnessRecordDTO.Metrics.class);

            // Đọc Rank History
            Object historyObj = rawHistories.get(i);
            Integer rankBefore = null;
            if (historyObj != null) {
                rankBefore = Integer.parseInt(historyObj.toString());
            }

            if (info != null && fitnessData != null) {
                LeaderboardDTO.RankItem<FitnessRecordDTO.Metrics> item = leaderboardMapper
                        .toRankItemFromFitness(currentRank++, info, fitnessData);

                // Set thêm rankBefore vào DTO
                item.setRankBefore(rankBefore);

                rankings.add(item);
            }
        }

        return LeaderboardDTO.Response.<FitnessRecordDTO.Metrics>builder()
                .year(year).quarter(quarter)
                .totalStudents(stringRedisTemplate.opsForZSet().zCard(redisKey).intValue())
                // TRUYỀN THẲNG RANKINGS VÀO ĐÂY, BỎ QUA toRankItemList()
                .rankings(rankings)
                .build();
    }

    private boolean rebuildFitnessLeaderboardCache(int year, int quarter, SkillLevel skillLevel, String redisKey, String redisDataKey) {
        List<FitnessRecord> allRecords = fitnessRecordRepository.findBestRecordsForQuarter(year, quarter, skillLevel);

        if (allRecords.isEmpty()) return false;

        List<Fitness> benchmarks = fitnessService.getAllFitness();
        Map<String, Double> scoresMap = new HashMap<>();
        Map<String, Object> detailsMap = new HashMap<>();

        // 1. Thêm Set để đánh dấu học viên đã được xử lý
        Set<String> processedStudents = new HashSet<>();

        for (FitnessRecord record : allRecords) {
            String code = record.getStudent().getStudentCode();

            // 2. CHỈ XỬ LÝ NẾU CHƯA GẶP HỌC VIÊN NÀY (Bản ghi đầu tiên là bản ghi tốt nhất)
            if (processedStudents.add(code)) {
                FitnessRecordDTO.Metrics dto = fitnessRecordMapper.toMetrics(record);
                int finalLevel = skillCalculator.calculateAndSetLevels(dto, benchmarks);
                dto.setFitnessLevel(finalLevel);

                double score = getScore(dto);

                scoresMap.put(code, score);
                detailsMap.put(code, dto);
            }
        }

        if (!scoresMap.isEmpty()) {
            stringRedisTemplate.delete(redisKey); // Xóa sạch trước khi nạp lại
            redisTemplate.delete(redisDataKey);

            scoresMap.forEach((code, score) -> stringRedisTemplate.opsForZSet().add(redisKey, code, score));
            redisTemplate.opsForHash().putAll(redisDataKey, detailsMap);
        }

        return true;
    }

    public void updateFitnessLeaderboard(FitnessRecordDTO.Response response, String studentCode) {
        int year = response.getMetrics().getAssessmentDate().getYear();
        int quarter = (response.getMetrics().getAssessmentDate().getMonthValue() - 1) / 3 + 1;
        String skillLevel = response.getMetrics().getSkillLevel().toString();

        String redisKey = String.format("leaderboard:fitness:%d:Q%d:%s", year, quarter, skillLevel);
        String redisDataKey = String.format("leaderboard_data:fitness:%d:Q%d:%s", year, quarter, skillLevel);
        String redisHistoryKey = String.format("leaderboard_history:fitness:%d:Q%d:%s", year, quarter, skillLevel);

        // 1. SỬA SCORE: Phải dùng chung hệ số 1 tỷ và có tie-breaker giống hệt hàm Rebuild
        double score = getScore(response.getMetrics());
        Double currentScore = stringRedisTemplate.opsForZSet().score(redisKey, studentCode);

        if (currentScore == null || score > currentScore) {
            // --- LOGIC LƯU RANK CŨ ---
            // Lấy rank hiện tại (trước khi điểm mới được ghi nhận). ZREVRANK tính từ 0, nên cần + 1
            Long currentRankRaw = stringRedisTemplate.opsForZSet().reverseRank(redisKey, studentCode);
            if (currentRankRaw != null) {
                int currentRank = currentRankRaw.intValue() + 1;
                // Lưu hạng cũ vào Hash History
                stringRedisTemplate.opsForHash().put(redisHistoryKey, studentCode, String.valueOf(currentRank));
                stringRedisTemplate.expire(redisHistoryKey, Duration.ofDays(30));
            }
            // -------------------------

            stringRedisTemplate.opsForZSet().add(redisKey, studentCode, score);
            // 2. SỬA DATA: Chỉ lưu Metrics vào Hash để đồng bộ với hàm get()
            redisTemplate.opsForHash().put(redisDataKey, studentCode, response.getMetrics());

            stringRedisTemplate.expire(redisKey, Duration.ofDays(30));
            redisTemplate.expire(redisDataKey, Duration.ofDays(30));
            log.info("✅ [Leaderboard] Cập nhật kỷ lục đồng bộ cho: {}", studentCode);
        }
    }

    private double getScore(FitnessRecordDTO.Metrics metrics) {
        // 1. SỬA LỖI: Rút gọn và đẩy fitnessLevel ra ngoài scope.
        // Dùng Boolean.FALSE.equals() để chống NullPointerException hoàn toàn.
        int fitnessLevel = Boolean.FALSE.equals(metrics.getIsQualified())
                ? 0
                : Optional.ofNullable(metrics.getFitnessLevel()).orElse(0);

        int durationLevel = Optional.ofNullable(metrics.getDurationLevel()).orElse(0);
        int amountLevel = Optional.ofNullable(metrics.getAmountLevel()).orElse(0);

        int duration = (metrics.getDuration() != null && metrics.getDuration() > 0) ? metrics.getDuration() : 1;
        int amount = (metrics.getAmount() != null) ? metrics.getAmount() : 0;

        // Các thuật toán tính toán của bạn giữ nguyên vì logic phân tách nguyên/thập phân đã rất chuẩn
        long baseScore = (fitnessLevel * 10_000_000L)
                + (durationLevel * 1_000_000L)
                + (amountLevel * 100_000L)
                + Math.round(((double) amount / duration) * 10_000);

        double dateBonus = 0.0;
        if (metrics.getAssessmentDate() != null) {
            dateBonus = (100_000.0 - metrics.getAssessmentDate().toEpochDay()) / 100_000.0;
        }

        return baseScore + dateBonus;
    }

    public void syncSingleStudentFitnessLeaderboard(String studentCode, int year, int quarter, SkillLevel skillLevel) {
        String redisKey = String.format("leaderboard:fitness:%d:Q%d:%s", year, quarter, skillLevel);
        String redisDataKey = String.format("leaderboard_data:fitness:%d:Q%d:%s", year, quarter, skillLevel);
        String redisHistoryKey = String.format("leaderboard_history:fitness:%d:Q%d:%s", year, quarter, skillLevel);

        // 1. Tìm bản ghi tốt nhất của RIÊNG học viên này
        Optional<FitnessRecord> bestRecordOpt = fitnessRecordRepository.findBestRecordForSingleStudent(year, quarter, skillLevel, studentCode);

        if (bestRecordOpt.isPresent()) {
            // 2. Nếu có bản ghi -> Tính điểm và Cập nhật Redis
            FitnessRecord record = bestRecordOpt.get();
            FitnessRecordDTO.Metrics dto = fitnessRecordMapper.toMetrics(record);

            List<Fitness> benchmarks = fitnessService.getAllFitness();
            int finalLevel = skillCalculator.calculateAndSetLevels(dto, benchmarks);
            dto.setFitnessLevel(finalLevel);

            double score = getScore(dto);

            // --- BỔ SUNG LOGIC LƯU RANK CŨ TRƯỚC KHI GHI ĐÈ ---
            Double currentScore = stringRedisTemplate.opsForZSet().score(redisKey, studentCode);
            // Chỉ lưu rank cũ nếu học viên đã có điểm trước đó và điểm này khác với điểm chuẩn bị update
            if (currentScore != null && score != currentScore) {
                Long currentRankRaw = stringRedisTemplate.opsForZSet().reverseRank(redisKey, studentCode);
                if (currentRankRaw != null) {
                    stringRedisTemplate.opsForHash().put(redisHistoryKey, studentCode, String.valueOf(currentRankRaw.intValue() + 1));
                    stringRedisTemplate.expire(redisHistoryKey, Duration.ofDays(30));
                }
            }
            // ---------------------------------------------------

            // Ghi đè điểm mới vào ZSET (nếu đã có sẽ tự update điểm, chưa có sẽ thêm mới)
            stringRedisTemplate.opsForZSet().add(redisKey, studentCode, score);
            // Ghi đè data vào HASH
            redisTemplate.opsForHash().put(redisDataKey, studentCode, dto);

            log.info("✅ [Webhook] Đã cập nhật lại điểm cho học viên {} trong Redis.", studentCode);
        } else {
            // 3. Nếu không tìm thấy (ví dụ: record duy nhất trong quý vừa bị xóa khỏi Supabase) -> Xóa khỏi Redis
            stringRedisTemplate.opsForZSet().remove(redisKey, studentCode);
            redisTemplate.opsForHash().delete(redisDataKey, studentCode);
            stringRedisTemplate.opsForHash().delete(redisHistoryKey, studentCode);

            log.info("🗑️ [Webhook] Đã xóa học viên {} khỏi Leaderboard Redis do không còn dữ liệu.", studentCode);
        }
    }

    public void processBatchSync(List<WebhookPayload<FitnessRecordDTO.Metrics>> payloads) {
        List<Fitness> benchmarks = fitnessService.getAllFitness();
        Set<String> requiresDbSync = new HashSet<>();
        Map<String, WebhookPayload<FitnessRecordDTO.Metrics>> bestInserts = new HashMap<>();

        // 1. Phân loại và tính toán điểm
        for (WebhookPayload<FitnessRecordDTO.Metrics> payload : payloads) {
            FitnessRecordDTO.Metrics metrics = payload.getData();

            // ✅ CHECK AN TOÀN: Nếu không có metrics mà hành động không phải DELETE thì bỏ qua luôn
            // (DELETE thường không cần metrics, chỉ cần studentCode và metadata)
            if (metrics == null && !"DELETE".equals(payload.getAction())) {
                log.warn("⚠️ Bỏ qua payload của học viên {} vì Metrics bị null!", payload.getStudentCode());
                continue; // Nhảy sang vòng lặp tiếp theo ngay lập tức
            }

            // 2. Tính toán fitness level (Chỉ thực hiện nếu có metrics)
            if (metrics != null) {
                int finalLevel = skillCalculator.calculateAndSetLevels(metrics, benchmarks);
                metrics.setFitnessLevel(finalLevel);
            }

            String key = String.format("%s|%d|%d|%s",
                    payload.getStudentCode(), payload.getYear(), payload.getQuarter(), payload.getSkillLevel());

            if ("DELETE".equals(payload.getAction()) || "UPDATE".equals(payload.getAction())) {
                requiresDbSync.add(key);
                bestInserts.remove(key);
            } else if ("INSERT".equals(payload.getAction())) {
                // ✅ Lúc này IDE sẽ hết báo lỗi vì nó biết chắc chắn metrics không thể null ở đây nhờ lệnh continue phía trên
                if (!requiresDbSync.contains(key)) {
                    double incomingScore = getScore(metrics);
                    WebhookPayload<FitnessRecordDTO.Metrics> existing = bestInserts.get(key);

                    if (existing == null || incomingScore > getScore(existing.getData())) {
                        // --- THÊM LOGIC CHECK ĐIỂM CŨ TRONG REDIS ---
                        String redisKey = String.format("leaderboard:fitness:%d:Q%d:%s",
                                payload.getYear(), payload.getQuarter(), payload.getSkillLevel());

                        Double currentRedisScore = stringRedisTemplate.opsForZSet().score(redisKey, payload.getStudentCode());

                        // CHỈ ĐƯA VÀO BATCH NẾU ĐIỂM MỚI > ĐIỂM CŨ TRONG REDIS (hoặc chưa từng có điểm)
                        if (currentRedisScore == null || incomingScore > currentRedisScore) {
                            bestInserts.put(key, payload);
                        } else {
                            log.info("Bỏ qua học viên {} vì điểm mới {} <= điểm cũ {}", payload.getStudentCode(), incomingScore, currentRedisScore);
                        }
                    }
                }
            }
        }

        // 2. Xử lý nhóm cần đồng bộ DB (Giữ nguyên logic cũ)
        for (String key : requiresDbSync) {
            String[] parts = key.split("\\|");
            syncSingleStudentFitnessLeaderboard(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), SkillLevel.valueOf(parts[3]));
        }

        // 3. Pipeline cập nhật Redis
        if (!bestInserts.isEmpty()) {
            log.info("⚡ Đang thực hiện Pipeline cập nhật {} bản ghi lên Redis...", bestInserts.size());

            // --- BỔ SUNG: LẤY RANK CŨ CỦA TẤT CẢ HỌC VIÊN TRƯỚC KHI CHẠY PIPELINE ---
            Map<String, String> rankHistoryUpdates = new HashMap<>();
            for (WebhookPayload<FitnessRecordDTO.Metrics> payload : bestInserts.values()) {
                String redisKey = String.format("leaderboard:fitness:%d:Q%d:%s",
                        payload.getYear(), payload.getQuarter(), payload.getSkillLevel());

                // Fetch rank hiện tại
                Long currentRankRaw = stringRedisTemplate.opsForZSet().reverseRank(redisKey, payload.getStudentCode());
                if (currentRankRaw != null) {
                    String redisHistoryKey = String.format("leaderboard_history:fitness:%d:Q%d:%s",
                            payload.getYear(), payload.getQuarter(), payload.getSkillLevel());

                    // Cấu trúc map: "HistoryKey:StudentCode" -> "RankCũ"
                    rankHistoryUpdates.put(redisHistoryKey + ":" + payload.getStudentCode(), String.valueOf(currentRankRaw.intValue() + 1));
                }
            }
            // -----------------------------------------------------------------------

            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                // --- BỔ SUNG: LƯU RANK CŨ VÀO HASH TRONG PIPELINE ---
                for (Map.Entry<String, String> entry : rankHistoryUpdates.entrySet()) {
                    String[] parts = entry.getKey().split(":");
                    // parts[0] đến n-1 là historyKey, phần tử cuối là studentCode
                    String studentCode = parts[parts.length - 1];
                    String historyKey = entry.getKey().substring(0, entry.getKey().lastIndexOf(":"));

                    byte[] rawHistoryKey = stringRedisTemplate.getStringSerializer().serialize(historyKey);
                    byte[] rawField = stringRedisTemplate.getStringSerializer().serialize(studentCode);
                    byte[] rawValue = stringRedisTemplate.getStringSerializer().serialize(entry.getValue());

                    connection.hashCommands().hSet(rawHistoryKey, rawField, rawValue);
                    connection.keyCommands().expire(rawHistoryKey, Duration.ofDays(30).getSeconds());
                }
                // ----------------------------------------------------

                for (WebhookPayload<FitnessRecordDTO.Metrics> payload : bestInserts.values()) {
                    String studentCode = payload.getStudentCode();

                    // Trích xuất data ra biến rõ ràng
                    FitnessRecordDTO.Metrics metricsData = payload.getData();
                    double score = getScore(metricsData);

                    String redisKey = String.format("leaderboard:fitness:%d:Q%d:%s",
                            payload.getYear(), payload.getQuarter(), payload.getSkillLevel());
                    String redisDataKey = String.format("leaderboard_data:fitness:%d:Q%d:%s",
                            payload.getYear(), payload.getQuarter(), payload.getSkillLevel());

                    // Serialize Key và Value cho ZSET (dùng mặc định StringSerializer là chuẩn nhất cho Key)
                    byte[] rawKey = redisTemplate.getStringSerializer().serialize(redisKey);
                    byte[] rawValue = redisTemplate.getStringSerializer().serialize(studentCode);
                    connection.zSetCommands().zAdd(rawKey, score, rawValue);

                    try {
                        // --- ÉP KIỂU RÕ RÀNG ĐỂ TRÁNH LỖI "CAPTURE OF ?" ---
                        @SuppressWarnings("unchecked")
                        RedisSerializer<String> keySerializer = (RedisSerializer<String>) redisTemplate.getKeySerializer();

                        @SuppressWarnings("unchecked")
                        RedisSerializer<String> hashKeySerializer = (RedisSerializer<String>) redisTemplate.getHashKeySerializer();

                        @SuppressWarnings("unchecked")
                        RedisSerializer<Object> hashValueSerializer = (RedisSerializer<Object>) redisTemplate.getHashValueSerializer();

                        // 1. Serialize Hash Key (Tên của giỏ Hash)
                        byte[] rawHashKey = (keySerializer != null) ?
                                keySerializer.serialize(redisDataKey) :
                                redisTemplate.getStringSerializer().serialize(redisDataKey);

                        // 2. Serialize Hash Field (Mã học viên)
                        byte[] rawField = (hashKeySerializer != null) ?
                                hashKeySerializer.serialize(studentCode) :
                                redisTemplate.getStringSerializer().serialize(studentCode);

                        // 3. Serialize Hash Value (Object Metrics)
                        byte[] rawData = hashValueSerializer.serialize(metricsData);

                        // Lưu vào Hash
                        connection.hashCommands().hSet(rawHashKey, rawField, rawData);

                    } catch (Exception e) {
                        log.error("❌ Không thể nạp dữ liệu Redis cho học viên {}: {}", studentCode, e.getMessage());
                    }
                }
                return null;
            });
            log.info("✅ Đã hoàn tất Pipeline.");
        }
    }
}