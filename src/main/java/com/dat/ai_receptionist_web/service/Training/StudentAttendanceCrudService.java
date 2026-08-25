package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Training.StudentAttendance;
import com.dat.ai_receptionist_web.dto.Training.StudentAttendanceDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.mapper.Training.StudentAttendanceMapper;
import com.dat.ai_receptionist_web.repository.Training.StudentAttendanceRepository;
import com.dat.ai_receptionist_web.repository.Training.ClassSessionRepository;
import com.dat.ai_receptionist_web.repository.Training.StudentEnrollmentRepository;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentAttendanceCrudService {
    private final StudentAttendanceRepository repository;
    private final StudentAttendanceMapper mapper;
    private final ClassSessionRepository classSessionRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final PersonRepository personRepository;

    @Transactional(readOnly = true)
    public PageResponse<StudentAttendanceDTO.Response> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public StudentAttendanceDTO.Response get(UUID id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    public StudentAttendanceDTO.Response create(StudentAttendanceDTO.CreateRequest request) {
        StudentAttendance entity = new StudentAttendance();
        entity.setClassSession(classSessionRepository.findById(request.classSessionId()).orElseThrow(() -> new IllegalArgumentException("ClassSession not found")));
        entity.setStudentEnrollment(studentEnrollmentRepository.findById(request.studentEnrollmentId()).orElseThrow(() -> new IllegalArgumentException("StudentEnrollment not found")));
        entity.setEvaluatedByCoach(personRepository.findById(request.evaluatedByCoachId()).orElseThrow(() -> new IllegalArgumentException("Person not found")));
        entity.setCheckInTime(request.checkInTime());
        entity.setAttendanceStatus(request.attendanceStatus());
        entity.setEvaluationStatus(request.evaluationStatus());
        entity.setNote(request.note());
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public StudentAttendanceDTO.Response update(UUID id, StudentAttendanceDTO.UpdateRequest request) {
        var entity = find(id);
        entity.setClassSession(classSessionRepository.findById(request.classSessionId()).orElseThrow(() -> new IllegalArgumentException("ClassSession not found")));
        entity.setStudentEnrollment(studentEnrollmentRepository.findById(request.studentEnrollmentId()).orElseThrow(() -> new IllegalArgumentException("StudentEnrollment not found")));
        entity.setEvaluatedByCoach(personRepository.findById(request.evaluatedByCoachId()).orElseThrow(() -> new IllegalArgumentException("Person not found")));
        mapper.updateEntity(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        var entity = find(id);
        repository.delete(entity);
    }

    private StudentAttendance find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("StudentAttendance not found"));
    }
}
