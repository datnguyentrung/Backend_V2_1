package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Security.ChangePasswordReq;
import com.dat.ai_receptionist_web.dto.Security.UserDTO;
import com.dat.ai_receptionist_web.enums.Security.UserStatus;
import com.dat.ai_receptionist_web.mapper.Security.UserMapper;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.dat.ai_receptionist_web.util.PhoneNumberUtil;
import com.dat.ai_receptionist_web.util.error.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public PageResponse<UserDTO.Response> list(Pageable pageable) {
        return PageResponse.of(userRepository.findAll(pageable), userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserDTO.Response get(UUID id) {
        return userMapper.toResponse(find(id));
    }

    @Transactional
    public UserDTO.Response create(UserDTO.CreateRequest request) {
        User user = new User();
        user.setPhoneNumber(request.phoneNumber());
        user.setPasswordHash(request.passwordHash());
        user.setStatus(request.status());
        user.setAuthorizationVersion(request.authorizationVersion());
        user.setLastLoginAt(request.lastLoginAt());
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserDTO.Response update(UUID id, UserDTO.UpdateRequest request) {
        User user = find(id);
        userMapper.updateEntity(request, user);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public void delete(UUID id) {
        User user = find(id);
        user.setStatus(UserStatus.DISABLED);
    }

    @Transactional(readOnly = true)
    public User getUserById(UUID id) {
        return find(id);
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

    private User find(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
    }
}
