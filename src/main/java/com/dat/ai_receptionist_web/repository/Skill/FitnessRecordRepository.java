package com.dat.ai_receptionist_web.repository.Skill;

import com.dat.ai_receptionist_web.domain.Skill.FitnessRecord;
import com.dat.ai_receptionist_web.enums.Skill.SkillLevel;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Collection;

@Repository
public interface FitnessRecordRepository extends JpaRepository<FitnessRecord, Long>, JpaSpecificationExecutor<FitnessRecord> {
    @Override
    @NonNull
    Page<FitnessRecord> findAll(
            @NonNull Specification<FitnessRecord> spec, @NonNull Pageable pageable
    );

    @EntityGraph(attributePaths = {"student"})
    @Query("""
                SELECT fr FROM FitnessRecord fr
                WHERE year(fr.assessmentDate) = :year
                  AND quarter(fr.assessmentDate) = :quarter
                  AND fr.skillLevel = :skillLevel
                  AND fr.student.studentCode IN :studentCodes
                ORDER BY fr.student.studentCode, fr.id
            """)
    List<FitnessRecord> findRecordsForQuarterAndStudents(
            @Param("year") int year,
            @Param("quarter") int quarter,
            @Param("skillLevel") SkillLevel skillLevel,
            @Param("studentCodes") Collection<String> studentCodes
    );

    @EntityGraph(attributePaths = {"student"})
    @Query("""
                SELECT fr FROM FitnessRecord fr
                WHERE year(fr.assessmentDate) = :year
                  AND quarter(fr.assessmentDate) = :quarter
                  AND fr.skillLevel = :skillLevel
                  AND fr.student.studentCode = :studentCode
                ORDER BY fr.id
            """)
    List<FitnessRecord> findRecordsForSingleStudent(int year, int quarter, SkillLevel skillLevel, String studentCode);
}
