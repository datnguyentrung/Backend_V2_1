package com.dat.ai_receptionist_web.repository.Core;

import com.dat.ai_receptionist_web.domain.Core.Fitness;
import com.dat.ai_receptionist_web.dto.Core.FitnessId;
import org.jspecify.annotations.NonNull;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FitnessRepository extends JpaRepository<Fitness, FitnessId> {
    @Override
    @NonNull
    @Cacheable(value = "fitnessCache", key = "'allFitness'", cacheManager = "redisCacheManager")
    List<Fitness> findAll();
}
