package com.dat.backend_v2_1.controller.Security;

import com.dat.backend_v2_1.config.SecurityRule;
import com.dat.backend_v2_1.domain.Core.Coach;
import com.dat.backend_v2_1.domain.Core.Student;
import com.dat.backend_v2_1.domain.Security.User;
import com.dat.backend_v2_1.dto.Operation.CoachAssignmentResDTO;
import com.dat.backend_v2_1.dto.Security.ChangePasswordReq;
import com.dat.backend_v2_1.dto.Security.UserRes;
import com.dat.backend_v2_1.enums.Operation.CoachAssignmentStatus;
import com.dat.backend_v2_1.mapper.Core.CoachMapper;
import com.dat.backend_v2_1.mapper.Core.StudentMapper;
import com.dat.backend_v2_1.mapper.Security.UserMapper;
import com.dat.backend_v2_1.service.Core.StudentService;
import com.dat.backend_v2_1.service.Operation.CoachAssignmentService;
import com.dat.backend_v2_1.service.Security.UserService;
import com.dat.backend_v2_1.util.error.IdInvalidException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService usersService;
    private final UserMapper userMapper;
    private final CoachMapper coachMapper;
    private final StudentMapper studentMapper;
    private final CoachAssignmentService coachAssignmentService;
    private final SecurityRule securityRule;
    private final StudentService studentService;

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/me/change-password")
    public ResponseEntity<String> changePassword(
            @RequestBody ChangePasswordReq request,
            Authentication authentication) {
        String idUser = authentication.getName();
        usersService.changePassword(idUser, request);

        return ResponseEntity.ok("Đổi mật khẩu thành công");
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<List<UserRes>> getCurrentUser(Authentication authentication) throws IdInvalidException {
        String idUser = authentication.getName();

        // Lấy user từ database
        User user = usersService.getUserById(idUser);

        // Map User sang UserRes dựa trên type thực tế (Coach có staffCode, Student có studentCode)
        List<UserRes> userResList = new ArrayList<>();
        if (securityRule.isCoach(authentication)) {
            UserRes userRes = coachMapper.toUserRes((Coach) user);

            // Xử lý phân quyền: Set assignedClasses cho COACH
            if (securityRule.isCoach(authentication) && !securityRule.isManagerSenior(authentication)) {
                // Chỉ là COACH (không phải MANAGER hay HEAD_COACH)
                List<CoachAssignmentResDTO.Response> coachAssignments =
                        coachAssignmentService.findDetailedCoachAssignmentsByUserId(UUID.fromString(idUser), CoachAssignmentStatus.ACTIVE);

                if (userRes.getUserInfo() != null) {
                    userRes.getUserInfo().setAssignedClasses(coachAssignments);
                }
            }
            userResList.add(userRes);
        } else if (securityRule.isStudent(authentication)) {
            userResList.add(studentMapper.toUserRes((Student) user));
        } else if (securityRule.isParent(authentication)) {
            List<Student> children = studentService.getStudentByParentId(UUID.fromString(idUser));

            // Map danh sách Student thành danh sách UserRes
            List<UserRes> mappedChildren = children.stream()
                    .map(child -> {
                        // Bước 3.1: Map thông tin của Child sang UserRes như bình thường
                        UserRes childRes = studentMapper.toUserRes(child);

                        // Bước 3.2: GÁN THÔNG TIN PARENT VÀO CHILD
                        // Ví dụ: Gán số điện thoại của Parent làm phoneNumber của con trong UserInfo
                        if (childRes.getUserInfo() != null) {
                            childRes.getUserProfile().setPhone(user.getPhoneNumber());
                        }

                        return childRes;
                    })
                    .toList();

            userResList.addAll(mappedChildren); // Chú ý: Dùng addAll thay vì add
        }

        return ResponseEntity.ok(userResList);
    }
}