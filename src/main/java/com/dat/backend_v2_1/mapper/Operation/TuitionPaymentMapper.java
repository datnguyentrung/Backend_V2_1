package com.dat.backend_v2_1.mapper.Operation;

import com.dat.backend_v2_1.domain.Core.Student;
import com.dat.backend_v2_1.domain.Operation.TuitionPayment;
import com.dat.backend_v2_1.domain.Operation.TuitionPaymentDetail;
import com.dat.backend_v2_1.dto.Core.StudentResDTO;
import com.dat.backend_v2_1.dto.Operation.TuitionPaymentDTO;
import com.dat.backend_v2_1.dto.Operation.TuitionPaymentDetailDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface TuitionPaymentMapper {
    /**
     * 1. Map Student entity sang StudentSummary DTO
     */
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "fullName", source = "fullName")
//    @Mapping(target = "email", source = "email") // Giả định email nằm ở class User cha của Student
    @Mapping(target = "code", source = "studentCode")
    // Dựa theo StudentMapper của bạn
    StudentResDTO.StudentSummary toStudentSummary(Student student);

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
}
