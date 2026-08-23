package com.dat.ai_receptionist_web.repository.Skill;

import com.dat.ai_receptionist_web.enums.Core.ScheduleLevel;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import static org.assertj.core.api.Assertions.assertThat;

class FitnessRecordRepositoryContractTest {
    @Test
    void listQueryUsesCurrentFitnessRecordAndScheduleLevelPaths() throws Exception {
        Query query = FitnessRecordRepository.class
                .getMethod("findListRows", String.class, ScheduleLevel.class, Pageable.class)
                .getAnnotation(Query.class);

        assertThat(query.value())
                .contains("fr.recordDate", "fr.fitness.scheduleLevel", "student.personCode")
                .doesNotContain("fr.assessmentDate", "fr.skillLevel", "student.studentCode");
    }
}
