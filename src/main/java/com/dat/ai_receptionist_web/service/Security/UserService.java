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
    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<UserDTO.Response> theo kết quả xử lý.
     */
    public PageResponse<UserDTO.Response> list(Pageable pageable) {
        return PageResponse.of(userRepository.findAll(pageable), userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về UserDTO.Response theo kết quả xử lý.
     */
    public UserDTO.Response get(UUID id) {
        return userMapper.toResponse(find(id));
    }

    @Transactional
    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận UserDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về UserDTO.Response theo kết quả xử lý.
     */
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
    /**
     * Tác dụng: Cập nhật bản ghi hiện có và trả về dữ liệu sau khi cập nhật.
     * Input: Nhận UUID id, UserDTO.UpdateRequest request từ caller hoặc request.
     * Output: Trả về UserDTO.Response theo kết quả xử lý.
     */
    public UserDTO.Response update(UUID id, UserDTO.UpdateRequest request) {
        User user = find(id);
        userMapper.updateEntity(request, user);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    /**
     * Tác dụng: Xóa hoặc vô hiệu hóa bản ghi theo định danh đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    public void delete(UUID id) {
        User user = find(id);
        user.setStatus(UserStatus.DISABLED);
    }

    @Transactional(readOnly = true)
    /**
     * Tác dụng: Thực hiện logic getUserById của lớp hiện tại.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về User theo kết quả xử lý.
     */
    public User getUserById(UUID id) {
        return find(id);
    }

    @Transactional(readOnly = true)
    /**
     * Tác dụng: Thực hiện logic getUserByPhoneNumber của lớp hiện tại.
     * Input: Nhận String phoneNumber từ caller hoặc request.
     * Output: Trả về User theo kết quả xử lý.
     */
    public User getUserByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(PhoneNumberUtil.normalize(phoneNumber))
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    @Transactional
    /**
     * Tác dụng: Thực hiện logic createLoginUser của lớp hiện tại.
     * Input: Nhận String phoneNumber, String rawPassword từ caller hoặc request.
     * Output: Trả về User theo kết quả xử lý.
     */
    public User createLoginUser(String phoneNumber, String rawPassword) {
        String normalized = PhoneNumberUtil.normalize(phoneNumber);
        if (userRepository.findByPhoneNumber(normalized).isPresent()) {
            throw new BusinessException("Phone number already exists");
        }
        return createLoginUserWithoutDuplicateCheck(normalized, rawPassword);
    }

    @Transactional
    /**
     * Tác dụng: Thực hiện logic createLoginUserWithoutDuplicateCheck của lớp hiện tại.
     * Input: Nhận String phoneNumber, String rawPassword từ caller hoặc request.
     * Output: Trả về User theo kết quả xử lý.
     */
    public User createLoginUserWithoutDuplicateCheck(String phoneNumber, String rawPassword) {
        String normalized = PhoneNumberUtil.normalize(phoneNumber);
        return userRepository.save(User.builder()
                .phoneNumber(normalized)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .status(UserStatus.ACTIVE)
                .authorizationVersion(0)
                .build());
    }

    @Transactional
    /**
     * Tác dụng: Thực hiện logic updateLastLogin của lớp hiện tại.
     * Input: Nhận UUID userId từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    public void updateLastLogin(UUID userId) {
        userRepository.updateLastLogin(userId);
    }

    @Transactional
    /**
     * Tác dụng: Thực hiện logic changePassword của lớp hiện tại.
     * Input: Nhận UUID userId, ChangePasswordReq request từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
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

    /**
     * Tác dụng: Tìm và trả về dữ liệu nội bộ theo điều kiện đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về User theo kết quả xử lý.
     */
    private User find(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
    }
}


