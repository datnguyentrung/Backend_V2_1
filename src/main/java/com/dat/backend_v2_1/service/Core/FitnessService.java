package com.dat.backend_v2_1.service.Core;

import com.dat.backend_v2_1.domain.Core.Fitness;
import com.dat.backend_v2_1.enums.Skill.SkillLevel;
import com.dat.backend_v2_1.repository.Core.FitnessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FitnessService {
    private final FitnessRepository fitnessRepository;

    public List<Fitness> getAllFitness() {
        return fitnessRepository.findAll();
    }

    public List<Fitness> getFitnessBySkillLevel(SkillLevel skillLevel) {
        // Cuộc gọi này sẽ chọc xuống Repo, và đi qua Proxy của Repo (được cache)
        return fitnessRepository.findAll().stream()
                .filter(fitness -> fitness.getSkillLevel() == skillLevel)
                .toList();
    }
}
