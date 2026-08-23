package com.dat.ai_receptionist_web.enums.Operation;

public enum CoachAssignmentStatus {
    PENDING,    // Chờ phân công
    ACTIVE,     // Đang được phân công
    SUSPENDED,  // Tạm đình chỉ
    ENDED,      // Đã kết thúc
    CANCELLED;  // Đã hủy

    public boolean isActiveLike() {
        return this == ACTIVE;
    }

    public boolean blocksNewAssignment() {
        return this == ACTIVE || this == PENDING || this == SUSPENDED;
    }
}
