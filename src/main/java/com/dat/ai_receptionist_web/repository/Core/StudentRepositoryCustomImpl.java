package com.dat.ai_receptionist_web.repository.Core;

import com.dat.ai_receptionist_web.domain.Core.Student;
import com.dat.ai_receptionist_web.enums.Core.StudentStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class StudentRepositoryCustomImpl implements StudentRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

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
                public StudentStatus getStatus() {
                    return tuple.get("status", StudentStatus.class);
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
