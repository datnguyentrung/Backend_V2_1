package com.dat.ai_receptionist_web.mapper.Finance;

import com.dat.ai_receptionist_web.domain.Finance.Wallet;
import com.dat.ai_receptionist_web.dto.Finance.WalletDTO;
import org.springframework.stereotype.Component;

@Component
public class WalletMapper {
    public WalletDTO.Response toResponse(Wallet entity) {
        if (entity == null) return null;
        return new WalletDTO.Response(entity.getWalletId(), entity.getPerson() == null ? null : entity.getPerson().getPersonId(), entity.getBalance(), entity.getStatus(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public void updateEntity(WalletDTO.UpdateRequest request, Wallet entity) {
        entity.setBalance(request.balance());
        entity.setStatus(request.status());
    }
}
