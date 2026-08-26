package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.enums.Security.UserStatus;

import java.util.List;
import java.util.UUID;

public final class AuthenticatedUserPrincipal extends org.springframework.security.core.userdetails.User {
    private final UUID userId;
    private final UserStatus status;

    /**
     * Tác dụng: Thực hiện logic AuthenticatedUserPrincipal của lớp hiện tại.
     * Input: Nhận User user từ caller hoặc request.
     * Output: Khởi tạo instance của lớp với các phụ thuộc đầu vào.
     */
    public AuthenticatedUserPrincipal(User user) {
        super(user.getPhoneNumber(), user.getPasswordHash(), user.getStatus() == UserStatus.ACTIVE,
                true, true, user.getStatus() != UserStatus.LOCKED && user.getStatus() != UserStatus.BANNED,
                List.of());
        this.userId = user.getUserId();
        this.status = user.getStatus();
    }

    /**
     * Tác dụng: Thực hiện logic getUserId của lớp hiện tại.
     * Input: Không có tham số đầu vào.
     * Output: Trả về UUID theo kết quả xử lý.
     */
    public UUID getUserId() { return userId; }
    /**
     * Tác dụng: Thực hiện logic getStatus của lớp hiện tại.
     * Input: Không có tham số đầu vào.
     * Output: Trả về UserStatus theo kết quả xử lý.
     */
    public UserStatus getStatus() { return status; }
}


