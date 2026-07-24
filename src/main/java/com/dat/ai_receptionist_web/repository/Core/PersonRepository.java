package com.dat.ai_receptionist_web.repository.Core;

import com.dat.ai_receptionist_web.domain.Core.Person;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface PersonRepository extends JpaRepository<Person, UUID> {

    @Query(
            value = """
                    SELECT p.person_id AS personId,
                           p.full_name AS fullName,
                           p.birth_date AS birthDate,
                           CAST(p.belt AS varchar) AS belt,
                           'STUDENT' AS personType,
                           s.student_code AS code,
                           CAST(s.student_status AS varchar) AS status
                    FROM core.person p
                    JOIN core.student s ON s.person_id = p.person_id
                    WHERE (:search IS NULL
                           OR LOWER(p.full_name) LIKE :search
                           OR LOWER(s.student_code) LIKE :search)
                    UNION ALL
                    SELECT p.person_id AS personId,
                           p.full_name AS fullName,
                           p.birth_date AS birthDate,
                           CAST(p.belt AS varchar) AS belt,
                           'COACH' AS personType,
                           c.staff_code AS code,
                           CAST(c.coach_status AS varchar) AS status
                    FROM core.person p
                    JOIN core.coach c ON c.person_id = p.person_id
                    WHERE (:search IS NULL
                           OR LOWER(p.full_name) LIKE :search
                           OR LOWER(c.staff_code) LIKE :search)
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM (
                        SELECT p.person_id
                        FROM core.person p
                        JOIN core.student s ON s.person_id = p.person_id
                        WHERE (:search IS NULL
                               OR LOWER(p.full_name) LIKE :search
                               OR LOWER(s.student_code) LIKE :search)
                        UNION ALL
                        SELECT p.person_id
                        FROM core.person p
                        JOIN core.coach c ON c.person_id = p.person_id
                        WHERE (:search IS NULL
                               OR LOWER(p.full_name) LIKE :search
                               OR LOWER(c.staff_code) LIKE :search)
                    ) person_rows
                    """,
            nativeQuery = true
    )
    Page<PersonSearchProjection> searchStudentsAndCoaches(
            @Param("search") String search,
            Pageable pageable
    );

    interface PersonSearchProjection {
        UUID getPersonId();

        String getFullName();

        LocalDate getBirthDate();

        String getBelt();

        String getPersonType();

        String getCode();

        String getStatus();
    }
}
