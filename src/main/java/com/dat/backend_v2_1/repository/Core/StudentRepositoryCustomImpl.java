package com.dat.backend_v2_1.repository.Core;

import com.dat.backend_v2_1.domain.Core.Student;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class StudentRepositoryCustomImpl implements StudentRepositoryCustom {

    @PersistenceContext
    private final EntityManager entityManager;

    @Override
    public List<StudentStatusCount> countStudentsByStatus(Specification<Student> spec) {
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(Tuple.class);
        Root<Student> root = query.from(Student.class);

        if (spec != null) {
            Predicate predicate = spec.toPredicate(root, query, cb);
            if (predicate != null) {
                query.where(predicate);
            }
        }

        query.multiselect(
                root.get("studentStatus").alias("status"),
                cb.count(root).alias("count")
        );
        query.groupBy(root.get("studentStatus"));

        List<Tuple> results = entityManager.createQuery(query).getResultList();

        List<StudentStatusCount> counts = new ArrayList<>();
        for (Tuple tuple : results) {
            counts.add(new StudentStatusCount() {
                @Override
                public com.dat.backend_v2_1.enums.Core.StudentStatus getStatus() {
                    return tuple.get("status", com.dat.backend_v2_1.enums.Core.StudentStatus.class);
                }

                @Override
                public Long getCount() {
                    return tuple.get("count", Long.class);
                }
            });
        }

        return counts;
    }
}
