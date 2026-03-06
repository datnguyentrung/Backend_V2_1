package com.dat.backend_v2_1.controller.Core;

import com.dat.backend_v2_1.dto.Core.ClassScheduleReqDTO;
import com.dat.backend_v2_1.dto.Core.ClassScheduleResDTO;
import com.dat.backend_v2_1.dto.RestResponse;
import com.dat.backend_v2_1.service.Core.ClassScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller quản lý lịch học (Class Schedule)
 * Xử lý các thao tác CRUD và tra cứu thông tin lịch học
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/class-schedules")
public class ClassScheduleController {

    private final ClassScheduleService classScheduleService;

    /**
     * Lấy danh sách tất cả lịch học
     * GET /api/v1/class-schedules
     *
     * Trả về danh sách đầy đủ lịch học kèm theo:
     * - Thông tin chi nhánh
     * - Danh sách huấn luyện viên được phân công
     * - Số lượng học viên đang theo học
     *
     * @return 200 OK - Danh sách lịch học
     */
    @GetMapping
    public ResponseEntity<List<ClassScheduleResDTO.ClassScheduleDetail>> getAllClassSchedules() {
        log.info("Request to get all class schedules");

        List<ClassScheduleResDTO.ClassScheduleDetail> schedules = classScheduleService.getAllClassSchedules();

        return ResponseEntity.ok(schedules);
    }

    /**
     * Lấy thông tin chi tiết một lịch học
     * GET /api/v1/class-schedules/{scheduleId}
     *
     * @param scheduleId ID của lịch học
     * @return 200 OK - Thông tin chi tiết lịch học
     *         404 Not Found - Không tìm thấy lịch học
     */
    @GetMapping("/{scheduleId}")
    public ResponseEntity<ClassScheduleResDTO.ClassScheduleDetail> getClassScheduleDetail(
            @PathVariable String scheduleId) {
        log.info("Request to get class schedule detail: {}", scheduleId);

        ClassScheduleResDTO.ClassScheduleDetail schedule = classScheduleService.getClassScheduleDetail(scheduleId);

        return ResponseEntity.ok(schedule);
    }

    /**
     * Tạo mới lịch học
     * POST /api/v1/class-schedules
     *
     * @param request Thông tin lịch học cần tạo
     * @return 201 Created - Lịch học vừa tạo
     *         400 Bad Request - Dữ liệu không hợp lệ
     *         409 Conflict - Mã lịch học đã tồn tại
     */
    @PostMapping
    public ResponseEntity<ClassScheduleResDTO.ClassScheduleDetail> createClassSchedule(
            @Valid @RequestBody ClassScheduleReqDTO.CreateRequest request) {
        log.info("Request to create class schedule: {}", request.getScheduleId());

        ClassScheduleResDTO.ClassScheduleDetail createdSchedule = classScheduleService.createClassSchedule(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdSchedule);
    }

    /**
     * Cập nhật thông tin lịch học
     * PUT /api/v1/class-schedules/{scheduleId}
     *
     * Chỉ cập nhật các field không null trong request.
     * Không cho phép thay đổi scheduleId.
     *
     * @param scheduleId ID của lịch học cần cập nhật
     * @param request Thông tin cần cập nhật
     * @return 200 OK - Lịch học sau khi cập nhật
     *         404 Not Found - Không tìm thấy lịch học
     *         400 Bad Request - Dữ liệu không hợp lệ
     */
    @PutMapping("/{scheduleId}")
    public ResponseEntity<ClassScheduleResDTO.ClassScheduleDetail> updateClassSchedule(
            @PathVariable String scheduleId,
            @Valid @RequestBody ClassScheduleReqDTO.UpdateRequest request) {
        log.info("Request to update class schedule: {}", scheduleId);

        ClassScheduleResDTO.ClassScheduleDetail updatedSchedule =
                classScheduleService.updateClassSchedule(scheduleId, request);

        return ResponseEntity.ok(updatedSchedule);
    }

    /**
     * Xóa lịch học
     * DELETE /api/v1/class-schedules/{scheduleId}
     *
     * Chỉ cho phép xóa nếu:
     * - Không còn học viên nào đang theo học (status ACTIVE)
     * - Không còn huấn luyện viên nào được phân công (status ACTIVE)
     *
     * @param scheduleId ID của lịch học cần xóa
     * @return 200 OK - Xóa thành công
     *         404 Not Found - Không tìm thấy lịch học
     *         400 Bad Request - Không thể xóa vì còn học viên/HLV
     */
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Void> deleteClassSchedule(@PathVariable String scheduleId) {
        log.info("Request to delete class schedule: {}", scheduleId);

        classScheduleService.deleteClassSchedule(scheduleId);

        return ResponseEntity.ok().build();
    }
}
