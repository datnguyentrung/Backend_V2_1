package com.dat.ai_receptionist_web.util.Helper;

import com.dat.ai_receptionist_web.domain.Core.Fitness;
import com.dat.ai_receptionist_web.dto.Skill.FitnessRecordDTO;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class SkillCalculator {
    public int calculateAndSetLevels(
            FitnessRecordDTO.Metrics response,
            List<Fitness> benchmarkList) {

        // 1. Lọc và sắp xếp benchmark
        List<Fitness> benchmarks = benchmarkList.stream()
                .filter(f -> f.getSkillLevel() == response.getSkillLevel())
                .sorted(Comparator.comparingInt(Fitness::getFitnessLevel))
                .toList();

        int durationLevel = 0;
        int amountLevel = 0;

        for (Fitness b : benchmarks) {
            // (Lưu ý: Nếu bài tập tính thời gian hoàn thành càng Nhanh càng tốt,
            // thì chỗ này phải là <= b.getDuration(). Còn nếu là bài tập Sinh Tồn như Plank, thì >= là đúng)
            if (response.getDuration() >= b.getDuration()) {
                durationLevel = b.getFitnessLevel();
            }
            if (response.getAmount() >= b.getAmount()) {
                amountLevel = b.getFitnessLevel();
            }
        }

        response.setDurationLevel(durationLevel);
        response.setAmountLevel(amountLevel);

        // 2. Lấy level thấp nhất (Bottleneck)
        int skillLevel = Math.min(durationLevel, amountLevel);

        // 3. Xử lý isQualified AN TOÀN & TỐI ƯU
        boolean isQualified = false;

        Fitness finalBenchmark = benchmarks.stream()
                .filter(b -> b.getFitnessLevel() == skillLevel)
                .findFirst()
                .orElse(null);

        // Thay thế assert bằng IF an toàn
        if (finalBenchmark != null) {
            // TỐI ƯU HÓA: Dùng phép Nhân Chéo (Cross-multiplication) thay vì phép Chia (Division)
            // Thay vì so sánh: (amount / duration) >= (benchAmount / benchDuration)
            // Ta nhân chéo: amount * benchDuration >= benchAmount * duration
            // => Tính trên số nguyên (long), nhanh hơn, chính xác tuyệt đối 100% và không bao giờ bị lỗi chia cho 0.
            long userVelocity = (long) response.getAmount() * finalBenchmark.getDuration();
            long benchVelocity = (long) finalBenchmark.getAmount() * response.getDuration();

            isQualified = userVelocity >= benchVelocity;
        }

        response.setIsQualified(isQualified);

        // Trả về level thấp nhất
        return skillLevel;
    }
}
