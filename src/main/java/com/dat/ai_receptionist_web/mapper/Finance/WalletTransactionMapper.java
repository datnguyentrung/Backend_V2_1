package com.dat.ai_receptionist_web.mapper.Finance;

import com.dat.ai_receptionist_web.domain.Finance.WalletTransaction;
import com.dat.ai_receptionist_web.dto.Finance.WalletTransactionDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface WalletTransactionMapper {
    @Mapping(target = "walletId", source = "wallet.walletId")
    @Mapping(target = "createdByUserId", source = "createdByUser.userId")
    @Mapping(target = "approvedByUserId", source = "approvedByUser.userId")
    WalletTransactionDTO.Response toResponse(WalletTransaction entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "wallet", ignore = true)
    @Mapping(target = "createdByUser", ignore = true)
    @Mapping(target = "approvedByUser", ignore = true)
    @Mapping(target = "type", source = "type")
    @Mapping(target = "direction", source = "direction")
    @Mapping(target = "amount", source = "amount")
    @Mapping(target = "balanceBefore", source = "balanceBefore")
    @Mapping(target = "balanceAfter", source = "balanceAfter")
    @Mapping(target = "externalReference", source = "externalReference")
    @Mapping(target = "approvedAt", source = "approvedAt")
    @Mapping(target = "note", source = "note")
    @Mapping(target = "status", source = "status")
    void updateEntity(WalletTransactionDTO.UpdateRequest request, @MappingTarget WalletTransaction entity);
}
