package com.dat.backend_v2_1.controller.Security;

import com.dat.backend_v2_1.config.SecurityRule;
import com.dat.backend_v2_1.dto.Operation.CoachAssignmentResDTO;
import com.dat.backend_v2_1.dto.RestResponse;
import com.dat.backend_v2_1.dto.Security.ChangePasswordReq;
import com.dat.backend_v2_1.dto.Security.UserRes;
import com.dat.backend_v2_1.mapper.Security.UserMapper;
import com.dat.backend_v2_1.service.Operation.CoachAssignmentService;
import com.dat.backend_v2_1.service.Security.UserService;
import com.dat.backend_v2_1.util.error.IdInvalidException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService usersService;
    private final UserMapper userMapper;
    private final CoachAssignmentService coachAssignmentService;

    // 1. Inject SecurityRule vào Controller
    private final SecurityRule securityRule;

    @PostMapping("/me/change-password")
    public ResponseEntity<RestResponse<String>> changePassword(
            @RequestBody ChangePasswordReq request,
            Authentication authentication) {
        String idUser = authentication.getName();
        usersService.changePassword(idUser, request);

        RestResponse<String> res = new RestResponse<>();
        res.setStatusCode(HttpStatus.OK.value());
        res.setMessage("Đổi mật khẩu thành công");
        res.setData(null);

        return ResponseEntity.ok(res);
    }

    @GetMapping("/me")
    public ResponseEntity<UserRes> getCurrentUser(Authentication authentication) throws IdInvalidException {
        String idUser = authentication.getName();

        // Lấy thông tin user cơ bản
        UserRes userRes = userMapper.toUserRes(usersService.getUserById(idUser));

        // 2. Sử dụng SecurityRule để rẽ nhánh logic (Check từ cao xuống thấp)
        if (securityRule.isHeadCoach(authentication)) {
            // Xử lý riêng nếu là HEAD_COACH / ADMIN
            // VD: Lấy full quyền hạn, toàn bộ dữ liệu hệ thống

        } else if (securityRule.isManager(authentication)) {
            // Xử lý riêng nếu là MANAGER
            // VD: Lấy danh sách các cơ sở do Manager này quản lý

        } else if (securityRule.isCoach(authentication)) {
            // Xử lý riêng nếu là COACH
            List<CoachAssignmentResDTO.SimpleResponse> coachAssignments =
                    coachAssignmentService.findStudentEnrollmentsByCoachId(UUID.fromString(idUser));

            // Map danh sách lớp học sang List<String> (giả sử lấy tên lớp hoặc ID lớp)
            List<String> classNames = coachAssignments.stream()
                    .map(assignment -> assignment.getClassSchedule().getScheduleId()) // Thay bằng getter thực tế của bạn
                    .collect(Collectors.toList());

            // 4. Set dữ liệu vào UserInfo
            if (userRes.getUserInfo() != null) {
                userRes.getUserInfo().setAssignedClasses(classNames);
            }
        } else {
            // Xử lý cho các Role còn lại (như STUDENT)
            // Không làm gì thêm, chỉ trả về thông tin UserRes cơ bản đã map ở trên
        }

        return ResponseEntity.ok(userRes);
    }
}