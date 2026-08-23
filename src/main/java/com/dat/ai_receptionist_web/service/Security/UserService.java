package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.dto.Security.ChangePasswordReq;
import com.dat.ai_receptionist_web.enums.Security.UserStatus;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.dat.ai_receptionist_web.util.PhoneNumberUtil;
import com.dat.ai_receptionist_web.util.error.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
    }

    @Transactional(readOnly = true)
    public User getUserByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(PhoneNumberUtil.normalize(phoneNumber))
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    @Transactional
    public User createLoginUser(String phoneNumber, String rawPassword) {
        String normalized = PhoneNumberUtil.normalize(phoneNumber);
        if (userRepository.findByPhoneNumber(normalized).isPresent()) {
            throw new BusinessException("Phone number already exists");
        }
        return userRepository.save(User.builder()
                .phoneNumber(normalized)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .status(UserStatus.ACTIVE)
                .authorizationVersion(0)
                .build());
    }

    @Transactional
    public void updateLastLogin(UUID userId) {
        userRepository.updateLastLogin(userId);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordReq request) {
        User user = getUserById(userId);
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException("Old password is incorrect");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new InvalidPasswordException("Password confirmation does not match");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
    }
}
