package com.dat.ai_receptionist_web.controller.Training;

import com.dat.ai_receptionist_web.dto.Training.StudentAttendanceDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.service.Training.StudentAttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/student-attendances")
@RequiredArgsConstructor
public class StudentAttendanceController {
    private final StudentAttendanceService service;

    @GetMapping
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).STUDENT_ATTENDANCE_READ.getCode())")
    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<StudentAttendanceDTO.Response> theo kết quả xử lý.
     */
    public PageResponse<StudentAttendanceDTO.Response> list(Pageable pageable) { return service.list(pageable); }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).STUDENT_ATTENDANCE_READ.getCode())")
    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về StudentAttendanceDTO.Response theo kết quả xử lý.
     */
    public StudentAttendanceDTO.Response get(@PathVariable UUID id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).STUDENT_ATTENDANCE_CREATE.getCode())")
    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận StudentAttendanceDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về StudentAttendanceDTO.Response theo kết quả xử lý.
     */
    public StudentAttendanceDTO.Response create(@Valid @RequestBody StudentAttendanceDTO.CreateRequest request) { return service.create(request); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).STUDENT_ATTENDANCE_UPDATE.getCode())")
    /**
     * Tác dụng: Cập nhật bản ghi hiện có và trả về dữ liệu sau khi cập nhật.
     * Input: Nhận UUID id, StudentAttendanceDTO.UpdateRequest request từ caller hoặc request.
     * Output: Trả về StudentAttendanceDTO.Response theo kết quả xử lý.
     */
    public StudentAttendanceDTO.Response update(@PathVariable UUID id, @Valid @RequestBody StudentAttendanceDTO.UpdateRequest request) { return service.update(id, request); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).STUDENT_ATTENDANCE_DELETE.getCode())")
    /**
     * Tác dụng: Xóa hoặc vô hiệu hóa bản ghi theo định danh đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    public void delete(@PathVariable UUID id) { service.delete(id); }
}


