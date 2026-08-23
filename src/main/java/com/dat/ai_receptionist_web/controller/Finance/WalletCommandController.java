package com.dat.ai_receptionist_web.controller.Finance;

import com.dat.ai_receptionist_web.dto.Finance.WalletCommandDTO;
import com.dat.ai_receptionist_web.service.Finance.WalletCommandService;
import com.dat.ai_receptionist_web.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletCommandController {
    private final WalletCommandService commandService;

    @PostMapping("/top-up")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).WALLET_TOP_UP_CREATE.getCode())")
    public WalletCommandDTO.TransactionResponse topUp(
            @Valid @RequestBody WalletCommandDTO.TopUpRequest request) {
        return commandService.topUp(request, currentUserId());
    }

    @PostMapping("/course-purchases")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).COURSE_PURCHASE_CREATE.getCode())")
    public WalletCommandDTO.TransactionResponse purchase(
            @Valid @RequestBody WalletCommandDTO.CoursePurchaseRequest request) {
        return commandService.purchaseCourse(request, currentUserId());
    }

    @PostMapping("/refunds")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).WALLET_REFUND_CREATE.getCode())")
    public WalletCommandDTO.TransactionResponse refund(
            @Valid @RequestBody WalletCommandDTO.RefundRequest request) {
        return commandService.refund(request, currentUserId());
    }

    private UUID currentUserId() {
        return SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("Missing authenticated user"));
    }
}
