package com.dat.backend_v2_1.controller.Security;

import com.dat.backend_v2_1.config.SecurityRule;
import com.dat.backend_v2_1.domain.Core.Coach;
import com.dat.backend_v2_1.domain.Core.Student;
import com.dat.backend_v2_1.dto.Operation.CoachAssignmentResDTO;
import com.dat.backend_v2_1.dto.Security.ChangePasswordReq;
import com.dat.backend_v2_1.dto.Security.UserReq;
import com.dat.backend_v2_1.dto.Security.UserRes;
import com.dat.backend_v2_1.enums.Operation.CoachAssignmentStatus;
import com.dat.backend_v2_1.mapper.Core.CoachMapper;
import com.dat.backend_v2_1.mapper.Core.StudentMapper;
import com.dat.backend_v2_1.mapper.Security.UserMapper;
import com.dat.backend_v2_1.service.Core.CoachService;
import com.dat.backend_v2_1.service.Core.StudentService;
import com.dat.backend_v2_1.service.Operation.CoachAssignmentService;
import com.dat.backend_v2_1.service.Security.UserService;
import com.dat.backend_v2_1.util.SecurityUtil;
import com.dat.backend_v2_1.util.error.IdInvalidException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @PreAuthorize("@securityRule.isManagerSenior(authentication)")
    @PostMapping
    public ResponseEntity<UserRes.UserDetail> createUser(
            @RequestBody @Valid UserReq request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(usersService.createUserWithDefaultPassword(request));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/me/change-password")
    public ResponseEntity<String> changePassword(@RequestBody ChangePasswordReq request) {
        UUID userId = SecurityUtil.getCurrentUserId()
                .map(UUID::fromString)
                .orElseThrow();
        usersService.changePasswordByUserId(userId, request);
        return ResponseEntity.ok("Password changed successfully");
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<List<UserRes>> getCurrentUser(Authentication authentication) throws IdInvalidException {
        UUID userUuid = SecurityUtil.getCurrentUserId().map(UUID::fromString).orElseThrow();
        UUID activePersonId = SecurityUtil.getCurrentActivePersonId().map(UUID::fromString).orElse(null);
        List<UserRes> userResList = new ArrayList<>();

        if (securityRule.isCoach(authentication)) {
            Coach coach = coachService.getCoachById(activePersonId == null ? userUuid : activePersonId);
            UserRes userRes = coachMapper.toUserRes(coach);

            if (!securityRule.isManagerSenior(authentication)) {
                List<CoachAssignmentResDTO.Response> coachAssignments =
                        coachAssignmentService.findDetailedCoachAssignmentsByUserId(activePersonId == null ? userUuid : activePersonId, CoachAssignmentStatus.ACTIVE);
                if (userRes.getUserInfo() != null) {
                    userRes.getUserInfo().setAssignedClasses(coachAssignments);
                }
            }
            userResList.add(userRes);
        } else if (securityRule.isStudent(authentication)) {
            Student student = studentService.getStudentById(activePersonId == null ? userUuid : activePersonId);
            userResList.add(studentMapper.toUserRes(student));
        } else if (securityRule.isParent(authentication)) {
            List<Student> children = studentService.getStudentByParentId(userUuid);
            userResList.addAll(children.stream().map(studentMapper::toUserRes).toList());
        } else {
            userResList.add(userMapper.toUserRes(usersService.getUserWithRolesById(userUuid)));
        }

        return ResponseEntity.ok(userResList);
    }
}
