package com.dat.ai_receptionist_web.enums.Report;

public enum ExamEligibility {
    EXEMPTED,     // Miễn thi
    ELIGIBLE,     // Đủ điều kiện
    PENDING,      // Chưa đạt (đang tích lũy)
    NOT_ELIGIBLE, // Không đủ điều kiện (vi phạm hoặc hết thời gian)
    NONE          // Không có dữ liệu (Chưa tham gia)
}