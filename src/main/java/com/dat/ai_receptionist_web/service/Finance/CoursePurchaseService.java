package com.dat.ai_receptionist_web.service.Finance;

import com.dat.ai_receptionist_web.domain.Finance.CoursePurchase;
import com.dat.ai_receptionist_web.dto.Finance.CoursePurchaseDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.mapper.Finance.CoursePurchaseMapper;
import com.dat.ai_receptionist_web.repository.Finance.CoursePurchaseRepository;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.repository.Catalog.CoursePriceRepository;
import com.dat.ai_receptionist_web.repository.Finance.WalletTransactionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CoursePurchaseService {
    private final CoursePurchaseRepository repository;
    private final CoursePurchaseMapper mapper;
    private final PersonRepository personRepository;
    private final CoursePriceRepository coursePriceRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    @Transactional(readOnly = true)
    public PageResponse<CoursePurchaseDTO.Response> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CoursePurchaseDTO.Response get(UUID id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    public CoursePurchaseDTO.Response create(CoursePurchaseDTO.CreateRequest request) {
        CoursePurchase entity = new CoursePurchase();
        entity.setStudentPerson(personRepository.findById(request.studentPersonId()).orElseThrow(() -> new IllegalArgumentException("Person not found")));
        entity.setCoursePrice(coursePriceRepository.findById(request.coursePriceId()).orElseThrow(() -> new IllegalArgumentException("CoursePrice not found")));
        entity.setDebitTransaction(walletTransactionRepository.findById(request.debitTransactionId()).orElseThrow(() -> new IllegalArgumentException("WalletTransaction not found")));
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public CoursePurchaseDTO.Response update(UUID id, CoursePurchaseDTO.UpdateRequest request) {
        var entity = find(id);
        entity.setStudentPerson(personRepository.findById(request.studentPersonId()).orElseThrow(() -> new IllegalArgumentException("Person not found")));
        entity.setCoursePrice(coursePriceRepository.findById(request.coursePriceId()).orElseThrow(() -> new IllegalArgumentException("CoursePrice not found")));
        entity.setDebitTransaction(walletTransactionRepository.findById(request.debitTransactionId()).orElseThrow(() -> new IllegalArgumentException("WalletTransaction not found")));
        mapper.updateEntity(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        var entity = find(id);
        repository.delete(entity);
    }

    private CoursePurchase find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("CoursePurchase not found"));
    }
}
