package com.dat.ai_receptionist_web.service.Operation;

import com.dat.ai_receptionist_web.domain.Core.Student;
import com.dat.ai_receptionist_web.domain.Operation.StudentEnrollment;
import com.dat.ai_receptionist_web.domain.Operation.TuitionPayment;
import com.dat.ai_receptionist_web.domain.Operation.TuitionPaymentDetail;
import com.dat.ai_receptionist_web.dto.Operation.TuitionPaymentDTO;
import com.dat.ai_receptionist_web.dto.Operation.TuitionPaymentDetailDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.enums.ErrorCode;
import com.dat.ai_receptionist_web.enums.Operation.StudentEnrollmentStatus;
import com.dat.ai_receptionist_web.mapper.Operation.TuitionPaymentMapper;
import com.dat.ai_receptionist_web.repository.Operation.StudentEnrollmentRepository;
import com.dat.ai_receptionist_web.repository.Operation.TuitionPaymentDetailRepository;
import com.dat.ai_receptionist_web.repository.Operation.TuitionPaymentRepository;
import com.dat.ai_receptionist_web.service.Core.StudentService;
import com.dat.ai_receptionist_web.util.error.AppException;
import com.dat.ai_receptionist_web.util.error.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TuitionPaymentService {

    private final TuitionPaymentRepository tuitionPaymentRepository;
    private final TuitionPaymentDetailRepository tuitionPaymentDetailRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final StudentService studentService;
    private final TuitionPaymentMapper tuitionPaymentMapper;

    // =========================================================================
    // A. TÁC VỤ ĐÓNG PHÍ (CREATE PAYMENT)
    // =========================================================================

    /**
     * Xử lý đóng học phí cho 1 enrollment (1 lớp cụ thể) theo số tháng.
     * Tự động phân bổ số tiền vào từng tháng dựa trên monthlyFee của ClassSchedule.
     * Kiểm tra trùng lặp: nếu tháng nào đã đóng thì throw lỗi.
     *
     * @param request ProcessPaymentRequest chứa studentId, enrollmentId, numberOfMonths, note
     * @return TuitionPaymentResponse chứa thông tin hóa đơn và danh sách detail
     */
    @Transactional(rollbackFor = Exception.class)
    public TuitionPaymentDTO.TuitionPaymentResponse processPayment(TuitionPaymentDTO.ProcessPaymentRequest request) {

        // 1. Lấy thông tin học viên
        Student student = studentService.getStudentById(request.getStudentId());

        // 2. Lấy Enrollment, kiểm tra enrollment thuộc về học viên này và đang ACTIVE
        StudentEnrollment enrollment = studentEnrollmentRepository.findById(request.getEnrollmentId())
                .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_FOUND));

        if (!enrollment.getStudent().getPersonId().equals(request.getStudentId())) {
            throw new BusinessException("Enrollment không thuộc về học viên này");
        }
        if (enrollment.getStatus() != StudentEnrollmentStatus.ACTIVE) {
            throw new BusinessException("Học viên không đang học lớp này (trạng thái: " + enrollment.getStatus() + ")");
        }

        // 3. Lấy giá tiền theo tháng từ ClassSchedule
        BigDecimal monthlyFee = enrollment.getClassSchedule().getMonthlyFee();

        // 4. Xác định tháng bắt đầu tính từ tháng hiện tại
        LocalDate now = LocalDate.now();
        int startMonth = now.getMonthValue();
        int startYear = now.getYear();

        // 5. Kiểm tra trùng lặp: không cho đóng tháng đã đóng rồi
        for (int i = 0; i < request.getNumberOfMonths(); i++) {
            int targetMonth = ((startMonth - 1 + i) % 12) + 1;
            int targetYear = startYear + (startMonth - 1 + i) / 12;

            boolean alreadyPaid = tuitionPaymentDetailRepository
                    .findByEnrollment_EnrollmentIdAndForMonthAndForYear(
                            enrollment.getEnrollmentId(), targetMonth, targetYear)
                    .isPresent();

            if (alreadyPaid) {
                throw new AppException(ErrorCode.TUITION_ALREADY_PAID);
            }
        }

        // 6. Tính tổng tiền
        BigDecimal totalAmount = monthlyFee.multiply(BigDecimal.valueOf(request.getNumberOfMonths()));

        // 7. Tạo và lưu TuitionPayment (bằng chứng giao dịch)
        TuitionPayment payment = TuitionPayment.builder()
                .student(student)
                .totalAmount(totalAmount)
                .note(request.getNote())
                .build();
        tuitionPaymentRepository.save(payment);

        log.info("Created TuitionPayment [{}] for student [{}], total: {}",
                payment.getPaymentId(), student.getPersonId(), totalAmount);

        // 8. Phân bổ từng tháng vào TuitionPaymentDetail
        List<TuitionPaymentDetail> details = new ArrayList<>();
        for (int i = 0; i < request.getNumberOfMonths(); i++) {
            int targetMonth = ((startMonth - 1 + i) % 12) + 1;
            int targetYear = startYear + (startMonth - 1 + i) / 12;

            TuitionPaymentDetail detail = TuitionPaymentDetail.builder()
                    .tuitionPayment(payment)
                    .enrollment(enrollment)
                    .forMonth(targetMonth)
                    .forYear(targetYear)
                    .amountAllocated(monthlyFee)
                    .build();
            details.add(detail);

            log.debug("  → Phân bổ tháng {}/{}: {}đ", targetMonth, targetYear, monthlyFee);
        }
        tuitionPaymentDetailRepository.saveAll(details);

        log.info("Saved {} TuitionPaymentDetail records for payment [{}]",
                details.size(), payment.getPaymentId());

        // 9. Xây dựng response
        return tuitionPaymentMapper.toResponse(payment, details);
    }

    // =========================================================================
    // B. TÁC VỤ KIỂM TRA TRẠNG THÁI (CHECK STATUS) — Dùng cho AI Receptionist
    // =========================================================================

    /**
     * Kiểm tra trạng thái học phí của học viên cho tháng hiện tại.
     * Trả về kết quả cho từng lớp đang ACTIVE.
     * hasPaidCurrentMonth = true khi TẤT CẢ lớp đang học đều đã đóng phí.
     *
     * @param studentId UUID của học viên
     * @return TuitionStatusResponse
     */
    @Transactional(readOnly = true)
    public TuitionPaymentDetailDTO.TuitionStatusResponse checkTuitionStatus(UUID studentId) {

        Student student = studentService.getStudentById(studentId);

        // Lấy tất cả enrollment đang ACTIVE
        List<StudentEnrollment> activeEnrollments = studentEnrollmentRepository
                .findByStudent_PersonIdAndStatusWithClassSchedule(studentId, StudentEnrollmentStatus.ACTIVE);

        LocalDate now = LocalDate.now();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();

        if (activeEnrollments.isEmpty()) {
            return TuitionPaymentDetailDTO.TuitionStatusResponse.builder()
                    .studentId(student.getPersonId())
                    .studentCode(student.getStudentCode())
                    .fullName(student.getFullName())
                    .hasPaidCurrentMonth(false)
                    .currentMonth(currentMonth)
                    .currentYear(currentYear)
                    .activeClasses(List.of())
                    .build();
        }

        // Lấy enrollmentIds để query batch
        List<UUID> enrollmentIds = activeEnrollments.stream()
                .map(StudentEnrollment::getEnrollmentId)
                .collect(Collectors.toList());

//        // Batch query: lấy các detail đã đóng trong tháng này
//        List<TuitionPaymentDetail> paidDetails = tuitionPaymentDetailRepository
//                .findPaidEnrollmentsForMonth(enrollmentIds, currentMonth, currentYear);

        // Batch query: lấy các detail đã đóng trong tháng này
        List<TuitionPaymentDetail> paidDetails = tuitionPaymentDetailRepository
                .findPaidEnrollmentsForYear(enrollmentIds, currentYear);

        // Map enrollmentId → detail để tra cứu O(1)
        List<TuitionPaymentDetailDTO.ActiveClassStatus> classStatuses =
                tuitionPaymentMapper.toActiveClassStatuses(activeEnrollments, paidDetails);

        boolean allPaid = classStatuses.stream().allMatch(TuitionPaymentDetailDTO.ActiveClassStatus::isPaid);

        return TuitionPaymentDetailDTO.TuitionStatusResponse.builder()
                .studentId(student.getPersonId())
                .studentCode(student.getStudentCode())
                .fullName(student.getFullName())
                .hasPaidCurrentMonth(allPaid)
                .currentMonth(currentMonth)
                .currentYear(currentYear)
                .activeClasses(classStatuses)
                .build();
    }

    // =========================================================================
    // C. TÁC VỤ LỊCH SỬ ĐÓNG PHÍ (GET HISTORY)
    // =========================================================================

    /**
     * Lấy toàn bộ lịch sử đóng phí của học viên (tất cả các lớp).
     * Dùng cho App phụ huynh xem: "Tháng 3 (Đã đóng 12/03), Tháng 4 (Đã đóng 12/03)..."
     *
     * @param studentId UUID của học viên
     * @return Danh sách PaymentHistoryItem
     */
    @Transactional(readOnly = true)
    public List<TuitionPaymentDTO.PaymentHistoryItem> getPaymentHistory(UUID studentId) {
        // Xác nhận học viên tồn tại
        studentService.getStudentById(studentId);

        // Lấy tất cả detail kèm payment và classSchedule
        List<TuitionPaymentDetail> details = tuitionPaymentDetailRepository
                .findAllByStudentIdWithDetails(studentId);

        return details.stream()
                .map(tuitionPaymentMapper::toPaymentHistoryItem)
                .collect(Collectors.toList());
    }

    /**
     * Lấy lịch sử đóng phí cho 1 enrollment cụ thể.
     *
     * @param enrollmentId UUID của enrollment
     * @return Danh sách PaymentHistoryItem
     */
    @Transactional(readOnly = true)
    public List<TuitionPaymentDTO.PaymentHistoryItem> getPaymentHistoryByEnrollment(UUID enrollmentId) {
        StudentEnrollment enrollment = studentEnrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_FOUND));

        List<TuitionPaymentDetail> details = tuitionPaymentDetailRepository
                .findByEnrollmentIdWithPayment(enrollmentId);

        return details.stream()
                .map(detail -> tuitionPaymentMapper.toPaymentHistoryItem(detail, enrollment))
                .collect(Collectors.toList());
    }

    public PageResponse<TuitionPaymentDTO.TuitionPaymentResponse> getPaymentHistoryForAdmin(String search, Pageable pageable) {
        String safeSearch = (search == null) ? "" : search;

        // 1. Lấy danh sách Payment theo phân trang (Đã có sẵn Student nhờ EntityGraph)
        Page<TuitionPayment> tuitionPayments = tuitionPaymentRepository.findTuitionPaymentHistory(safeSearch, pageable);

        // 1.5. Nếu dữ liệu rỗng (không tìm thấy ai), map thẳng ra PageResponse rỗng
        if (tuitionPayments.isEmpty()) {
            return new PageResponse<>(
                    List.of(), // list rỗng
                    tuitionPayments.getNumber(),
                    tuitionPayments.getSize(),
                    tuitionPayments.getTotalElements(),
                    tuitionPayments.getTotalPages(),
                    tuitionPayments.isFirst(),
                    tuitionPayments.isLast(),
                    tuitionPayments.isEmpty()
            );
        }

        // 2. Lấy danh sách các Payment ID có trong trang hiện tại
        List<UUID> paymentIds = tuitionPayments.stream()
                .map(TuitionPayment::getPaymentId)
                .toList();

        // 3. Query 1 LẦN DUY NHẤT để lấy tất cả details của các payment này
        List<TuitionPaymentDetail> allDetails = tuitionPaymentDetailRepository.findByTuitionPayment_PaymentIdIn(paymentIds);

        // 4. Gom nhóm Details theo Payment ID (Map<PaymentId, List<Detail>>)
        Map<UUID, List<TuitionPaymentDetail>> detailsByPaymentId = allDetails.stream()
                .collect(Collectors.groupingBy(detail -> detail.getTuitionPayment().getPaymentId()));

        // 5. Map từ Entity sang Response DTO (Lúc này vẫn đang là Spring Page)
        Page<TuitionPaymentDTO.TuitionPaymentResponse> dtoPage = tuitionPayments.map(payment -> {
            // Lấy list detail tương ứng từ Map, nếu không có thì trả về list rỗng
            List<TuitionPaymentDetail> paymentDetails = detailsByPaymentId.getOrDefault(payment.getPaymentId(), List.of());
            return tuitionPaymentMapper.toResponse(payment, paymentDetails); // Lưu ý: sửa tuitionPaymentMap thành tuitionPaymentMapper
        });

        // 6. Convert từ Spring Page sang Custom PageResponse của bạn
        return new PageResponse<>(
                dtoPage.getContent(),
                dtoPage.getNumber(),
                dtoPage.getSize(),
                dtoPage.getTotalElements(),
                dtoPage.getTotalPages(),
                dtoPage.isFirst(),
                dtoPage.isLast(),
                dtoPage.isEmpty()
        );
    }
}
