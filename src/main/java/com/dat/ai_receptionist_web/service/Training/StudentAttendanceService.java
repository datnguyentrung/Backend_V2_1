package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Training.StudentAttendance;
import com.dat.ai_receptionist_web.dto.Training.StudentAttendanceDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CoreErrorCode;
import com.dat.ai_receptionist_web.error.code.TrainingErrorCode;
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
public class StudentAttendanceService {
    private final StudentAttendanceRepository repository;
    private final StudentAttendanceMapper mapper;
    private final ClassSessionRepository classSessionRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final PersonRepository personRepository;

    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<StudentAttendanceDTO.Response> theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public PageResponse<StudentAttendanceDTO.Response> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toResponse);
    }

    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về StudentAttendanceDTO.Response theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public StudentAttendanceDTO.Response get(UUID id) {
        return mapper.toResponse(find(id));
    }

    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận StudentAttendanceDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về StudentAttendanceDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public StudentAttendanceDTO.Response create(StudentAttendanceDTO.CreateRequest request) {
        StudentAttendance entity = new StudentAttendance();
        entity.setClassSession(classSessionRepository.findById(request.classSessionId()).orElseThrow(() -> new ApiException(TrainingErrorCode.CLASS_SESSION_NOT_FOUND)));
        entity.setStudentEnrollment(studentEnrollmentRepository.findById(request.studentEnrollmentId()).orElseThrow(() -> new ApiException(TrainingErrorCode.STUDENT_ENROLLMENT_NOT_FOUND)));
        entity.setEvaluatedByCoach(personRepository.findById(request.evaluatedByCoachId()).orElseThrow(() -> new ApiException(CoreErrorCode.PERSON_NOT_FOUND)));
        entity.setCheckInTime(request.checkInTime());
        entity.setAttendanceStatus(request.attendanceStatus());
        entity.setEvaluationStatus(request.evaluationStatus());
        entity.setNote(request.note());
        return mapper.toResponse(repository.save(entity));
    }

    /**
     * Tác dụng: Cập nhật bản ghi hiện có và trả về dữ liệu sau khi cập nhật.
     * Input: Nhận UUID id, StudentAttendanceDTO.UpdateRequest request từ caller hoặc request.
     * Output: Trả về StudentAttendanceDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public StudentAttendanceDTO.Response update(UUID id, StudentAttendanceDTO.UpdateRequest request) {
        var entity = find(id);
        entity.setClassSession(classSessionRepository.findById(request.classSessionId()).orElseThrow(() -> new ApiException(TrainingErrorCode.CLASS_SESSION_NOT_FOUND)));
        entity.setStudentEnrollment(studentEnrollmentRepository.findById(request.studentEnrollmentId()).orElseThrow(() -> new ApiException(TrainingErrorCode.STUDENT_ENROLLMENT_NOT_FOUND)));
        entity.setEvaluatedByCoach(personRepository.findById(request.evaluatedByCoachId()).orElseThrow(() -> new ApiException(CoreErrorCode.PERSON_NOT_FOUND)));
        mapper.updateEntity(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    /**
     * Tác dụng: Xóa hoặc vô hiệu hóa bản ghi theo định danh đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @Transactional
    public void delete(UUID id) {
        var entity = find(id);
        repository.delete(entity);
    }

    /**
     * Tác dụng: Tìm và trả về dữ liệu nội bộ theo điều kiện đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về StudentAttendance theo kết quả xử lý.
     */
    private StudentAttendance find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(TrainingErrorCode.STUDENT_ATTENDANCE_NOT_FOUND));
    }
}


