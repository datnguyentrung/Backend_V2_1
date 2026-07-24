package com.dat.ai_receptionist_web.enums.Operation;

public enum SessionStatus {
    SCHEDULED, // Lớp học đã được lên lịch nhưng chưa bắt đầu
    ACTIVE, // Lớp học đang diễn ra bình thường
    COMPLETED, // Lớp học đã kết thúc

    TERMINATED, // Lớp học bị chấm dứt sớm (do việc nghiêm trọng như vi phạm nội quy, v.v.)
    CANCELLED, // Lớp học đã bị hủy
    POSTPONED, // Lớp học bị hoãn lại
}
