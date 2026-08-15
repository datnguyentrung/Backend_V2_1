package com.dat.ai_receptionist_web.service.Projection.handler;

import com.dat.ai_receptionist_web.enums.Infrastructure.ProjectionType;
import com.dat.ai_receptionist_web.service.Projection.ProjectionJob;
import com.dat.ai_receptionist_web.service.Report.LeaderboardProjectionQueryService;
import com.dat.ai_receptionist_web.service.Report.LeaderboardProjectionWriter;
import com.dat.ai_receptionist_web.service.Report.LeaderboardRedisStore;
import com.dat.ai_receptionist_web.service.Report.LeaderboardScope;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberLeaderboardProjectionHandler implements ProjectionHandler {
    private final LeaderboardProjectionQueryService queryService;
    private final LeaderboardProjectionWriter writer;
    private final LeaderboardRedisStore redisStore;
    private final ObjectMapper objectMapper;

    @Override
    public ProjectionType supports() {
        return ProjectionType.LEADERBOARD_MEMBER;
    }

    @Override
    public void process(ProjectionJob job) {
        boolean membershipChanged = readMembershipChanged(job.payload());
        var memberState = queryService.loadMember(job.aggregateKey());
        for (LeaderboardScope scope : redisStore.registeredScopes()) {
            if (!membershipChanged) {
                writer.updateMemberIfPresent(memberState, scope);
            } else if (scope.type() == LeaderboardScope.Type.QUARTER) {
                writer.apply(queryService.loadConduct(job.aggregateKey(), scope.year(), scope.quarter()));
            } else {
                writer.apply(queryService.loadFitness(
                        job.aggregateKey(), scope.year(), scope.quarter(), scope.skillLevel()
                ));
            }
        }
    }

    private boolean readMembershipChanged(String payload) {
        try {
            return objectMapper.readTree(payload).path("membershipChanged").asBoolean(false);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid LEADERBOARD_MEMBER payload", exception);
        }
    }
}
