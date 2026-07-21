package com.dat.backend_v2_1.enums.Operation;

public enum CoachTimesheetStatus {
    PENDING,
    CHECKED_IN,
    APPROVED,
    REJECTED,
    ADJUSTED,
    CANCELLED;

    public static String getCoachTimesheetStatusText(
            CoachTimesheetStatus status
    ) {
        if (status == null) {
            return "chưa xác định trạng thái chấm công";
        }

        return switch (status) {
            case CHECKED_IN -> "đã chấm công";
            case PENDING -> "đang chờ duyệt";
            case ADJUSTED -> "bảng công đã được điều chỉnh";
            case APPROVED -> "bảng công đã được duyệt";
            case REJECTED -> "bảng công bị từ chối";
            case CANCELLED -> "bảng công đã bị hủy";
        };
    }
}
