package com.dat.backend_v2_1.controller.Core;

import com.dat.backend_v2_1.dto.Core.StudentReqDTO;
import com.dat.backend_v2_1.dto.Core.StudentResDTO;
import com.dat.backend_v2_1.enums.Core.StudentStatus;
import com.dat.backend_v2_1.service.Core.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/students")
public class StudentController {

    private final StudentService studentService;

    /**
     * Tạo học viên mới
     * POST /api/v1/students
     */
    @PostMapping
    public ResponseEntity<String> createStudent(
            @RequestBody @Valid StudentReqDTO.StudentCreate createDTO) {
        log.info("Request create student: {}", createDTO.getFullName());

        String newStudentCode = studentService.createStudent(createDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(newStudentCode);
    }

    @GetMapping
    public ResponseEntity<StudentResDTO.StudentListResponse> getStudents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) StudentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        log.info("Request get students - page: {}, size: {}", page, size);

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        // Gọi hàm Service mới
        StudentResDTO.StudentListResponse response = studentService.getStudentsWithStats(search, status, pageable);

        return ResponseEntity.ok(response);
    }

    /**
     * Lấy thông tin chi tiết học viên
     * GET /api/v1/students/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<StudentResDTO.StudentDetail> getStudentDetail(
            @PathVariable UUID userId) {
        log.info("Request get student detail: {}", userId);

        StudentResDTO.StudentDetail studentDetail = studentService.getStudentDetail(userId);

        return ResponseEntity.ok(studentDetail);
    }

    /**
     * Cập nhật thông tin học viên
     * PUT /api/v1/students/{userId}
     */
    @PutMapping("/{userId}")
    public ResponseEntity<StudentResDTO.StudentDetail> updateStudent(
            @PathVariable UUID userId,
            @RequestBody @Valid StudentReqDTO.StudentUpdate updateDTO) {
        log.info("Request update student: {}", userId);

        // Set userId từ path variable
        updateDTO.setUserId(userId);

        StudentResDTO.StudentDetail updatedStudent = studentService.updateStudent(updateDTO);

        return ResponseEntity.ok(updatedStudent);
    }

    /**
     * Xóa học viên (Soft Delete)
     * DELETE /api/v1/students/{userId}
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteStudent(@PathVariable UUID userId) {
        log.info("Request delete student: {}", userId);

        studentService.deleteStudent(userId);

        return ResponseEntity.ok().build();
    }

    /**
     * Xóa vĩnh viễn học viên (Hard Delete) - ADMIN ONLY
     * DELETE /api/v1/students/{userId}/permanent
     * ⚠️ CẢNH BÁO: Không thể hoàn tác!
     */
    @DeleteMapping("/{userId}/permanent")
    public ResponseEntity<Void> permanentlyDeleteStudent(@PathVariable UUID userId) {
        log.warn("⚠️ Request PERMANENTLY delete student: {}", userId);

        studentService.permanentlyDeleteStudent(userId);

        return ResponseEntity.ok().build();
    }
}
