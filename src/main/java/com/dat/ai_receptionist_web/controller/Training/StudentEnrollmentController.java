package com.dat.ai_receptionist_web.controller.Training;

import com.dat.ai_receptionist_web.dto.Training.StudentEnrollmentDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.service.Training.StudentEnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/student-enrollments")
@RequiredArgsConstructor
public class StudentEnrollmentController {
    private final StudentEnrollmentService service;

    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<StudentEnrollmentDTO.Response> theo kết quả xử lý.
     */
    @GetMapping
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).STUDENT_ENROLLMENT_READ.getCode())")
    public PageResponse<StudentEnrollmentDTO.Response> list(Pageable pageable) { return service.list(pageable); }

    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về StudentEnrollmentDTO.Response theo kết quả xử lý.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).STUDENT_ENROLLMENT_READ.getCode())")
    public StudentEnrollmentDTO.Response get(@PathVariable UUID id) { return service.get(id); }

    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận StudentEnrollmentDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về StudentEnrollmentDTO.Response theo kết quả xử lý.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).STUDENT_ENROLLMENT_CREATE.getCode())")
    public StudentEnrollmentDTO.Response create(@Valid @RequestBody StudentEnrollmentDTO.CreateRequest request) { return service.create(request); }

    /**
     * Tác dụng: Cập nhật bản ghi hiện có và trả về dữ liệu sau khi cập nhật.
     * Input: Nhận UUID id, StudentEnrollmentDTO.UpdateRequest request từ caller hoặc request.
     * Output: Trả về StudentEnrollmentDTO.Response theo kết quả xử lý.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).STUDENT_ENROLLMENT_UPDATE.getCode())")
    public StudentEnrollmentDTO.Response update(@PathVariable UUID id, @Valid @RequestBody StudentEnrollmentDTO.UpdateRequest request) { return service.update(id, request); }

    /**
     * Tác dụng: Xóa hoặc vô hiệu hóa bản ghi theo định danh đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).STUDENT_ENROLLMENT_DELETE.getCode())")
    public void delete(@PathVariable UUID id) { service.delete(id); }
}


