package com.dat.ai_receptionist_web.service.Finance;

import com.dat.ai_receptionist_web.domain.Finance.WalletTransaction;
import com.dat.ai_receptionist_web.dto.Finance.WalletTransactionDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.mapper.Finance.WalletTransactionMapper;
import com.dat.ai_receptionist_web.repository.Finance.WalletTransactionRepository;
import com.dat.ai_receptionist_web.repository.Finance.WalletRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.dat.ai_receptionist_web.enums.Finance.WalletTransactionStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WalletTransactionCrudService {
    private final WalletTransactionRepository repository;
    private final WalletTransactionMapper mapper;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PageResponse<WalletTransactionDTO.Response> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public WalletTransactionDTO.Response get(UUID id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    public WalletTransactionDTO.Response create(WalletTransactionDTO.CreateRequest request) {
        WalletTransaction entity = new WalletTransaction();
        entity.setWallet(walletRepository.findById(request.walletId()).orElseThrow(() -> new IllegalArgumentException("Wallet not found")));
        entity.setCreatedByUser(userRepository.findById(request.createdByUserId()).orElseThrow(() -> new IllegalArgumentException("User not found")));
        entity.setApprovedByUser(userRepository.findById(request.approvedByUserId()).orElseThrow(() -> new IllegalArgumentException("User not found")));
        entity.setType(request.type());
        entity.setDirection(request.direction());
        entity.setAmount(request.amount());
        entity.setBalanceBefore(request.balanceBefore());
        entity.setBalanceAfter(request.balanceAfter());
        entity.setExternalReference(request.externalReference());
        entity.setApprovedAt(request.approvedAt());
        entity.setNote(request.note());
        entity.setStatus(request.status());
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public WalletTransactionDTO.Response update(UUID id, WalletTransactionDTO.UpdateRequest request) {
        var entity = find(id);
        entity.setWallet(walletRepository.findById(request.walletId()).orElseThrow(() -> new IllegalArgumentException("Wallet not found")));
        entity.setCreatedByUser(userRepository.findById(request.createdByUserId()).orElseThrow(() -> new IllegalArgumentException("User not found")));
        entity.setApprovedByUser(userRepository.findById(request.approvedByUserId()).orElseThrow(() -> new IllegalArgumentException("User not found")));
        mapper.updateEntity(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        var entity = find(id);
        entity.setStatus(WalletTransactionStatus.REJECTED);
    }

    private WalletTransaction find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("WalletTransaction not found"));
    }
}
