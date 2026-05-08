package com.dat.backend_v2_1.controller.Core;

import com.dat.backend_v2_1.domain.Core.Fitness;
import com.dat.backend_v2_1.enums.Skill.SkillLevel;
import com.dat.backend_v2_1.service.Core.FitnessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/fitness")
public class FitnessController {
    private final FitnessService fitnessService;

    @GetMapping
    public ResponseEntity<List<Fitness>> getBySkillLevel(@RequestParam SkillLevel skillLevel) {
        log.info("Request get fitness by skill level: {}", skillLevel);
        return ResponseEntity.ok(fitnessService.getFitnessBySkillLevel(skillLevel));
    }
}
