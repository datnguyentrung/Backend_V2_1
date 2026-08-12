package com.dat.ai_receptionist_web.repository.Report;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class LeaderboardLockRepository {
    private final EntityManager entityManager;

    public void lock(String scope) {
        entityManager.createNativeQuery("select pg_advisory_xact_lock(hashtextextended(:scope, 0))")
                .setParameter("scope", scope)
                .getSingleResult();
    }
}
