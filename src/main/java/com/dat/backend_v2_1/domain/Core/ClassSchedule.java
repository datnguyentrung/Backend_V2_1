package com.dat.backend_v2_1.domain.Core;

import com.dat.backend_v2_1.enums.Core.*;
import com.dat.backend_v2_1.util.converter.WeekdayConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "class_schedule", schema = "core") // Tên bảng snake_case
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClassSchedule {
    @Id
    @Column(name = "schedule_id", length = 5)
    String scheduleId;

    // Logic: Lịch học thường gắn với một Chi nhánh cụ thể
    @NotNull(message = "Lịch học phải thuộc về một chi nhánh")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    @ToString.Exclude
    Branch branch;

    @NotNull(message = "Thứ trong tuần không được để trống")
    @Convert(converter = WeekdayConverter.class) // Dùng Converter để lưu số (2,3...) xuống DB
    @Column(name = "weekday", nullable = false)
    Weekday weekday;

    @NotNull(message = "Trình độ không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(name = "level", length = 20, nullable = false)
    ScheduleLevel level;

    // SỬA ĐỔI QUAN TRỌNG: Dùng LocalTime thay vì Instant
    @NotNull(message = "Giờ bắt đầu không được để trống")
    @Column(name = "start_time", nullable = false, columnDefinition = "TIME")
    LocalTime startTime;

    @NotNull(message = "Giờ kết thúc không được để trống")
    @Column(name = "end_time", nullable = false, columnDefinition = "TIME")
    LocalTime endTime;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "shift", length = 20)
    ScheduleShift shift; // Ví dụ: MORNING, AFTERNOON, EVENING

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "location", length = 50)
    ScheduleLocation location; // Ví dụ: ROOM_A, ROOM_B, YOGA_STUDIO_1

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_status", length = 20)
    ScheduleStatus scheduleStatus;

    // --- Validation Logic (Optional) ---
    // Hibernate sẽ gọi hàm này trước khi Insert/Update
    @PrePersist
    @PreUpdate
    private void validateTime() {
        if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            throw new IllegalStateException("Giờ kết thúc phải sau giờ bắt đầu");
        }
    }
}
