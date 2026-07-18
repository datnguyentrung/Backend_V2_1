package com.dat.backend_v2_1.repository.Security;

import com.dat.backend_v2_1.domain.Security.UserProfile;
import com.dat.backend_v2_1.enums.Security.RelationshipType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    @Query(value = """
            SELECT p.person_id AS "personId",
                   up.relationship_type AS "relationshipType",
                   p.full_name AS "displayName",
                   s.student_code AS "studentCode",
                   c.staff_code AS "staffCode"
            FROM security.user_profile up
            JOIN core.person p ON p.person_id = up.person_id
            LEFT JOIN core.student s ON s.person_id = p.person_id
            LEFT JOIN core.coach c ON c.person_id = p.person_id
            WHERE up.user_id = :userId
              AND up.is_active = true
            """, nativeQuery = true)
    List<UserContextRow> findActiveContextRowsByUserId(@Param("userId") UUID userId);

    @EntityGraph(attributePaths = "person")
    List<UserProfile> findAllByUser_UserIdAndActiveTrue(UUID userId);

    @EntityGraph(attributePaths = "person")
    Optional<UserProfile> findByUser_UserIdAndPerson_PersonIdAndRelationshipTypeAndActiveTrue(
            UUID userId,
            UUID personId,
            RelationshipType relationshipType
    );

    @EntityGraph(attributePaths = {"person", "user", "user.roles"})
    List<UserProfile> findAllByPerson_PersonIdInAndRelationshipTypeAndActiveTrue(
            Collection<UUID> personIds,
            RelationshipType relationshipType
    );

    interface UserContextRow {
        UUID getPersonId();

        String getRelationshipType();

        String getDisplayName();

        String getStudentCode();

        String getStaffCode();
    }
}
