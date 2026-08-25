package com.dat.ai_receptionist_web.service.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.Course;
import com.dat.ai_receptionist_web.dto.Catalog.CourseDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.mapper.Catalog.CourseMapper;
import com.dat.ai_receptionist_web.repository.Catalog.CourseRepository;
import com.dat.ai_receptionist_web.repository.Catalog.ClassScheduleRepository;
import com.dat.ai_receptionist_web.enums.Catalog.CourseStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseCrudService {
    private final CourseRepository repository;
    private final CourseMapper mapper;
    private final ClassScheduleRepository classScheduleRepository;

    @Transactional(readOnly = true)
    public PageResponse<CourseDTO.Response> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CourseDTO.Response get(UUID id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    public CourseDTO.Response create(CourseDTO.CreateRequest request) {
        Course entity = new Course();
        entity.setClassSchedule(classScheduleRepository.findById(request.classScheduleId()).orElseThrow(() -> new IllegalArgumentException("ClassSchedule not found")));
        entity.setCapacity(request.capacity());
        entity.setStatus(request.status());
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public CourseDTO.Response update(UUID id, CourseDTO.UpdateRequest request) {
        var entity = find(id);
        entity.setClassSchedule(classScheduleRepository.findById(request.classScheduleId()).orElseThrow(() -> new IllegalArgumentException("ClassSchedule not found")));
        mapper.updateEntity(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        var entity = find(id);
        entity.setStatus(CourseStatus.CANCELLED);
    }

    private Course find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Course not found"));
    }
}
