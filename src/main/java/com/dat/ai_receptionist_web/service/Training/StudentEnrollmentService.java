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
    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<StudentEnrollmentDTO.Response> theo kết quả xử lý.
     */
    public PageResponse<StudentEnrollmentDTO.Response> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về StudentEnrollmentDTO.Response theo kết quả xử lý.
     */
    public StudentEnrollmentDTO.Response get(UUID id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận StudentEnrollmentDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về StudentEnrollmentDTO.Response theo kết quả xử lý.
     */
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
    /**
     * Tác dụng: Cập nhật bản ghi hiện có và trả về dữ liệu sau khi cập nhật.
     * Input: Nhận UUID id, StudentEnrollmentDTO.UpdateRequest request từ caller hoặc request.
     * Output: Trả về StudentEnrollmentDTO.Response theo kết quả xử lý.
     */
    public StudentEnrollmentDTO.Response update(UUID id, StudentEnrollmentDTO.UpdateRequest request) {
        var entity = find(id);
        entity.setStudentPerson(personRepository.findById(request.studentPersonId()).orElseThrow(() -> new IllegalArgumentException("Person not found")));
        entity.setCoursePurchase(coursePurchaseRepository.findById(request.coursePurchaseId()).orElseThrow(() -> new IllegalArgumentException("CoursePurchase not found")));
        entity.setClassSchedule(classScheduleRepository.findById(request.classScheduleId()).orElseThrow(() -> new IllegalArgumentException("ClassSchedule not found")));
        mapper.updateEntity(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    /**
     * Tác dụng: Xóa hoặc vô hiệu hóa bản ghi theo định danh đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    public void delete(UUID id) {
        var entity = find(id);
        entity.setStatus(StudentEnrollmentStatus.CANCELLED);
    }

    /**
     * Tác dụng: Tìm và trả về dữ liệu nội bộ theo điều kiện đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về StudentEnrollment theo kết quả xử lý.
     */
    private StudentEnrollment find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("StudentEnrollment not found"));
    }
}


