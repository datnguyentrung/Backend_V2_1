package com.dat.ai_receptionist_web.enums.Operation;

public enum SessionStatus {
    SCHEDULED,  // Đã lên lịch
    ACTIVE,     // Đang diễn ra
    COMPLETED,  // Đã hoàn thành
    TERMINATED, // Bị chấm dứt
    CANCELLED,  // Đã hủy
    POSTPONED   // Bị hoãn
}
