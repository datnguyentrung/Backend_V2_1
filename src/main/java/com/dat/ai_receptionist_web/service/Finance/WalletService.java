package com.dat.ai_receptionist_web.service.Finance;

import com.dat.ai_receptionist_web.domain.Finance.Wallet;
import com.dat.ai_receptionist_web.dto.Finance.WalletDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.mapper.Finance.WalletMapper;
import com.dat.ai_receptionist_web.repository.Finance.WalletRepository;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.enums.Finance.WalletStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WalletService {
    private final WalletRepository repository;
    private final WalletMapper mapper;
    private final PersonRepository personRepository;

    @Transactional(readOnly = true)
    public PageResponse<WalletDTO.Response> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public WalletDTO.Response get(UUID id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    public WalletDTO.Response create(WalletDTO.CreateRequest request) {
        Wallet entity = new Wallet();
        entity.setPerson(personRepository.findById(request.personId()).orElseThrow(() -> new IllegalArgumentException("Person not found")));
        entity.setBalance(request.balance());
        entity.setStatus(request.status());
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public WalletDTO.Response update(UUID id, WalletDTO.UpdateRequest request) {
        var entity = find(id);
        entity.setPerson(personRepository.findById(request.personId()).orElseThrow(() -> new IllegalArgumentException("Person not found")));
        mapper.updateEntity(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        var entity = find(id);
        entity.setStatus(WalletStatus.CLOSED);
    }

    private Wallet find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
    }
}
