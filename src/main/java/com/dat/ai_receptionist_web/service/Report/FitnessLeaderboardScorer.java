package com.dat.ai_receptionist_web.service.Report;

import com.dat.ai_receptionist_web.domain.Core.Fitness;
import com.dat.ai_receptionist_web.domain.Skill.FitnessRecord;
import com.dat.ai_receptionist_web.dto.Skill.FitnessRecordDTO;
import com.dat.ai_receptionist_web.mapper.Skill.FitnessRecordMapper;
import com.dat.ai_receptionist_web.util.Helper.SkillCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FitnessLeaderboardScorer {
    private final FitnessRecordMapper fitnessRecordMapper;
    private final SkillCalculator skillCalculator;

    public ScoredMetrics score(FitnessRecord record, List<Fitness> benchmarks) {
        FitnessRecordDTO.Metrics metrics = fitnessRecordMapper.toMetrics(record);
        metrics.setFitnessLevel(skillCalculator.calculateAndSetLevels(metrics, benchmarks));
        return new ScoredMetrics(metrics, calculateScore(metrics), record.getId());
    }

    public Optional<ScoredMetrics> selectBest(List<FitnessRecord> records, List<Fitness> benchmarks) {
        return records.stream()
                .map(record -> score(record, benchmarks))
                .max(Comparator.comparingDouble(ScoredMetrics::score)
                        .thenComparing(ScoredMetrics::recordId, Comparator.reverseOrder()));
    }

    private double calculateScore(FitnessRecordDTO.Metrics metrics) {
        int fitnessLevel = Boolean.FALSE.equals(metrics.getIsQualified())
                ? 0
                : Optional.ofNullable(metrics.getFitnessLevel()).orElse(0);
        int durationLevel = Optional.ofNullable(metrics.getDurationLevel()).orElse(0);
        int amountLevel = Optional.ofNullable(metrics.getAmountLevel()).orElse(0);
        int duration = metrics.getDuration() != null && metrics.getDuration() > 0 ? metrics.getDuration() : 1;
        int amount = Optional.ofNullable(metrics.getAmount()).orElse(0);

        long baseScore = fitnessLevel * 10_000_000L
                + durationLevel * 1_000_000L
                + amountLevel * 100_000L
                + Math.round((double) amount / duration * 10_000);

        if (metrics.getCreatedAt() == null) {
            return baseScore;
        }
        long epochSeconds = metrics.getCreatedAt().toEpochSecond(ZoneOffset.UTC);
        return baseScore + (10_000_000_000.0 - epochSeconds) / 10_000_000_000.0;
    }

    public record ScoredMetrics(FitnessRecordDTO.Metrics metrics, double score, Long recordId) {
    }
}
