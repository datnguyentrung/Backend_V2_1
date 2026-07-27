package com.dat.ai_receptionist_web.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    STUDENT_ALREADY_ENROLLED(409, "Học viên đang theo học lớp này, không thể đăng ký thêm"),
    STUDENT_NOT_FOUND(404, "Không tìm thấy thông tin học viên"),
    PERSON_NOT_FOUND(404, "Không tìm thấy thông tin người dùng"),
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
    MULTIPLE_ACTIVE_CLASS_SESSIONS(409, "Có nhiều buổi học ACTIVE cùng giờ, không thể tự xác định buổi điểm danh"),
    ATTENDANCE_CHECK_IN_NOT_ALLOWED(409, "Bản ghi điểm danh hiện tại không cho phép check-in tự động"),
    ATTENDANCE_ALREADY_EXISTS(409, "Học viên đã được điểm danh cho buổi học này"),
    WRONG_CLASS_DAY(400, "Ngày chấm công không khớp với lịch học"),
    WRONG_CLASS_SHIFT(400, "Ca chấm công không khớp với lịch học"),
    CHECK_IN_TOO_EARLY(400, "Chưa đến thời gian được phép chấm công"),
    CHECK_IN_TOO_LATE(400, "Đã quá thời gian được phép chấm công"),
    ACCESS_DENIED(403, "Không có quyền truy cập dữ liệu này"),
    INVALID_DATE_RANGE(400, "Khoảng ngày không hợp lệ"),
    NOTIFICATION_RECIPIENT_NOT_FOUND(404, "Notification not found"),
    FACE_IMAGE_INVALID(
            400,
            "Ảnh khuôn mặt không hợp lệ"
    ),
    INVALID_IMAGE_FILE(400, "File upload không hợp lệ"),
    EMPTY_IMAGE_FILE(400, "File không chứa dữ liệu"),
    FILE_TOO_LARGE(413, "File vượt quá dung lượng cho phép"),
    UNSUPPORTED_IMAGE_TYPE(400, "MIME type không được hỗ trợ"),
    IMAGE_DECODE_FAILED(400, "Không thể decode dữ liệu thành ảnh"),
    FACE_NOT_DETECTED(
            422,
            "Không phát hiện được khuôn mặt trong ảnh"
    ),
    FACE_NOT_MATCHED(
            422,
            "Khuôn mặt không khớp với dữ liệu đã đăng ký"
    ),
    MULTIPLE_FACES_DETECTED(
            422,
            "Ảnh chỉ được chứa một khuôn mặt"
    ),
    FACE_EMBEDDING_FAILED(422, "Không tạo được face embedding"),
    INVALID_EMBEDDING(422, "Face embedding không hợp lệ"),
    MODEL_NOT_INITIALIZED(503, "InsightFace chưa được khởi tạo"),
    INTERNAL_ERROR(500, "Lỗi hệ thống không xác định"),
    FACE_CHECK_IN_PERSON_TYPE_INVALID(
            409,
            "Người được nhận diện không thuộc loại có thể điểm danh"
    ),
    PYTHON_BACKEND_UNAVAILABLE(
            503,
            "Dịch vụ nhận diện khuôn mặt hiện không khả dụng"
    ),
    PYTHON_BACKEND_ERROR(
            502,
            "Dịch vụ nhận diện khuôn mặt trả về dữ liệu không hợp lệ"
    );

    private final int statusCode;
    private final String message;
}
