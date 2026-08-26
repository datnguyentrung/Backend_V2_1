package com.dat.ai_receptionist_web.service.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.Course;
import com.dat.ai_receptionist_web.dto.Catalog.CourseDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CatalogErrorCode;
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
public class CourseService {
    private final CourseRepository repository;
    private final CourseMapper mapper;
    private final ClassScheduleRepository classScheduleRepository;

    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<CourseDTO.Response> theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public PageResponse<CourseDTO.Response> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toResponse);
    }

    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về CourseDTO.Response theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public CourseDTO.Response get(UUID id) {
        return mapper.toResponse(find(id));
    }

    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận CourseDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về CourseDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public CourseDTO.Response create(CourseDTO.CreateRequest request) {
        Course entity = new Course();
        entity.setClassSchedule(classScheduleRepository.findById(request.classScheduleId()).orElseThrow(() -> new ApiException(CatalogErrorCode.CLASS_SCHEDULE_NOT_FOUND)));
        entity.setCapacity(request.capacity());
        entity.setStatus(request.status());
        entity.setName(request.name());
        return mapper.toResponse(repository.save(entity));
    }

    /**
     * Tác dụng: Cập nhật bản ghi hiện có và trả về dữ liệu sau khi cập nhật.
     * Input: Nhận UUID id, CourseDTO.UpdateRequest request từ caller hoặc request.
     * Output: Trả về CourseDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public CourseDTO.Response update(UUID id, CourseDTO.UpdateRequest request) {
        var entity = find(id);
        entity.setClassSchedule(classScheduleRepository.findById(request.classScheduleId()).orElseThrow(() -> new ApiException(CatalogErrorCode.CLASS_SCHEDULE_NOT_FOUND)));
        entity.setName(request.name());
        entity.setCapacity(request.capacity());
        entity.setStatus(request.status());
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
        entity.setStatus(CourseStatus.CANCELLED);
    }

    /**
     * Tác dụng: Tìm và trả về dữ liệu nội bộ theo điều kiện đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về Course theo kết quả xử lý.
     */
    private Course find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(CatalogErrorCode.COURSE_NOT_FOUND));
    }
}


