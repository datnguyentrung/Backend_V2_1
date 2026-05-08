package com.dat.backend_v2_1.repository.Core;

import com.dat.backend_v2_1.domain.Core.Fitness;
import com.dat.backend_v2_1.dto.Core.FitnessId;
import org.jspecify.annotations.NonNull;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FitnessRepository extends JpaRepository<Fitness, FitnessId> {
    @Override
    @NonNull
    @Cacheable(value = "fitnessCache", key = "'allFitness'")
    List<Fitness> findAll();
}
