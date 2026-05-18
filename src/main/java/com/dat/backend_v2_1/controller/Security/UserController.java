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
import com.dat.backend_v2_1.service.Core.CoachService;
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
    private final CoachService coachService;
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
        UUID userUuid = UUID.fromString(idUser);
        List<UserRes> userResList = new ArrayList<>();

        if (securityRule.isCoach(authentication)) {
            // CÁCH FIX: Query trực tiếp từ CoachService thay vì UserService
            Coach coach = coachService.getCoachById(userUuid);
            UserRes userRes = coachMapper.toUserRes(coach);

            if (!securityRule.isManagerSenior(authentication)) {
                List<CoachAssignmentResDTO.Response> coachAssignments =
                        coachAssignmentService.findDetailedCoachAssignmentsByUserId(userUuid, CoachAssignmentStatus.ACTIVE);
                if (userRes.getUserInfo() != null) {
                    userRes.getUserInfo().setAssignedClasses(coachAssignments);
                }
            }
            userResList.add(userRes);

        } else if (securityRule.isStudent(authentication)) {
            // Tương tự, lấy Student trực tiếp
            Student student = studentService.getStudentById(userUuid);
            userResList.add(studentMapper.toUserRes(student));

        } else if (securityRule.isParent(authentication)) {
            // (Logic lấy danh sách con của bạn giữ nguyên, vì nó đang gọi đúng qua studentService)
            User parentUser = usersService.getUserById(idUser); // Lấy user cha để lấy SĐT
            List<Student> children = studentService.getStudentByParentId(userUuid);

            List<UserRes> mappedChildren = children.stream()
                    .map(child -> {
                        UserRes childRes = studentMapper.toUserRes(child);
                        if (childRes.getUserInfo() != null) {
                            childRes.getUserProfile().setPhone(parentUser.getPhoneNumber());
                        }
                        return childRes;
                    })
                    .toList();

            userResList.addAll(mappedChildren);
        }

        return ResponseEntity.ok(userResList);
    }
}