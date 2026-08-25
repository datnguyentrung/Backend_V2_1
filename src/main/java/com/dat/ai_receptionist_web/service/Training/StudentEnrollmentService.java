package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Training.StudentEnrollment;
import com.dat.ai_receptionist_web.dto.Training.StudentEnrollmentDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.mapper.Training.StudentEnrollmentMapper;
import com.dat.ai_receptionist_web.repository.Training.StudentEnrollmentRepository;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.repository.Finance.CoursePurchaseRepository;
import com.dat.ai_receptionist_web.repository.Catalog.ClassScheduleRepository;
import com.dat.ai_receptionist_web.enums.Operation.StudentEnrollmentStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentEnrollmentService {
    private final StudentEnrollmentRepository repository;
    private final StudentEnrollmentMapper mapper;
    private final PersonRepository personRepository;
    private final CoursePurchaseRepository coursePurchaseRepository;
    private final ClassScheduleRepository classScheduleRepository;

    @Transactional(readOnly = true)
    public PageResponse<StudentEnrollmentDTO.Response> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public StudentEnrollmentDTO.Response get(UUID id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    public StudentEnrollmentDTO.Response create(StudentEnrollmentDTO.CreateRequest request) {
        StudentEnrollment entity = new StudentEnrollment();
        entity.setStudentPerson(personRepository.findById(request.studentPersonId()).orElseThrow(() -> new IllegalArgumentException("Person not found")));
        entity.setCoursePurchase(coursePurchaseRepository.findById(request.coursePurchaseId()).orElseThrow(() -> new IllegalArgumentException("CoursePurchase not found")));
        entity.setClassSchedule(classScheduleRepository.findById(request.classScheduleId()).orElseThrow(() -> new IllegalArgumentException("ClassSchedule not found")));
        entity.setStartDate(request.startDate());
        entity.setEndDate(request.endDate());
        entity.setStatus(request.status());
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public StudentEnrollmentDTO.Response update(UUID id, StudentEnrollmentDTO.UpdateRequest request) {
        var entity = find(id);
        entity.setStudentPerson(personRepository.findById(request.studentPersonId()).orElseThrow(() -> new IllegalArgumentException("Person not found")));
        entity.setCoursePurchase(coursePurchaseRepository.findById(request.coursePurchaseId()).orElseThrow(() -> new IllegalArgumentException("CoursePurchase not found")));
        entity.setClassSchedule(classScheduleRepository.findById(request.classScheduleId()).orElseThrow(() -> new IllegalArgumentException("ClassSchedule not found")));
        mapper.updateEntity(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        var entity = find(id);
        entity.setStatus(StudentEnrollmentStatus.CANCELLED);
    }

    private StudentEnrollment find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("StudentEnrollment not found"));
    }
}
