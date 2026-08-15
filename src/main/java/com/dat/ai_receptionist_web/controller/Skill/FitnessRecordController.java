package com.dat.ai_receptionist_web.controller.Skill;

import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Skill.FitnessRecordDTO;
import com.dat.ai_receptionist_web.enums.Skill.SkillLevel;
import com.dat.ai_receptionist_web.service.Skill.FitnessRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/fitness-record")
public class FitnessRecordController {
    private final FitnessRecordService fitnessRecordService;

    /**
     * API Tạo mới kỷ lục Fitness
     * POST /api/v1/fitness-record
     */
    @PostMapping
    public ResponseEntity<FitnessRecordDTO.Response> create(
            @Valid @RequestBody FitnessRecordDTO.CreateRequest request) { // Nên có @Valid để validate đầu vào

        log.info("REST request to create FitnessRecord for Student ID: {}", request.getStudentCode());
        FitnessRecordDTO.Response response = fitnessRecordService.createFitnessRecord(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FitnessRecordDTO.Response> update(
            @PathVariable Long id,
            @Valid @RequestBody FitnessRecordDTO.UpdateRequest request) {
        return ResponseEntity.ok(fitnessRecordService.updateFitnessRecord(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        fitnessRecordService.deleteFitnessRecord(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * API Lấy danh sách kỷ lục Fitness có phân trang và tìm kiếm
     * GET /api/v1/fitness-record?search=abc&skillLevel=BEGINNER&page=0&size=20
     */
    @GetMapping
    public ResponseEntity<PageResponse<FitnessRecordDTO.ListResponse>> getList(
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false) SkillLevel skillLevel,
            @PageableDefault(
                    size = 20,
                    sort = "assessmentDate",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        log.info("REST request to get a page of FitnessRecords");
        PageResponse<FitnessRecordDTO.ListResponse> response =
                fitnessRecordService.listFitnessRecords(search, skillLevel, pageable);

        return ResponseEntity.ok(response);
    }


}
