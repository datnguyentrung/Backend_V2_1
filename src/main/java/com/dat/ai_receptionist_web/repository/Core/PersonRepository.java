package com.dat.ai_receptionist_web.repository.Core;

import com.dat.ai_receptionist_web.domain.Core.Person;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PersonRepository extends JpaRepository<Person, UUID> {

    boolean existsByNationalCode(String nationalCode);

    @Query(
            value = """
                    SELECT s.person_id
                    FROM core.student s
                    WHERE UPPER(s.student_code) = UPPER(:personCode)
                    UNION ALL
                    SELECT c.person_id
                    FROM core.coach c
                    WHERE UPPER(c.staff_code) = UPPER(:personCode)
                    """,
            nativeQuery = true
    )
    List<UUID> findPersonIdsByPersonCode(@Param("personCode") String personCode);

    @Query(
            value = """
                    SELECT p.person_id AS personId,
                           p.full_name AS fullName,
                           CAST(p.belt AS varchar) AS belt,
                           p.email AS email,
                           s.student_code AS studentCode,
                           CAST(s.student_status AS varchar) AS studentStatus,
                           c.staff_code AS staffCode,
                           CAST(c.coach_status AS varchar) AS coachStatus
                    FROM core.person p
                    LEFT JOIN core.student s ON s.person_id = p.person_id
                    LEFT JOIN core.coach c ON c.person_id = p.person_id
                    WHERE p.person_id = :personId
                    """,
            nativeQuery = true
    )
    Optional<FaceCheckInSubjectProjection> findFaceCheckInSubjectByPersonId(
            @Param("personId") UUID personId
    );

    /**
     * pgvector cosine distance. The vector is supplied as a PostgreSQL vector literal,
     * for example {@code [0.12,-0.03,...]}.
     */
    @Transactional(readOnly = true)
    @Query(
            value = """
                    SELECT p.person_id AS personId,
                           (p.face_embedding <=> CAST(:embedding AS vector)) AS distance
                    FROM core.person p
                    WHERE p.face_embedding IS NOT NULL
                    ORDER BY p.face_embedding <=> CAST(:embedding AS vector)
                    LIMIT 1
                    """,
            nativeQuery = true
    )
    List<NearestFaceMatchProjection> findNearestFaceMatch(@Param("embedding") String embedding);

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

    @Query(
            value = """
                    SELECT p.person_id AS personId,
                           p.face_image_path AS faceImagePath
                    FROM core.person p
                    ORDER BY p.person_id
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM core.person p
                    """,
            nativeQuery = true
    )
    Page<PersonAvatarProjection> findPersonAvatarProjections(Pageable pageable);

    interface PersonSearchProjection {
        UUID getPersonId();

        String getFullName();

        LocalDate getBirthDate();

        String getBelt();

        String getPersonType();

        String getCode();

        String getStatus();
    }

    interface FaceCheckInSubjectProjection {
        UUID getPersonId();

        String getFullName();

        String getBelt();

        String getEmail();

        String getStudentCode();

        String getStudentStatus();

        String getStaffCode();

        String getCoachStatus();
    }

    interface NearestFaceMatchProjection {
        UUID getPersonId();

        Double getDistance();
    }

    interface PersonAvatarProjection {
        UUID getPersonId();

        String getFaceImagePath();
    }
}
