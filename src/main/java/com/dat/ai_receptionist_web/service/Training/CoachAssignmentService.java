package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Training.CoachAssignment;
import com.dat.ai_receptionist_web.dto.Training.CoachAssignmentDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.mapper.Training.CoachAssignmentMapper;
import com.dat.ai_receptionist_web.repository.Training.CoachAssignmentRepository;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.repository.Catalog.CourseRepository;
import com.dat.ai_receptionist_web.enums.Operation.CoachAssignmentStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CoachAssignmentService {
    private final CoachAssignmentRepository repository;
    private final CoachAssignmentMapper mapper;
    private final PersonRepository personRepository;
    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<CoachAssignmentDTO.Response> theo kết quả xử lý.
     */
    public PageResponse<CoachAssignmentDTO.Response> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về CoachAssignmentDTO.Response theo kết quả xử lý.
     */
    public CoachAssignmentDTO.Response get(UUID id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận CoachAssignmentDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về CoachAssignmentDTO.Response theo kết quả xử lý.
     */
    public CoachAssignmentDTO.Response create(CoachAssignmentDTO.CreateRequest request) {
        CoachAssignment entity = new CoachAssignment();
        entity.setCoach(personRepository.findById(request.coachId()).orElseThrow(() -> new IllegalArgumentException("Person not found")));
        entity.setCourse(courseRepository.findById(request.courseId()).orElseThrow(() -> new IllegalArgumentException("Course not found")));
        entity.setAssignedDate(request.assignedDate());
        entity.setEndDate(request.endDate());
        entity.setCoachAssignmentStatus(request.coachAssignmentStatus());
        entity.setNote(request.note());
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    /**
     * Tác dụng: Cập nhật bản ghi hiện có và trả về dữ liệu sau khi cập nhật.
     * Input: Nhận UUID id, CoachAssignmentDTO.UpdateRequest request từ caller hoặc request.
     * Output: Trả về CoachAssignmentDTO.Response theo kết quả xử lý.
     */
    public CoachAssignmentDTO.Response update(UUID id, CoachAssignmentDTO.UpdateRequest request) {
        var entity = find(id);
        entity.setCoach(personRepository.findById(request.coachId()).orElseThrow(() -> new IllegalArgumentException("Person not found")));
        entity.setCourse(courseRepository.findById(request.courseId()).orElseThrow(() -> new IllegalArgumentException("Course not found")));
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
        entity.setCoachAssignmentStatus(CoachAssignmentStatus.CANCELLED);
    }

    /**
     * Tác dụng: Tìm và trả về dữ liệu nội bộ theo điều kiện đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về CoachAssignment theo kết quả xử lý.
     */
    private CoachAssignment find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("CoachAssignment not found"));
    }
}


