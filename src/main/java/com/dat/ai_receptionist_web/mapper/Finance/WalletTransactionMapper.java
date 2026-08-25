package com.dat.ai_receptionist_web.mapper.Finance;

import com.dat.ai_receptionist_web.domain.Finance.WalletTransaction;
import com.dat.ai_receptionist_web.dto.Finance.WalletTransactionDTO;
import org.springframework.stereotype.Component;

@Component
public class WalletTransactionMapper {
    public WalletTransactionDTO.Response toResponse(WalletTransaction entity) {
        if (entity == null) return null;
        return new WalletTransactionDTO.Response(entity.getWalletTransactionId(), entity.getWallet() == null ? null : entity.getWallet().getWalletId(), entity.getCreatedByUser() == null ? null : entity.getCreatedByUser().getUserId(), entity.getApprovedByUser() == null ? null : entity.getApprovedByUser().getUserId(), entity.getType(), entity.getDirection(), entity.getAmount(), entity.getBalanceBefore(), entity.getBalanceAfter(), entity.getExternalReference(), entity.getApprovedAt(), entity.getNote(), entity.getStatus(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public void updateEntity(WalletTransactionDTO.UpdateRequest request, WalletTransaction entity) {
        entity.setType(request.type());
        entity.setDirection(request.direction());
        entity.setAmount(request.amount());
        entity.setBalanceBefore(request.balanceBefore());
        entity.setBalanceAfter(request.balanceAfter());
        entity.setExternalReference(request.externalReference());
        entity.setApprovedAt(request.approvedAt());
        entity.setNote(request.note());
        entity.setStatus(request.status());
    }
}
