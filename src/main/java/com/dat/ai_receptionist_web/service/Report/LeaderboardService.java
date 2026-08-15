package com.dat.ai_receptionist_web.service.Report;

import com.dat.ai_receptionist_web.dto.Report.LeaderboardDTO;
import com.dat.ai_receptionist_web.dto.Report.LeaderboardMember;
import com.dat.ai_receptionist_web.dto.Report.YearlySummaryDTO;
import com.dat.ai_receptionist_web.dto.Skill.FitnessRecordDTO;
import com.dat.ai_receptionist_web.enums.Skill.SkillLevel;
import com.dat.ai_receptionist_web.mapper.Report.YearlySummaryMapper;
import com.dat.ai_receptionist_web.service.Core.PersonAvatarUrlCacheService;
import com.dat.ai_receptionist_web.util.error.LeaderboardUnavailableException;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderboardService {
    private final LeaderboardRedisStore redisStore;
    private final YearlySummaryMapper yearlySummaryMapper;
    private final MeterRegistry meterRegistry;
    private final PersonAvatarUrlCacheService avatarUrlCacheService;

    public LeaderboardDTO.Response<YearlySummaryDTO.QuarterSummary> getQuarterLeaderboard(
            int year,
            int quarter,
            Pageable pageable
    ) {
        LeaderboardScope scope = LeaderboardScope.quarter(year, quarter);
        LeaderboardRedisStore.Page page = read(scope, pageable);
        List<LeaderboardMember> members = page.rows().stream()
                .map(row -> redisStore.decode(row.memberJson(), LeaderboardMember.class))
                .toList();
        Map<UUID, String> avatarUrls = avatarUrlCacheService.getMany(
                members.stream().map(LeaderboardMember::personId).toList()
        );
        List<LeaderboardDTO.RankItem<YearlySummaryDTO.QuarterSummary>> rankings = new ArrayList<>();
        int rank = Math.toIntExact(pageable.getOffset()) + 1;

        for (int index = 0; index < page.rows().size(); index++) {
            LeaderboardRedisStore.Row row = page.rows().get(index);
            LeaderboardMember member = members.get(index);
            YearlySummaryDTO.QuarterSummaryForRedis stored = redisStore.decode(
                    row.dataJson(), YearlySummaryDTO.QuarterSummaryForRedis.class
            );
            rankings.add(LeaderboardDTO.RankItem.<YearlySummaryDTO.QuarterSummary>builder()
                    .rank(rank++)
                    .personId(member.personId())
                    .avatarUrl(avatarUrls.get(member.personId()))
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
        List<LeaderboardMember> members = page.rows().stream()
                .map(row -> redisStore.decode(row.memberJson(), LeaderboardMember.class))
                .toList();
        Map<UUID, String> avatarUrls = avatarUrlCacheService.getMany(
                members.stream().map(LeaderboardMember::personId).toList()
        );
        List<LeaderboardDTO.RankItem<FitnessRecordDTO.Metrics>> rankings = new ArrayList<>();
        int rank = Math.toIntExact(pageable.getOffset()) + 1;

        for (int index = 0; index < page.rows().size(); index++) {
            LeaderboardRedisStore.Row row = page.rows().get(index);
            LeaderboardMember member = members.get(index);
            rankings.add(LeaderboardDTO.RankItem.<FitnessRecordDTO.Metrics>builder()
                    .rank(rank++)
                    .rankBefore(row.rankBefore())
                    .personId(member.personId())
                    .avatarUrl(avatarUrls.get(member.personId()))
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
