package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Security.Role;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.dto.Security.ChangePasswordReq;
import com.dat.ai_receptionist_web.dto.Security.UserReq;
import com.dat.ai_receptionist_web.dto.Security.UserRes;
import com.dat.ai_receptionist_web.enums.Security.UserStatus;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.dat.ai_receptionist_web.util.PhoneNumberUtil;
import com.dat.ai_receptionist_web.util.error.BusinessException;
import com.dat.ai_receptionist_web.util.error.InvalidPasswordException;
import com.dat.ai_receptionist_web.util.error.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final String DEFAULT_PASSWORD_HASH = "$2a$10$pDCb306dF99wUKluGLnm4ek0aVPkrxkk5V9D1a0fnuw7O5uGE0lHy";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;
    private final UserRoleService userRoleService;

    @Value("${password.time_password_change_days}")
    private long timePasswordChange;

    @Transactional(rollbackFor = Exception.class)
    public UserRes.UserDetail createUserWithDefaultPassword(UserReq request) {
        String normalizedPhone = PhoneNumberUtil.normalize(request.getPhoneNumber());
        if (userRepository.findByPhoneNumber(normalizedPhone).isPresent()) {
            throw new BusinessException("Phone number already exists");
        }

        User user = new User();
        user.setPhoneNumber(normalizedPhone);
        user.setPasswordHash(DEFAULT_PASSWORD_HASH);
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);
        userRoleService.assignRoles(user, request.getRoleCodes());

        return toUserDetail(getUserWithRolesById(user.getUserId()));
    }

    public User createLoginUser(String phoneNumber, String rawPassword, Set<String> roleCodes) {
        String normalizedPhone = PhoneNumberUtil.normalize(phoneNumber);
        if (userRepository.findByPhoneNumber(normalizedPhone).isPresent()) {
            throw new IllegalArgumentException("Phone number already exists");
        }
        User user = new User();
        user.setPhoneNumber(normalizedPhone);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setStatus(UserStatus.ACTIVE);
        roleCodes.forEach(code -> user.getRoles().add(roleService.getRoleReferenceByCode(normalizeRoleCode(code))));
        return userRepository.save(user);
    }

    public List<User> getAllUsersByRoleCode(String roleCode) {
        return userRepository.findDistinctByRoles_Code(roleCode);
    }

    public User getUserById(String userId) {
        return getUserById(UUID.fromString(userId));
    }

    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with idUser: " + userId));
    }

    public User getUserWithRolesById(UUID userId) {
        return userRepository.findWithRolesByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with idUser: " + userId));
    }

    public User getUserByPhoneNumber(String phoneNumber) throws UserNotFoundException {
        String normalizedPhone = PhoneNumberUtil.normalize(phoneNumber);
        return userRepository.findByPhoneNumber(normalizedPhone)
                .orElseThrow(() -> new UserNotFoundException("User not found with phone number"));
    }

    public User getUserWithRolesByPhoneNumber(String phoneNumber) throws UserNotFoundException {
        String normalizedPhone = PhoneNumberUtil.normalize(phoneNumber);
        return userRepository.findWithRolesByPhoneNumber(normalizedPhone)
                .orElseThrow(() -> new UserNotFoundException("User not found with phone number"));
    }

    @Transactional
    @Async
    public void updateLastLogin(UUID userId) {
        User user = getUserById(userId);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }

    public void changePassword(String phoneNumber, ChangePasswordReq passwordReq) {
        User user = getUserByPhoneNumber(phoneNumber);
        changePassword(user, passwordReq);
    }

    public void changePasswordByUserId(UUID userId, ChangePasswordReq passwordReq) {
        User user = getUserById(userId);
        changePassword(user, passwordReq);
    }

    private void changePassword(User user, ChangePasswordReq passwordReq) {

        if (!passwordEncoder.matches(passwordReq.getOldPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException("Mat khau cu khong dung");
        }

        if (!passwordReq.getNewPassword().equals(passwordReq.getConfirmPassword())) {
            throw new InvalidPasswordException("Xac nhan mat khau khong khop");
        }

        user.setPasswordHash(passwordEncoder.encode(passwordReq.getNewPassword()));
        userRepository.save(user);
    }

    public void addRole(User user, String roleCode) {
        Role role = roleService.getRoleReferenceByCode(normalizeRoleCode(roleCode));
        user.getRoles().add(role);
    }

    private String normalizeRoleCode(String roleCode) {
        return roleCode == null || roleCode.startsWith("ROLE_") ? roleCode : "ROLE_" + roleCode;
    }

    private UserRes.UserDetail toUserDetail(User user) {
        return UserRes.UserDetail.builder()
                .userId(user.getUserId())
                .phoneNumber(user.getPhoneNumber())
                .status(user.getStatus())
                .roles(user.getRoles().stream().map(Role::getCode).sorted().toList())
                .build();
    }
}
