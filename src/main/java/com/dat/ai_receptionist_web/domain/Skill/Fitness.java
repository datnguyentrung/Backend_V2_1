package com.dat.ai_receptionist_web.domain.Skill;

import com.dat.ai_receptionist_web.enums.Core.ScheduleLevel;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "fitness", schema = "skill")
public class Fitness {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fitness_id")
    private Long fitnessId;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_level", nullable = false, length = 20)
    private ScheduleLevel scheduleLevel;

    @Column(name = "amount", nullable = false)
    private int amount;

    @Column(name = "duration", nullable = false)
    private int duration;
}
