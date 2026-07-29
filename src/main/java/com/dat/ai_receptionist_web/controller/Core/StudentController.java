package com.dat.ai_receptionist_web.controller.Core;

import com.dat.ai_receptionist_web.dto.Core.StudentReqDTO;
import com.dat.ai_receptionist_web.dto.Core.StudentResDTO;
import com.dat.ai_receptionist_web.dto.Report.YearlySummaryDTO;
import com.dat.ai_receptionist_web.enums.Core.Belt;
import com.dat.ai_receptionist_web.enums.Core.StudentStatus;
import com.dat.ai_receptionist_web.service.Core.StudentService;
import com.dat.ai_receptionist_web.service.Report.StudentSummaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@securityRule.isManagerSenior(authentication)")
    public ResponseEntity<StudentResDTO.StudentDetail> createStudent(
            @RequestBody @Valid StudentReqDTO.StudentCreate createDTO) {
        log.info("Request create student: {}", createDTO.getFullName());

        StudentResDTO.StudentDetail newStudent = studentService.createStudent(createDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(newStudent);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@securityRule.isManagerSenior(authentication)")
    public ResponseEntity<StudentResDTO.StudentDetail> createStudentMultipart(
            @RequestPart("data") @Valid StudentReqDTO.StudentCreate createDTO,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        log.info("Request create student with face image: {}", createDTO.getFullName());
        return ResponseEntity.status(HttpStatus.CREATED).body(studentService.createStudent(createDTO, file));
    }

    @PreAuthorize("@securityRule.isCoach(authentication)")
    @GetMapping
    public ResponseEntity<StudentResDTO.StudentListResponse> getStudents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) StudentStatus status,
            @RequestParam(required = false) List<String> scheduleIds, // Thêm filter theo lớp học
            @RequestParam(required = false) List<Belt> belts,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "studentCode") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        log.info("Request get students - page: {}, size: {}", page, size);

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        // Gọi hàm Service mới
        StudentResDTO.StudentListResponse response = studentService
                .getStudentsWithStats(search, status, pageable, scheduleIds, belts);

        return ResponseEntity.ok(response);
    }

    /**
     * Lấy thông tin chi tiết học viên
     * GET /api/v1/students/{studentCode}
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
     * PUT /api/v1/students/{personId}
     */
    @PutMapping(value = "/{personId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@securityRule.isManagerSenior(authentication)")
    public ResponseEntity<StudentResDTO.StudentDetail> updateStudent(
            @PathVariable UUID personId,
            @RequestBody @Valid StudentReqDTO.StudentUpdate updateDTO) {
        log.info("Request update student: {}", personId);

        // Set personId từ path variable
        updateDTO.setPersonId(personId);

        StudentResDTO.StudentDetail updatedStudent = studentService.updateStudent(updateDTO);

        return ResponseEntity.ok(updatedStudent);
    }

    @PutMapping(value = "/{personId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@securityRule.isManagerSenior(authentication)")
    public ResponseEntity<StudentResDTO.StudentDetail> updateStudentMultipart(
            @PathVariable UUID personId,
            @RequestPart("data") @Valid StudentReqDTO.StudentUpdate updateDTO,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        log.info("Request update student with face image: {}", personId);
        updateDTO.setPersonId(personId);
        return ResponseEntity.ok(studentService.updateStudent(updateDTO, file));
    }

    /**
     * Xóa học viên (Soft Delete)
     * DELETE /api/v1/students/{studentCode}
     */
    @DeleteMapping("/{studentCode}")
    @PreAuthorize("@securityRule.isManagerSenior(authentication)")
    public ResponseEntity<Void> deleteStudent(@PathVariable String studentCode) {
        log.info("Request delete student: {}", studentCode);

        studentService.deleteStudent(studentCode);

        return ResponseEntity.ok().build();
    }

    /**
     * Xóa vĩnh viễn học viên (Hard Delete) - ADMIN ONLY
     * DELETE /api/v1/students/{studentCode}/permanent
     * ⚠️ CẢNH BÁO: Không thể hoàn tác!
     */
    @DeleteMapping("/{studentCode}/permanent")
    @PreAuthorize("@securityRule.isManagerSenior(authentication)")
    public ResponseEntity<Void> permanentlyDeleteStudent(@PathVariable String studentCode) {
        log.warn("⚠️ Request PERMANENTLY delete student: {}", studentCode);

        studentService.permanentlyDeleteStudent(studentCode);

        return ResponseEntity.ok().build();
    }
}

