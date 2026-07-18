package com.dat.backend_v2_1.repository.Security;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class UserRoleRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public long countByUserIdAndRoleCode(UUID userId, String roleCode) {
        Number count = (Number) entityManager
                .createNativeQuery("""
                        SELECT COUNT(*)
                        FROM security.user_role
                        WHERE user_id = :userId
                          AND role_code = :roleCode
                        """)
                .setParameter("userId", userId)
                .setParameter("roleCode", roleCode)
                .getSingleResult();
        return count.longValue();
    }
}
