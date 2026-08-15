package com.dat.ai_receptionist_web.service.Projection.handler;

import com.dat.ai_receptionist_web.enums.Infrastructure.ProjectionType;
import com.dat.ai_receptionist_web.service.Projection.ProjectionJob;
import com.dat.ai_receptionist_web.service.Report.LeaderboardProjectionQueryService;
import com.dat.ai_receptionist_web.service.Report.LeaderboardProjectionWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConductLeaderboardProjectionHandler implements ProjectionHandler {
    private final LeaderboardProjectionQueryService queryService;
    private final LeaderboardProjectionWriter writer;

    @Override
    public ProjectionType supports() {
        return ProjectionType.LEADERBOARD_CONDUCT;
    }

    @Override
    public void process(ProjectionJob job) {
        writer.apply(queryService.loadConduct(job.aggregateKey(), job.year(), job.quarter()));
    }
}
