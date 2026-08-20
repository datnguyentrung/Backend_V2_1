package com.dat.ai_receptionist_web.repository.Security;

import com.dat.ai_receptionist_web.domain.Security.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByPhoneNumber(String phoneNumber);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findWithRolesByPhoneNumber(String phoneNumber);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findWithRolesByUserId(UUID userId);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE User u
            SET u.lastLoginAt = CURRENT_TIMESTAMP,
                u.updatedAt = CURRENT_TIMESTAMP
            WHERE u.userId = :userId
            """)
    int updateLastLogin(@Param("userId") UUID userId);

    List<User> findDistinctByRoles_Code(String code);

    @Query("""
            SELECT DISTINCT u.userId
            FROM User u
            JOIN u.roles role
            WHERE role.code = :roleCode
            """)
    List<UUID> findUserIdsByRoleCode(@Param("roleCode") String roleCode);
}
