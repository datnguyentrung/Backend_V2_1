package com.dat.backend_v2_1.repository.Security;

import com.dat.backend_v2_1.domain.Security.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
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

    List<User> findDistinctByRoles_Code(String code);

    @Query("""
            SELECT DISTINCT u.userId
            FROM User u
            JOIN u.roles role
            WHERE role.code = :roleCode
            """)
    List<UUID> findUserIdsByRoleCode(@Param("roleCode") String roleCode);
}
