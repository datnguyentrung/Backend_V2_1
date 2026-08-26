package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.util.PhoneNumberUtil;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component("userDetailsService")
public class UserDetailCustom implements UserDetailsService {
    private final UserService userService;

    /**
     * Tác dụng: Thực hiện logic UserDetailCustom của lớp hiện tại.
     * Input: Nhận UserService userService từ caller hoặc request.
     * Output: Khởi tạo instance của lớp với các phụ thuộc đầu vào.
     */
    public UserDetailCustom(UserService userService) {
        this.userService = userService;
    }

    @Override
    /**
     * Tác dụng: Nạp dữ liệu cần thiết từ nguồn lưu trữ để phục vụ xử lý nghiệp vụ.
     * Input: Nhận String phoneNumber từ caller hoặc request.
     * Output: Trả về UserDetails theo kết quả xử lý.
     */
    public UserDetails loadUserByUsername(@NonNull String phoneNumber) throws UsernameNotFoundException {
        String normalizedPhone;
        try {
            normalizedPhone = PhoneNumberUtil.normalize(phoneNumber);
        } catch (IllegalArgumentException e) {
            throw new UsernameNotFoundException("Invalid phone number");
        }

        User user = userService.getUserByPhoneNumber(normalizedPhone);
        return new AuthenticatedUserPrincipal(user);
    }
}


