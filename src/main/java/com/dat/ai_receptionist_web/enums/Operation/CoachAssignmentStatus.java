package com.dat.ai_receptionist_web.enums.Operation;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CoachAssignmentStatus {
    ACTIVE("Đang giảng dạy"),
    INACTIVE("Không hoạt động"),
    ENDED("Đã kết thúc"),
    CANCELLED("Đã hủy"),

    // Giữ các giá trị cũ để không phá dữ liệu/API hiện tại.
    SUSPENDED("Tạm ngưng"),
    COMPLETED("Hoàn thành nhiệm vụ"),
    TERMINATED("Chấm dứt"),
    PENDING("Chờ nhận lớp");

    private final String description;

    public boolean isActiveLike() {
        return this == ACTIVE;
    }

    public boolean blocksNewAssignment() {
        return this == ACTIVE || this == PENDING || this == SUSPENDED;
    }
}
