package com.dat.backend_v2_1.controller.Core;

import com.dat.backend_v2_1.dto.Core.StudentReqDTO;
import com.dat.backend_v2_1.dto.Core.StudentResDTO;
import com.dat.backend_v2_1.dto.Report.YearlySummaryDTO;
import com.dat.backend_v2_1.enums.Core.StudentStatus;
import com.dat.backend_v2_1.service.Core.StudentService;
import com.dat.backend_v2_1.service.Report.StudentSummaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/students")
public class StudentController {

    private final StudentService studentService;
    private final StudentSummaryService studentSummaryService;

    /**
     * Tạo học viên mới
     * POST /api/v1/students
     *
     * @param createDTO Thông tin tạo mới học viên (bao gồm cả enrollment nếu muốn)
     * @return 201 Created - Thông tin chi tiết học viên vừa tạo
     */
    @PostMapping
    @PreAuthorize("@securityRule.isManagerSenior(authentication)")
    public ResponseEntity<StudentResDTO.StudentDetail> createStudent(
            @RequestBody @Valid StudentReqDTO.StudentCreate createDTO) {
        log.info("Request create student: {}", createDTO.getFullName());

        StudentResDTO.StudentDetail newStudent = studentService.createStudent(createDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(newStudent);
    }

    @PreAuthorize("@securityRule.isCoach(authentication)")
    @GetMapping
    public ResponseEntity<StudentResDTO.StudentListResponse> getStudents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) StudentStatus status,
            @RequestParam(required = false) List<String> scheduleIds, // Thêm filter theo lớp học
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "userId") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        log.info("Request get students - page: {}, size: {}", page, size);

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        // Gọi hàm Service mới
        StudentResDTO.StudentListResponse response = studentService
                .getStudentsWithStats(search, status, pageable, scheduleIds);

        return ResponseEntity.ok(response);
    }

    /**
     * Lấy thông tin chi tiết học viên
     * GET /api/v1/students/{userId}
     */
//    @PreAuthorize("@securityRule.isS(authentication)")
    @GetMapping("/{studentCode}")
    public ResponseEntity<StudentResDTO.StudentDetail> getStudentDetail(
            @PathVariable String studentCode) {
        log.info("Request get student detail: {}", studentCode);

        StudentResDTO.StudentDetail studentDetail = studentService.getStudentDetail(studentCode);

        return ResponseEntity.ok(studentDetail);
    }

    @GetMapping("/{studentCode}/yearly-summary")
    public ResponseEntity<YearlySummaryDTO.YearlySummaryResponse> getYearlySummary(
            @PathVariable String studentCode,
            @RequestParam int year) {
        log.info("Request get yearly summary for student: {}, year: {}", studentCode, year);

        YearlySummaryDTO.YearlySummaryResponse summary = studentSummaryService.getYearlySummary(studentCode, year);

        return ResponseEntity.ok(summary);
    }

    /**
     * Cập nhật thông tin học viên
     * PUT /api/v1/students/{userId}
     */
    @PutMapping("/{userId}")
    @PreAuthorize("@securityRule.isManagerSenior(authentication)")
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
    @PreAuthorize("@securityRule.isManagerSenior(authentication)")
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
    @PreAuthorize("@securityRule.isManagerSenior(authentication)")
    public ResponseEntity<Void> permanentlyDeleteStudent(@PathVariable UUID userId) {
        log.warn("⚠️ Request PERMANENTLY delete student: {}", userId);

        studentService.permanentlyDeleteStudent(userId);

        return ResponseEntity.ok().build();
    }
}
