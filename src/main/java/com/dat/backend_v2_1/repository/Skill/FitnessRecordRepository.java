package com.dat.backend_v2_1.repository.Skill;

import com.dat.backend_v2_1.domain.Skill.FitnessRecord;
import com.dat.backend_v2_1.enums.Skill.SkillLevel;
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
import java.util.Optional;

@Repository
public interface FitnessRecordRepository extends JpaRepository<FitnessRecord, Long>, JpaSpecificationExecutor<FitnessRecord> {
    @Override
    @NonNull
    Page<FitnessRecord> findAll(
            @NonNull Specification<FitnessRecord> spec, @NonNull Pageable pageable
    );

    @EntityGraph(attributePaths = {"student"}) // Ép lấy Student và User
    @Query("""
                SELECT fr FROM FitnessRecord fr
                WHERE year(fr.assessmentDate) = :year
                  AND quarter(fr.assessmentDate) = :quarter
                  AND fr.skillLevel = :skillLevel
                ORDER BY fr.student.studentCode, fr.duration DESC, fr.amount DESC
            """)
    List<FitnessRecord> findBestRecordsForQuarter(
            @Param("year") int year,
            @Param("quarter") int quarter,
            @Param("skillLevel") SkillLevel skillLevel
    );

    @Query("""
                SELECT fr FROM FitnessRecord fr
                WHERE year(fr.assessmentDate) = :year
                  AND quarter(fr.assessmentDate) = :quarter
                  AND fr.skillLevel = :skillLevel
                  AND fr.student.studentCode = :studentCode
                ORDER BY fr.duration DESC, fr.amount DESC
                LIMIT 1
            """)
    Optional<FitnessRecord> findBestRecordForSingleStudent(int year, int quarter, SkillLevel skillLevel, String studentCode);
}
