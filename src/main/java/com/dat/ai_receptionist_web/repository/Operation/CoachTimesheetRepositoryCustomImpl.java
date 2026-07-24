package com.dat.ai_receptionist_web.repository.Operation;

import com.dat.ai_receptionist_web.domain.Operation.CoachTimesheet;
import com.dat.ai_receptionist_web.dto.Operation.CoachTimesheetDTO;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class CoachTimesheetRepositoryCustomImpl implements CoachTimesheetRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<CoachTimesheet> findAllWithEntityGraph(Specification<CoachTimesheet> spec, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<CoachTimesheet> countRoot = countQuery.from(CoachTimesheet.class);
        Predicate countPredicate = spec == null ? null : spec.toPredicate(countRoot, countQuery, cb);
        if (countPredicate != null) {
            countQuery.where(countPredicate);
        }
        countQuery.select(cb.count(countRoot));
        long total = entityManager.createQuery(countQuery).getSingleResult();
        if (total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        CriteriaQuery<CoachTimesheet> dataQuery = cb.createQuery(CoachTimesheet.class);
        Root<CoachTimesheet> dataRoot = dataQuery.from(CoachTimesheet.class);
        Predicate dataPredicate = spec == null ? null : spec.toPredicate(dataRoot, dataQuery, cb);
        if (dataPredicate != null) {
            dataQuery.where(dataPredicate);
        }
        dataQuery.select(dataRoot);

        if (pageable.getSort().isSorted()) {
            List<Order> orders = new ArrayList<>();
            for (Sort.Order sortOrder : pageable.getSort()) {
                Path<Object> path = getPath(dataRoot, sortOrder.getProperty());
                orders.add(sortOrder.isAscending() ? cb.asc(path) : cb.desc(path));
            }
            dataQuery.orderBy(orders);
        }

        TypedQuery<CoachTimesheet> typedQuery = entityManager.createQuery(dataQuery);
        typedQuery.setHint("jakarta.persistence.fetchgraph", entityManager.getEntityGraph("CoachTimesheet.withDetails"));
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        return new PageImpl<>(typedQuery.getResultList(), pageable, total);
    }

    @Override
    public CoachTimesheetDTO.SummaryResponse getSummary(Specification<CoachTimesheet> spec) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<CoachTimesheet> root = query.from(CoachTimesheet.class);
        Predicate predicate = spec == null ? null : spec.toPredicate(root, query, cb);
        if (predicate != null) {
            query.where(predicate);
        }

        query.select(cb.tuple(
                cb.count(root)
        ));

        Tuple result = entityManager.createQuery(query).getSingleResult();
        long total = getLong(result, 0);

        return CoachTimesheetDTO.SummaryResponse.builder()
                .totalRecords(total)
                .totalTeachingSessions(total)
                .build();
    }

    private Long getLong(Tuple result, int index) {
        Object val = result.get(index);
        if (val == null) return 0L;
        if (val instanceof Long longValue) return longValue;
        return ((Number) val).longValue();
    }

    private Path<Object> getPath(Root<CoachTimesheet> root, String property) {
        String[] parts = property.split("\\.");
        Path<Object> path = root.get(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            path = path.get(parts[i]);
        }
        return path;
    }
}
