package com.dat.ai_receptionist_web.controller.Catalog;

import com.dat.ai_receptionist_web.dto.Catalog.CoursePriceDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.service.Catalog.CoursePriceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/course-prices")
@RequiredArgsConstructor
public class CoursePriceController {
    private final CoursePriceService service;

    @GetMapping
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).COURSE_PRICE_READ.getCode())")
    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<CoursePriceDTO.Response> theo kết quả xử lý.
     */
    public PageResponse<CoursePriceDTO.Response> list(Pageable pageable) { return service.list(pageable); }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).COURSE_PRICE_READ.getCode())")
    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về CoursePriceDTO.Response theo kết quả xử lý.
     */
    public CoursePriceDTO.Response get(@PathVariable UUID id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).COURSE_PRICE_CREATE.getCode())")
    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận CoursePriceDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về CoursePriceDTO.Response theo kết quả xử lý.
     */
    public CoursePriceDTO.Response create(@Valid @RequestBody CoursePriceDTO.CreateRequest request) { return service.create(request); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).COURSE_PRICE_UPDATE.getCode())")
    /**
     * Tác dụng: Cập nhật bản ghi hiện có và trả về dữ liệu sau khi cập nhật.
     * Input: Nhận UUID id, CoursePriceDTO.UpdateRequest request từ caller hoặc request.
     * Output: Trả về CoursePriceDTO.Response theo kết quả xử lý.
     */
    public CoursePriceDTO.Response update(@PathVariable UUID id, @Valid @RequestBody CoursePriceDTO.UpdateRequest request) { return service.update(id, request); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).COURSE_PRICE_DELETE.getCode())")
    /**
     * Tác dụng: Xóa hoặc vô hiệu hóa bản ghi theo định danh đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    public void delete(@PathVariable UUID id) { service.delete(id); }
}


