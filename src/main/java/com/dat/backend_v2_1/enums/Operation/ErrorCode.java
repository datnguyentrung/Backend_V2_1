package com.dat.backend_v2_1.enums.Operation;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    STUDENT_ALREADY_ENROLLED(409, "Học viên đang theo học lớp này, không thể đăng ký thêm"),
    STUDENT_NOT_FOUND(404, "Không tìm thấy thông tin học viên"),
    CLASS_NOT_FOUND(404, "Lớp học không tồn tại"),
    UNCATEGORIZED_EXCEPTION(500, "Lỗi hệ thống không xác định"),
    ENROLLMENT_NOT_FOUND(404, "Không tìm thấy thông tin đăng ký học viên"),
    COACH_ASSIGNMENT_NOT_FOUND(404, "Không tìm thấy thông tin phân công huấn luyện viên"),
    COACH_ALREADY_ASSIGNED(409, "Huấn luyện viên đã được phân công cho lớp học này");

    private final int statusCode;
    private final String message;
}