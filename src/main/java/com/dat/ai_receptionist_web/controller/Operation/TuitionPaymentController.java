package com.dat.ai_receptionist_web.controller.Operation;

import com.dat.ai_receptionist_web.dto.Operation.TuitionPaymentDTO;
import com.dat.ai_receptionist_web.dto.Operation.TuitionPaymentDetailDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.service.Operation.TuitionPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller quản lý học phí (Tuition Payment)
 * <p>
 * Các luồng chính:
 * POST /api/v1/tuition-payments          → Đóng học phí
 * GET  /api/v1/tuition-payments/status/{studentId} → Kiểm tra trạng thái (AI Receptionist)
 * GET  /api/v1/tuition-payments/history/{studentId}         → Lịch sử tất cả lớp
 * GET  /api/v1/tuition-payments/history/enrollment/{enrollmentId} → Lịch sử 1 lớp cụ thể
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tuition-payments")
public class TuitionPaymentController {

    private final TuitionPaymentService tuitionPaymentService;

    // =========================================================================
    // A. ĐÓNG HỌC PHÍ
    // =========================================================================

    /**
     * Đóng học phí cho 1 lớp theo số tháng.
     * <p>
     * Body ví dụ:
     * {
     * "studentId": "uuid-học-viên",
     * "enrollmentId": "uuid-enrollment-lớp-P14C1",
     * "numberOfMonths": 3,
     * "note": "Mẹ đóng học phí quý 2"
     * }
     *
     * @return 201 Created + TuitionPaymentResponse (bao gồm danh sách tháng đã phân bổ)
     */
    @PreAuthorize("@securityRule.isManagerSenior(authentication)")
    @PostMapping
    public ResponseEntity<TuitionPaymentDTO.TuitionPaymentResponse> processPayment(
            @RequestBody @Valid TuitionPaymentDTO.ProcessPaymentRequest request) {

        log.info("REST request to process tuition payment for student [{}], enrollment [{}], {} month(s)",
                request.getStudentId(), request.getEnrollmentId(), request.getNumberOfMonths());

        TuitionPaymentDTO.TuitionPaymentResponse response = tuitionPaymentService.processPayment(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =========================================================================
    // B. KIỂM TRA TRẠNG THÁI — AI RECEPTIONIST
    // =========================================================================

    /**
     * Kiểm tra học viên đã đóng học phí tháng hiện tại chưa.
     * Dùng khi võ sinh quét mã check-in tại cửa.
     * <p>
     * hasPaidCurrentMonth = true  → Cho qua cửa
     * hasPaidCurrentMonth = false → Yêu cầu đóng phí
     *
     * @param studentId UUID của học viên
     * @return TuitionStatusResponse chứa trạng thái từng lớp đang học
     */
    @PreAuthorize("@securityRule.isCoach(authentication)")
    @GetMapping("/status/{studentId}")
    public ResponseEntity<TuitionPaymentDetailDTO.TuitionStatusResponse> checkTuitionStatus(
            @PathVariable UUID studentId) {

        log.info("REST request to check tuition status for student [{}]", studentId);

        TuitionPaymentDetailDTO.TuitionStatusResponse response =
                tuitionPaymentService.checkTuitionStatus(studentId);

        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // C. LỊCH SỬ ĐÓNG PHÍ
    // =========================================================================

    /**
     * Lấy toàn bộ lịch sử đóng phí của học viên (tất cả các lớp).
     * Dùng cho App phụ huynh xem tổng quát.
     *
     * @param studentId UUID của học viên
     * @return Danh sách PaymentHistoryItem, sắp xếp theo năm/tháng mới nhất trước
     */
    @PreAuthorize("@securityRule.isCoach(authentication)")
    @GetMapping("/history/{studentId}")
    public ResponseEntity<List<TuitionPaymentDTO.PaymentHistoryItem>> getPaymentHistory(
            @PathVariable UUID studentId) {

        log.info("REST request to get payment history for student [{}]", studentId);

        List<TuitionPaymentDTO.PaymentHistoryItem> history =
                tuitionPaymentService.getPaymentHistory(studentId);

        return ResponseEntity.ok(history);
    }

    /**
     * Lấy lịch sử đóng phí cho 1 enrollment (1 lớp cụ thể).
     * Dùng khi phụ huynh/admin muốn xem riêng lớp nào đó.
     *
     * @param enrollmentId UUID của enrollment
     * @return Danh sách PaymentHistoryItem của lớp đó
     */
    @PreAuthorize("@securityRule.isCoach(authentication)")
    @GetMapping("/history/enrollment/{enrollmentId}")
    public ResponseEntity<List<TuitionPaymentDTO.PaymentHistoryItem>> getPaymentHistoryByEnrollment(
            @PathVariable UUID enrollmentId) {

        log.info("REST request to get payment history for enrollment [{}]", enrollmentId);

        List<TuitionPaymentDTO.PaymentHistoryItem> history =
                tuitionPaymentService.getPaymentHistoryByEnrollment(enrollmentId);

        return ResponseEntity.ok(history);
    }

    // =========================================================================
    // D. QUẢN LÝ TỔNG THỂ — ADMIN / MANAGER
    // =========================================================================

    /**
     * Lấy danh sách toàn bộ lịch sử giao dịch đóng học phí trên hệ thống.
     * Có hỗ trợ tìm kiếm theo tên võ sinh, mã võ sinh, hoặc ghi chú.
     * Dành riêng cho Admin, Quản lý hoặc Coach Trainee xem báo cáo.
     *
     * @param search   Từ khóa tìm kiếm (tên, mã, note)
     * @param pageable Cấu hình phân trang (mặc định 10 dòng, xếp mới nhất lên đầu)
     * @return 200 OK + PageResponse chứa danh sách giao dịch
     */
//    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'COACH_TRAINEE')")
    @PreAuthorize("@securityRule.isManagerSenior(authentication)")
    @GetMapping
    public ResponseEntity<PageResponse<TuitionPaymentDTO.TuitionPaymentResponse>> getAllPaymentsForAdmin(
            @RequestParam(required = false, defaultValue = "") String search,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("REST request to get all tuition payments for admin with search: [{}]", search);

        PageResponse<TuitionPaymentDTO.TuitionPaymentResponse> responsePage =
                tuitionPaymentService.getPaymentHistoryForAdmin(search, pageable);

        return ResponseEntity.ok(responsePage);
    }
}

