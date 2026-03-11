package com.dat.backend_v2_1.controller.Security;

import com.dat.backend_v2_1.dto.RestResponse;
import com.dat.backend_v2_1.dto.Security.ChangePasswordReq;
import com.dat.backend_v2_1.dto.Security.UserRes;
import com.dat.backend_v2_1.mapper.Core.CoachMapper;
import com.dat.backend_v2_1.mapper.Core.StudentMapper;
import com.dat.backend_v2_1.mapper.Security.UserMapper;
import com.dat.backend_v2_1.service.Core.CoachService;
import com.dat.backend_v2_1.service.Core.StudentService;
import com.dat.backend_v2_1.service.Security.UserService;
import com.dat.backend_v2_1.util.error.IdInvalidException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService usersService;
    private final CoachService coachService;
    private final StudentService studentService;
    private final StudentMapper studentMapper;
    private final CoachMapper coachMapper;
    private final UserMapper userMapper;

    @PostMapping("/me/change-password")
    public ResponseEntity<RestResponse<String>> changePassword(
            @RequestBody ChangePasswordReq request,
            Authentication authentication) {
        String idUser = authentication.getName();
        usersService.changePassword(idUser, request);

        RestResponse<String> res = new RestResponse<>();
        res.setStatusCode(HttpStatus.OK.value());
        res.setMessage("Đổi mật khẩu thành công");
        res.setData(null); // hoặc có thể set thêm thông tin gì đó nếu cần

        return ResponseEntity.ok(res);
    }

    @GetMapping("/me")
    public ResponseEntity<UserRes> getCurrentUser(Authentication authentication) throws IdInvalidException {
        String idUser = authentication.getName();

        UserRes userRes = userMapper.toUserRes(usersService.getUserById(idUser));
        return ResponseEntity.ok(userRes);
    }
}
