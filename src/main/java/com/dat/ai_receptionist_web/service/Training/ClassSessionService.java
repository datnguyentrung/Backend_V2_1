package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Training.ClassSession;
import com.dat.ai_receptionist_web.dto.Training.ClassSessionDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.mapper.Training.ClassSessionMapper;
import com.dat.ai_receptionist_web.repository.Training.ClassSessionRepository;
import com.dat.ai_receptionist_web.repository.Catalog.CourseRepository;
import com.dat.ai_receptionist_web.enums.Operation.SessionStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClassSessionService {
    private final ClassSessionRepository repository;
    private final ClassSessionMapper mapper;
    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public PageResponse<ClassSessionDTO.Response> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ClassSessionDTO.Response get(UUID id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    public ClassSessionDTO.Response create(ClassSessionDTO.CreateRequest request) {
        ClassSession entity = new ClassSession();
        entity.setCourse(courseRepository.findById(request.courseId()).orElseThrow(() -> new IllegalArgumentException("Course not found")));
        entity.setSessionDate(request.sessionDate());
        entity.setStatus(request.status());
        entity.setAttendanceClosed(request.attendanceClosed());
        entity.setStartTime(request.startTime());
        entity.setEndTime(request.endTime());
        entity.setNote(request.note());
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public ClassSessionDTO.Response update(UUID id, ClassSessionDTO.UpdateRequest request) {
        var entity = find(id);
        entity.setCourse(courseRepository.findById(request.courseId()).orElseThrow(() -> new IllegalArgumentException("Course not found")));
        mapper.updateEntity(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        var entity = find(id);
        entity.setStatus(SessionStatus.CANCELLED);
    }

    private ClassSession find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("ClassSession not found"));
    }
}
