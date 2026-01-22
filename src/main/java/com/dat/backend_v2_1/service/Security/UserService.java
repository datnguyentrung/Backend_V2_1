package com.dat.backend_v2_1.service.Security;

import com.dat.backend_v2_1.domain.Security.User;
import com.dat.backend_v2_1.domain.Security.Role;
import com.dat.backend_v2_1.dto.Security.ChangePasswordReq;
import com.dat.backend_v2_1.enums.Security.UserStatus;
import com.dat.backend_v2_1.repository.Security.UserRepository;
import com.dat.backend_v2_1.util.error.InvalidPasswordException;
import com.dat.backend_v2_1.util.error.UserNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RoleService roleService;

    @Value("${password.time_password_change_days}")
    private long timePasswordChange;

    // Hàm dùng chung để gán thông tin User
    public <T extends User> void setupBaseUser(T user, String roleCode) {
        Role role = roleService.getRoleByCode(roleCode);

        user.setRole(role);
        user.setPasswordHash(passwordEncoder.encode("123456"));
        user.setStatus(UserStatus.ACTIVE);
    }

    // Lấy user theo idUser
    public User getUserById(String userId) {
        return userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new UserNotFoundException("User not found with idUser: " + userId));
    }

    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with idUser: " + userId));
    }
    public User getUserByPhoneNumber(String phoneNumber) throws UserNotFoundException {
        return (User) userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new UserNotFoundException("User not found with phone number: " + phoneNumber));
    }

    public void changePassword(String phoneNumber, ChangePasswordReq passwordReq) {
        User user = getUserByPhoneNumber(phoneNumber);

        if (!passwordEncoder.matches(passwordReq.getOldPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException("Mật khẩu cũ không đúng");
        }

        if (!passwordReq.getNewPassword().equals(passwordReq.getConfirmPassword())) {
            throw new InvalidPasswordException("Xác nhận mật khẩu không khớp");
        }

        user.setPasswordHash(passwordEncoder.encode(passwordReq.getNewPassword()));
        userRepository.save(user);
    }
}
