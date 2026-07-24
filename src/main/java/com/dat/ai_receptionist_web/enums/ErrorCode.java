package com.dat.ai_receptionist_web.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    STUDENT_ALREADY_ENROLLED(409, "Học viên đang theo học lớp này, không thể đăng ký thêm"),
    STUDENT_NOT_FOUND(404, "Không tìm thấy thông tin học viên"),
    STUDENT_INACTIVE(400, "Học viên không ở trạng thái hoạt động"),
    STUDENT_ACTIVE_ENROLLMENT_NOT_FOUND(409, "Học viên không có đăng ký lớp đang hoạt động"),
    COACH_NOT_FOUND(404, "Không tìm thấy thông tin huấn luyện viên"),
    CLASS_NOT_FOUND(404, "Lớp học không tồn tại"),
    CLASS_ALREADY_EXISTS(409, "Mã lịch học đã tồn tại"),
    CLASS_HAS_STUDENTS(400, "Không thể xóa lớp học vì còn học viên đang theo học"),
    CLASS_HAS_COACHES(400, "Không thể xóa lớp học vì còn huấn luyện viên được phân công"),
    UNCATEGORIZED_EXCEPTION(500, "Lỗi hệ thống không xác định"),
    ENROLLMENT_NOT_FOUND(404, "Không tìm thấy thông tin đăng ký học viên"),
    COACH_ASSIGNMENT_NOT_FOUND(404, "Không tìm thấy thông tin phân công huấn luyện viên"),
    COACH_ALREADY_ASSIGNED(409, "Huấn luyện viên đã được phân công cho lớp học này"),
    PAYMENT_NOT_FOUND(404, "Không tìm thấy thông tin thanh toán"),
    TUITION_ALREADY_PAID(409, "Học phí tháng này đã được đóng cho lớp học tương ứng"),

    COACH_INACTIVE(400, "Huấn luyện viên không hoạt động"),
    CLASS_INACTIVE(400, "Lớp học không hoạt động"),
    COACH_ASSIGNMENT_INVALID(400, "Không có phân công huấn luyện viên hợp lệ"),
    COACH_ASSIGNMENT_NOT_STARTED(400, "Phân công huấn luyện viên chưa bắt đầu"),
    COACH_ASSIGNMENT_ENDED(400, "Phân công huấn luyện viên đã kết thúc"),
    COACH_ASSIGNMENT_OVERLAPPED(409, "Phân công huấn luyện viên bị trùng hoặc chồng chéo lịch"),
    COACH_TIMESHEET_NOT_FOUND(404, "Không tìm thấy bảng công huấn luyện viên"),
    COACH_TIMESHEET_ALREADY_EXISTS(409, "Huấn luyện viên đã chấm công cho ca dạy này"),
    CLASS_SESSION_NOT_FOUND(404, "Không tìm thấy buổi học"),
    MULTIPLE_ACTIVE_CLASS_SESSIONS(409, "Học viên có nhiều buổi học đang hoạt động, không thể xác định buổi cần điểm danh"),
    ATTENDANCE_ALREADY_EXISTS(409, "Học viên đã được điểm danh cho buổi học này"),
    WRONG_CLASS_DAY(400, "Ngày chấm công không khớp với lịch học"),
    WRONG_CLASS_SHIFT(400, "Ca chấm công không khớp với lịch học"),
    CHECK_IN_TOO_EARLY(400, "Chưa đến thời gian được phép chấm công"),
    CHECK_IN_TOO_LATE(400, "Đã quá thời gian được phép chấm công"),
    ACCESS_DENIED(403, "Không có quyền truy cập dữ liệu này"),
    INVALID_DATE_RANGE(400, "Khoảng ngày không hợp lệ"),
    NOTIFICATION_RECIPIENT_NOT_FOUND(404, "Notification not found");

    private final int statusCode;
    private final String message;
}
