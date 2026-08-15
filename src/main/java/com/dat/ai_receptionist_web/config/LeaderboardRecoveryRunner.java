package com.dat.ai_receptionist_web.config;

import com.dat.ai_receptionist_web.enums.Skill.SkillLevel;
import com.dat.ai_receptionist_web.service.Report.LeaderboardRecoveryService;
import com.dat.ai_receptionist_web.service.Report.LeaderboardScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "leaderboard.rebuild.enabled", havingValue = "true")
@Slf4j
public class LeaderboardRecoveryRunner implements ApplicationRunner {
    private final LeaderboardRecoveryService recoveryService;
    private final Environment environment;
    private final ConfigurableApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) {
        requireNonWebMode();
        String type = required("leaderboard.rebuild.type").toLowerCase();
        int year = Integer.parseInt(required("leaderboard.rebuild.year"));
        int quarter = Integer.parseInt(required("leaderboard.rebuild.quarter"));

        List<LeaderboardScope> scopes = switch (type) {
            case "quarter" -> List.of(LeaderboardScope.quarter(year, quarter));
            case "fitness" -> fitnessScopes(year, quarter);
            case "all" -> {
                List<LeaderboardScope> all = new java.util.ArrayList<>();
                all.add(LeaderboardScope.quarter(year, quarter));
                all.addAll(fitnessScopes(year, quarter));
                yield all;
            }
            default -> throw new IllegalArgumentException("leaderboard.rebuild.type must be quarter, fitness, or all");
        };

        for (LeaderboardScope scope : scopes) {
            recoveryService.rebuild(scope);
        }
        log.info("LEADERBOARD_RECOVERY_COMMAND_COMPLETED scopes={}", scopes.size());
        applicationContext.close();
    }

    private List<LeaderboardScope> fitnessScopes(int year, int quarter) {
        String configuredSkillLevel = environment.getProperty("leaderboard.rebuild.skill-level");
        if (configuredSkillLevel != null && !configuredSkillLevel.isBlank()) {
            return List.of(LeaderboardScope.fitness(year, quarter, SkillLevel.valueOf(configuredSkillLevel)));
        }
        return Arrays.stream(SkillLevel.values())
                .map(skillLevel -> LeaderboardScope.fitness(year, quarter, skillLevel))
                .toList();
    }

    private String required(String property) {
        String value = environment.getProperty(property);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required property: " + property);
        }
        return value;
    }

    private void requireNonWebMode() {
        String webType = environment.getProperty("spring.main.web-application-type", "servlet");
        if (!"none".equalsIgnoreCase(webType)) {
            throw new IllegalStateException("Leaderboard recovery must run with spring.main.web-application-type=none");
        }
    }
}
