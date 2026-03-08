package com.dat.backend_v2_1.repository.Operation;

import com.dat.backend_v2_1.domain.Operation.StudentAttendance;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Custom Repository Implementation để apply Named EntityGraph với Specification.
 * <p>
 * Giải pháp cho vấn đề N+1 query khi sử dụng Specification với Pageable:
 * 1. Sử dụng Named EntityGraph được định nghĩa trong Entity
 * 2. Apply EntityGraph thông qua Query Hint
 * 3. Tránh conflict giữa JOIN FETCH và Pageable
 */
@Slf4j
@Repository
public class StudentAttendanceRepositoryCustomImpl implements StudentAttendanceRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<StudentAttendance> findAllWithEntityGraph(Specification<StudentAttendance> spec, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // === COUNT QUERY (Không cần EntityGraph) ===
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<StudentAttendance> countRoot = countQuery.from(StudentAttendance.class);

        if (spec != null) {
            Predicate countPredicate = spec.toPredicate(countRoot, countQuery, cb);
            if (countPredicate != null) {
                countQuery.where(countPredicate);
            }
        }

        countQuery.select(cb.count(countRoot));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        // === DATA QUERY (Apply EntityGraph) ===
        CriteriaQuery<StudentAttendance> dataQuery = cb.createQuery(StudentAttendance.class);
        Root<StudentAttendance> dataRoot = dataQuery.from(StudentAttendance.class);

        if (spec != null) {
            Predicate dataPredicate = spec.toPredicate(dataRoot, dataQuery, cb);
            if (dataPredicate != null) {
                dataQuery.where(dataPredicate);
            }
        }

        dataQuery.select(dataRoot);

        // Apply Sorting từ Pageable
        if (pageable.getSort().isSorted()) {
            dataQuery.orderBy(
                    pageable.getSort().stream()
                            .map(order -> order.isAscending()
                                    ? cb.asc(dataRoot.get(order.getProperty()))
                                    : cb.desc(dataRoot.get(order.getProperty())))
                            .toList()
            );
        }

        // Create TypedQuery và apply Named EntityGraph
        TypedQuery<StudentAttendance> typedQuery = entityManager.createQuery(dataQuery);

        // 🔑 QUAN TRỌNG: Apply Named EntityGraph để eager fetch
        EntityGraph<?> entityGraph = entityManager.getEntityGraph("StudentAttendance.withDetails");
        typedQuery.setHint("jakarta.persistence.fetchgraph", entityGraph);

        // Apply Pagination
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<StudentAttendance> content = typedQuery.getResultList();

        log.debug("Executed query with EntityGraph, found {} results out of {} total",
                content.size(), total);

        return new PageImpl<>(content, pageable, total);
    }
}

