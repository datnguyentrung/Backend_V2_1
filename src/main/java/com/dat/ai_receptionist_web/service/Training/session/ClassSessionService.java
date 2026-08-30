package com.dat.ai_receptionist_web.service.Training.session;

import com.dat.ai_receptionist_web.domain.Catalog.Course;
import com.dat.ai_receptionist_web.domain.Training.ClassSession;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Training.ClassSessionDTO;
import com.dat.ai_receptionist_web.enums.Catalog.CourseStatus;
import com.dat.ai_receptionist_web.enums.Training.SessionStatus;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CatalogErrorCode;
import com.dat.ai_receptionist_web.error.code.TrainingErrorCode;
import com.dat.ai_receptionist_web.mapper.Training.ClassSessionMapper;
import com.dat.ai_receptionist_web.repository.Catalog.CourseRepository;
import com.dat.ai_receptionist_web.repository.Training.ClassSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * CRUD + filter ClassSession. courseId bất biến; xóa là soft-cancel;
 * session đã bắt đầu/hoàn thành/quá khứ không được sửa.
 */
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
        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new ApiException(CatalogErrorCode.COURSE_NOT_FOUND));
        if (course.getStatus() != CourseStatus.ACTIVE) {
            throw new ApiException(TrainingErrorCode.COURSE_NOT_ACTIVE);
        }
        validateTime(request.startTime(), request.endTime());
        if (request.sessionDate().isBefore(LocalDate.now())) {
            throw new ApiException(TrainingErrorCode.CLASS_SESSION_IMMUTABLE,
                    "Cannot create a class session in the past");
        }
        if (repository.existsByCourse_CourseIdAndSessionDateAndStatusNot(
                request.courseId(), request.sessionDate(), SessionStatus.CANCELLED)) {
            throw new ApiException(TrainingErrorCode.CLASS_SESSION_ALREADY_EXISTS);
        }
        ClassSession entity = ClassSession.builder()
                .course(course)
                .sessionDate(request.sessionDate())
                .status(request.status())
                .attendanceClosed(request.attendanceClosed())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .note(request.note())
                .build();
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public ClassSessionDTO.Response update(UUID id, ClassSessionDTO.UpdateRequest request) {
        ClassSession entity = find(id);
        requireMutable(entity);
        if (!entity.getCourse().getCourseId().equals(request.courseId())) {
            throw new ApiException(TrainingErrorCode.CLASS_SESSION_IMMUTABLE,
                    "Course of a class session cannot be changed");
        }
        validateTime(request.startTime(), request.endTime());
        if (!request.sessionDate().equals(entity.getSessionDate())
                && repository.existsByCourse_CourseIdAndSessionDateAndStatusNot(
                        request.courseId(), request.sessionDate(), SessionStatus.CANCELLED)) {
            throw new ApiException(TrainingErrorCode.CLASS_SESSION_ALREADY_EXISTS);
        }
        mapper.updateEntity(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        ClassSession entity = find(id);
        requireMutable(entity);
        entity.setStatus(SessionStatus.CANCELLED);
    }

    private ClassSession find(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiException(TrainingErrorCode.CLASS_SESSION_NOT_FOUND));
    }

    private void requireMutable(ClassSession entity) {
        if (entity.getStatus() != SessionStatus.SCHEDULED
                && entity.getStatus() != SessionStatus.POSTPONED) {
            throw new ApiException(TrainingErrorCode.CLASS_SESSION_IMMUTABLE);
        }
        LocalDate today = LocalDate.now();
        if (entity.getSessionDate().isBefore(today)) {
            throw new ApiException(TrainingErrorCode.CLASS_SESSION_IMMUTABLE);
        }
        if (entity.getSessionDate().equals(today)
                && (entity.getEndTime() == null || !entity.getEndTime().isAfter(LocalTime.now()))) {
            throw new ApiException(TrainingErrorCode.CLASS_SESSION_IMMUTABLE);
        }
    }

    private void validateTime(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new ApiException(TrainingErrorCode.CLASS_SESSION_TIME_INVALID);
        }
    }
}
