package com.dat.ai_receptionist_web.service.Report;

import com.dat.ai_receptionist_web.dto.Report.LeaderboardDTO;
import com.dat.ai_receptionist_web.dto.Report.LeaderboardMember;
import com.dat.ai_receptionist_web.dto.Report.YearlySummaryDTO;
import com.dat.ai_receptionist_web.dto.Skill.FitnessRecordDTO;
import com.dat.ai_receptionist_web.enums.Skill.SkillLevel;
import com.dat.ai_receptionist_web.mapper.Report.YearlySummaryMapper;
import com.dat.ai_receptionist_web.util.error.LeaderboardUnavailableException;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderboardService {
    private final LeaderboardRedisStore redisStore;
    private final YearlySummaryMapper yearlySummaryMapper;
    private final MeterRegistry meterRegistry;

    public LeaderboardDTO.Response<YearlySummaryDTO.QuarterSummary> getQuarterLeaderboard(
            int year,
            int quarter,
            Pageable pageable
    ) {
        LeaderboardScope scope = LeaderboardScope.quarter(year, quarter);
        LeaderboardRedisStore.Page page = read(scope, pageable);
        List<LeaderboardDTO.RankItem<YearlySummaryDTO.QuarterSummary>> rankings = new ArrayList<>();
        int rank = Math.toIntExact(pageable.getOffset()) + 1;

        for (LeaderboardRedisStore.Row row : page.rows()) {
            LeaderboardMember member = redisStore.decode(row.memberJson(), LeaderboardMember.class);
            YearlySummaryDTO.QuarterSummaryForRedis stored = redisStore.decode(
                    row.dataJson(), YearlySummaryDTO.QuarterSummaryForRedis.class
            );
            rankings.add(LeaderboardDTO.RankItem.<YearlySummaryDTO.QuarterSummary>builder()
                    .rank(rank++)
                    .studentCode(member.studentCode())
                    .fullName(member.fullName())
                    .belt(member.belt())
                    .data(yearlySummaryMapper.toQuarterSummary(stored))
                    .build());
        }

        return LeaderboardDTO.Response.<YearlySummaryDTO.QuarterSummary>builder()
                .year(year)
                .quarter(quarter)
                .totalStudents(page.totalEntries())
                .rankings(rankings)
                .build();
    }

    public LeaderboardDTO.Response<FitnessRecordDTO.Metrics> getFitnessLeaderboard(
            int year,
            int quarter,
            SkillLevel skillLevel,
            Pageable pageable
    ) {
        LeaderboardScope scope = LeaderboardScope.fitness(year, quarter, skillLevel);
        LeaderboardRedisStore.Page page = read(scope, pageable);
        List<LeaderboardDTO.RankItem<FitnessRecordDTO.Metrics>> rankings = new ArrayList<>();
        int rank = Math.toIntExact(pageable.getOffset()) + 1;

        for (LeaderboardRedisStore.Row row : page.rows()) {
            LeaderboardMember member = redisStore.decode(row.memberJson(), LeaderboardMember.class);
            rankings.add(LeaderboardDTO.RankItem.<FitnessRecordDTO.Metrics>builder()
                    .rank(rank++)
                    .rankBefore(row.rankBefore())
                    .studentCode(member.studentCode())
                    .fullName(member.fullName())
                    .belt(member.belt())
                    .data(redisStore.decode(row.dataJson(), FitnessRecordDTO.Metrics.class))
                    .build());
        }

        return LeaderboardDTO.Response.<FitnessRecordDTO.Metrics>builder()
                .year(year)
                .quarter(quarter)
                .totalStudents(page.totalEntries())
                .rankings(rankings)
                .build();
    }

    private LeaderboardRedisStore.Page read(LeaderboardScope scope, Pageable pageable) {
        LeaderboardRedisStore.Page page;
        try {
            page = redisStore.read(scope, pageable.getOffset(), pageable.getPageSize());
        } catch (LeaderboardUnavailableException exception) {
            meterRegistry.counter(
                    "leaderboard.read", "type", scope.type().name().toLowerCase(), "result", "unavailable"
            ).increment();
            log.error("LEADERBOARD_READ_UNAVAILABLE scope={}", scope.registryValue(), exception);
            throw exception;
        }
        if (!page.initialized()) {
            meterRegistry.counter("leaderboard.read", "type", scope.type().name().toLowerCase(), "result", "uninitialized")
                    .increment();
            log.warn("LEADERBOARD_SCOPE_UNINITIALIZED scope={}", scope.registryValue());
        }
        return page;
    }
}
