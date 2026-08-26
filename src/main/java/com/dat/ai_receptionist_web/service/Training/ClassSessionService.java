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
    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<ClassSessionDTO.Response> theo kết quả xử lý.
     */
    public PageResponse<ClassSessionDTO.Response> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về ClassSessionDTO.Response theo kết quả xử lý.
     */
    public ClassSessionDTO.Response get(UUID id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận ClassSessionDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về ClassSessionDTO.Response theo kết quả xử lý.
     */
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
    /**
     * Tác dụng: Cập nhật bản ghi hiện có và trả về dữ liệu sau khi cập nhật.
     * Input: Nhận UUID id, ClassSessionDTO.UpdateRequest request từ caller hoặc request.
     * Output: Trả về ClassSessionDTO.Response theo kết quả xử lý.
     */
    public ClassSessionDTO.Response update(UUID id, ClassSessionDTO.UpdateRequest request) {
        var entity = find(id);
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
        entity.setStatus(SessionStatus.CANCELLED);
    }

    /**
     * Tác dụng: Tìm và trả về dữ liệu nội bộ theo điều kiện đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về ClassSession theo kết quả xử lý.
     */
    private ClassSession find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("ClassSession not found"));
    }
}


