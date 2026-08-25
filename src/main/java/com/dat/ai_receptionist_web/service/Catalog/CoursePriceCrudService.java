package com.dat.ai_receptionist_web.service.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.CoursePrice;
import com.dat.ai_receptionist_web.dto.Catalog.CoursePriceDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.mapper.Catalog.CoursePriceMapper;
import com.dat.ai_receptionist_web.repository.Catalog.CoursePriceRepository;
import com.dat.ai_receptionist_web.repository.Catalog.CourseRepository;
import com.dat.ai_receptionist_web.enums.Catalog.CoursePriceStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CoursePriceCrudService {
    private final CoursePriceRepository repository;
    private final CoursePriceMapper mapper;
    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public PageResponse<CoursePriceDTO.Response> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CoursePriceDTO.Response get(UUID id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    public CoursePriceDTO.Response create(CoursePriceDTO.CreateRequest request) {
        CoursePrice entity = new CoursePrice();
        entity.setCourse(courseRepository.findById(request.courseId()).orElseThrow(() -> new IllegalArgumentException("Course not found")));
        entity.setDurationMonths(request.durationMonths());
        entity.setSessionCount(request.sessionCount());
        entity.setBasePrice(request.basePrice());
        entity.setFinalPrice(request.finalPrice());
        entity.setStatus(request.status());
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public CoursePriceDTO.Response update(UUID id, CoursePriceDTO.UpdateRequest request) {
        var entity = find(id);
        entity.setCourse(courseRepository.findById(request.courseId()).orElseThrow(() -> new IllegalArgumentException("Course not found")));
        mapper.updateEntity(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        var entity = find(id);
        entity.setStatus(CoursePriceStatus.INACTIVE);
    }

    private CoursePrice find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("CoursePrice not found"));
    }
}
