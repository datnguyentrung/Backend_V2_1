package com.dat.ai_receptionist_web.controller.Core;

import com.dat.ai_receptionist_web.domain.Core.Fitness;
import com.dat.ai_receptionist_web.enums.Skill.SkillLevel;
import com.dat.ai_receptionist_web.service.Core.FitnessService;
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
