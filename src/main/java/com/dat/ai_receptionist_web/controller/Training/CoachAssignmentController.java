package com.dat.ai_receptionist_web.controller.Training;

import com.dat.ai_receptionist_web.dto.Training.CoachAssignmentDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.service.Training.CoachAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/coach-assignments")
@RequiredArgsConstructor
public class CoachAssignmentController {
    private final CoachAssignmentService service;

    @GetMapping
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).COACH_ASSIGNMENT_READ.getCode())")
    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<CoachAssignmentDTO.Response> theo kết quả xử lý.
     */
    public PageResponse<CoachAssignmentDTO.Response> list(Pageable pageable) { return service.list(pageable); }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).COACH_ASSIGNMENT_READ.getCode())")
    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về CoachAssignmentDTO.Response theo kết quả xử lý.
     */
    public CoachAssignmentDTO.Response get(@PathVariable UUID id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).COACH_ASSIGNMENT_CREATE.getCode())")
    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận CoachAssignmentDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về CoachAssignmentDTO.Response theo kết quả xử lý.
     */
    public CoachAssignmentDTO.Response create(@Valid @RequestBody CoachAssignmentDTO.CreateRequest request) { return service.create(request); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).COACH_ASSIGNMENT_UPDATE.getCode())")
    /**
     * Tác dụng: Cập nhật bản ghi hiện có và trả về dữ liệu sau khi cập nhật.
     * Input: Nhận UUID id, CoachAssignmentDTO.UpdateRequest request từ caller hoặc request.
     * Output: Trả về CoachAssignmentDTO.Response theo kết quả xử lý.
     */
    public CoachAssignmentDTO.Response update(@PathVariable UUID id, @Valid @RequestBody CoachAssignmentDTO.UpdateRequest request) { return service.update(id, request); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).COACH_ASSIGNMENT_DELETE.getCode())")
    /**
     * Tác dụng: Xóa hoặc vô hiệu hóa bản ghi theo định danh đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    public void delete(@PathVariable UUID id) { service.delete(id); }
}


