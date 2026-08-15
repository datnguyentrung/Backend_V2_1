package com.dat.ai_receptionist_web.service.Projection.handler;

import com.dat.ai_receptionist_web.enums.Infrastructure.ProjectionType;
import com.dat.ai_receptionist_web.service.Projection.ProjectionJob;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
public class FitnessRecordsCacheProjectionHandler implements ProjectionHandler {
    private final CacheManager cacheManager;

    public FitnessRecordsCacheProjectionHandler(
            @Qualifier("redisCacheManager") CacheManager cacheManager
    ) {
        this.cacheManager = cacheManager;
    }

    @Override
    public ProjectionType supports() {
        return ProjectionType.FITNESS_RECORDS_CACHE;
    }

    @Override
    public void process(ProjectionJob job) {
        Cache cache = cacheManager.getCache("fitnessRecords");
        if (cache == null) {
            throw new IllegalStateException("Missing fitnessRecords cache");
        }
        cache.clear();
    }
}
