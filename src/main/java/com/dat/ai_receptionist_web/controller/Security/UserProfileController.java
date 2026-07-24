package com.dat.ai_receptionist_web.controller.Security;

import com.dat.ai_receptionist_web.dto.Security.UserProfileDTO;
import com.dat.ai_receptionist_web.service.Security.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user-profiles")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @PreAuthorize("@securityRule.isManagerSenior(authentication)")
    @PostMapping
    public ResponseEntity<UserProfileDTO.Response> createUserProfile(
            @RequestBody @Valid UserProfileDTO.CreateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userProfileService.createUserProfile(request));
    }
}
