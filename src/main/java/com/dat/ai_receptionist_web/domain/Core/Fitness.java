package com.dat.ai_receptionist_web.domain.Core;

import com.dat.ai_receptionist_web.dto.Core.FitnessId;
import com.dat.ai_receptionist_web.enums.Skill.SkillLevel;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "fitness",
        schema = "core"
)
@IdClass(FitnessId.class) // BẮT BUỘC: Khai báo class chứa cấu trúc khóa kép
public class Fitness {

    @Id // Đánh dấu đây là một phần của khóa chính
    @Column(name = "fitness_level", nullable = false)
    private Integer fitnessLevel;

    @Id // Đánh dấu đây là phần còn lại của khóa chính
    @Column(name = "skill_level", nullable = false)
    @Enumerated(EnumType.STRING) // Khuyên dùng: Lưu Enum dưới dạng Text trong DB thay vì số 0, 1, 2
    private SkillLevel skillLevel;

    @Column(name = "duration", nullable = false)
    private Integer duration;

    @Column(name = "amount", nullable = false)
    private Integer amount;
}