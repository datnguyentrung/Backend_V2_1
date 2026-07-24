package com.dat.ai_receptionist_web.specification;

import com.dat.ai_receptionist_web.domain.Skill.FitnessRecord;
import com.dat.ai_receptionist_web.enums.Skill.SkillLevel;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class FitnessRecordSpecification {
    public static Specification<FitnessRecord> hasSearch(String search) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(search)) return null;
            // Ví dụ: Tìm theo ghi chú (note) hoặc ID lịch học
            String pattern = "%" + search.toLowerCase().trim() + "%";
            return cb.or(
                    cb.like(cb.lower(root.join("student").get("fullName")), pattern)
            );
        };
    }

    public static Specification<FitnessRecord> hasSkillLevel(SkillLevel skillLevel) {
        return (root, query, cb) ->
                skillLevel == null ? null : cb.equal(root.get("skillLevel"), skillLevel);
    }
}
