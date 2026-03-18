package com.dat.backend_v2_1.enums.Operation;

public enum SessionStatus {
    ACTIVE, // Lớp học đang diễn ra bình thường
    CANCELLED, // Lớp học đã bị hủy
    COMPLETED, // Lớp học đã kết thúc
    SCHEDULED, // Lớp học đã được lên lịch nhưng chưa bắt đầu
    POSTPONED, // Lớp học bị hoãn lại
    TERMINATED // Lớp học bị chấm dứt sớm (do việc nghiêm trọng như vi phạm nội quy, v.v.)
}
