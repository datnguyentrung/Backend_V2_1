package com.dat.ai_receptionist_web.mapper.Operation;

import com.dat.ai_receptionist_web.domain.Operation.TuitionPayment;
import com.dat.ai_receptionist_web.domain.Operation.TuitionPaymentDetail;
import com.dat.ai_receptionist_web.domain.Operation.StudentEnrollment;
import com.dat.ai_receptionist_web.dto.Operation.TuitionPaymentDTO;
import com.dat.ai_receptionist_web.dto.Operation.TuitionPaymentDetailDTO;
import com.dat.ai_receptionist_web.mapper.Core.StudentMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = org.mapstruct.NullValuePropertyMappingStrategy.IGNORE,
        uses = {StudentMapper.class} // Sử dụng StudentMapper để map Student sang Student
)
public interface TuitionPaymentMapper {

    /**
     * 2. Map từng dòng Detail entity sang DetailResponse DTO
     */
    // Lấy ID của enrollment
    @Mapping(target = "enrollmentId", source = "enrollment.enrollmentId")
    // Tùy thuộc vào Entity StudentEnrollment của bạn, có thể bạn cần lấy scheduleId thông qua classSchedule
    // VD: source = "enrollment.classSchedule.scheduleId" hoặc source = "enrollment.scheduleId"
    @Mapping(target = "scheduleId", source = "enrollment.classSchedule.scheduleId")
    TuitionPaymentDetailDTO.TuitionPaymentDetailResponse toDetailResponse(TuitionPaymentDetail detail);

    /**
     * 3. Kết hợp Payment và list Details lại thành Response tổng
     */
    @Mapping(target = "paymentId", source = "payment.paymentId")
    @Mapping(target = "totalAmount", source = "payment.totalAmount")
    @Mapping(target = "note", source = "payment.note")
    @Mapping(target = "createdAt", source = "payment.createdAt")
    @Mapping(target = "student", source = "payment.student") // Sẽ tự gọi hàm toStudentSummary ở trên
    @Mapping(target = "details", source = "details")
    // MapStruct tự động lặp list và gọi hàm toDetailResponse
    TuitionPaymentDTO.TuitionPaymentResponse toResponse(TuitionPayment payment, List<TuitionPaymentDetail> details);

    @Mapping(target = "className", source = "enrollment.classSchedule.scheduleId")
    @Mapping(target = "paidAt", source = "tuitionPayment.createdAt")
    TuitionPaymentDTO.PaymentHistoryItem toPaymentHistoryItem(TuitionPaymentDetail detail);

    default TuitionPaymentDTO.PaymentHistoryItem toPaymentHistoryItem(
            TuitionPaymentDetail detail,
            StudentEnrollment enrollment) {
        TuitionPaymentDTO.PaymentHistoryItem item = toPaymentHistoryItem(detail);
        item.setClassName(enrollment.getClassSchedule().getScheduleId());
        return item;
    }

    default TuitionPaymentDetailDTO.ActiveClassStatus toActiveClassStatus(
            StudentEnrollment enrollment,
            TuitionPaymentDetail paidDetail) {
        return TuitionPaymentDetailDTO.ActiveClassStatus.builder()
                .enrollmentId(enrollment.getEnrollmentId())
                .scheduleId(enrollment.getClassSchedule().getScheduleId())
                .paid(paidDetail != null)
                .amountAllocated(paidDetail != null ? paidDetail.getAmountAllocated() : null)
                .build();
    }

    default List<TuitionPaymentDetailDTO.ActiveClassStatus> toActiveClassStatuses(
            List<StudentEnrollment> activeEnrollments,
            List<TuitionPaymentDetail> paidDetails) {
        Map<UUID, TuitionPaymentDetail> paidMap = paidDetails.stream()
                .collect(Collectors.toMap(
                        detail -> detail.getEnrollment().getEnrollmentId(),
                        detail -> detail,
                        (existing, replacement) -> existing
                ));
        return activeEnrollments.stream()
                .map(enrollment -> toActiveClassStatus(enrollment, paidMap.get(enrollment.getEnrollmentId())))
                .toList();
    }
}
