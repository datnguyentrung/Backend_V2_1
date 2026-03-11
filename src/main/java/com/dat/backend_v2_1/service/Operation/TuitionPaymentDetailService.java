package com.dat.backend_v2_1.service.Operation;

import com.dat.backend_v2_1.domain.Operation.TuitionPaymentDetail;
import com.dat.backend_v2_1.dto.Operation.TuitionPaymentDetailDTO;
import com.dat.backend_v2_1.repository.Operation.TuitionPaymentDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TuitionPaymentDetailService {

    private final TuitionPaymentDetailRepository tuitionPaymentDetailRepository;

    /**
     * Kiểm tra nhanh 1 enrollment đã đóng phí tháng hiện tại chưa.
     * Dùng trực tiếp trong luồng Check-in của AI Receptionist.
     */
    @Transactional(readOnly = true)
    public boolean isPaidForCurrentMonth(UUID enrollmentId) {
        LocalDate now = LocalDate.now();
        return tuitionPaymentDetailRepository
                .findByEnrollment_EnrollmentIdAndForMonthAndForYear(
                        enrollmentId, now.getMonthValue(), now.getYear())
                .isPresent();
    }

    /**
     * Lấy danh sách tất cả detail của 1 payment (dùng khi cần hiện lại hóa đơn).
     */
    @Transactional(readOnly = true)
    public List<TuitionPaymentDetailDTO.TuitionPaymentDetailResponse> getDetailsByPaymentId(UUID paymentId) {
        return tuitionPaymentDetailRepository
                .findByTuitionPayment_PaymentId(paymentId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ---- helpers ----

    private TuitionPaymentDetailDTO.TuitionPaymentDetailResponse toResponse(TuitionPaymentDetail d) {
        return TuitionPaymentDetailDTO.TuitionPaymentDetailResponse.builder()
                .detailId(d.getDetailId())
                .enrollmentId(d.getEnrollment().getEnrollmentId())
                .scheduleId(d.getEnrollment().getClassSchedule().getScheduleId())
                .forMonth(d.getForMonth())
                .forYear(d.getForYear())
                .amountAllocated(d.getAmountAllocated())
                .build();
    }
}
