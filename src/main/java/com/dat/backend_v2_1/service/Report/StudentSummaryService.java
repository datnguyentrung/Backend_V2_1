package com.dat.backend_v2_1.service.Report;

import com.dat.backend_v2_1.domain.Core.Student;
import com.dat.backend_v2_1.domain.Operation.StudentAttendance;
import com.dat.backend_v2_1.dto.Operation.StudentAttendanceDTO;
import com.dat.backend_v2_1.dto.Report.YearlySummaryDTO;
import com.dat.backend_v2_1.enums.Report.ExamEligibility;
import com.dat.backend_v2_1.repository.Operation.StudentAttendanceRepository;
import com.dat.backend_v2_1.specification.StudentAttendanceSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dat.backend_v2_1.util.ScoreCalculator.calculateAttendanceScore;
import static com.dat.backend_v2_1.util.ScoreCalculator.calculatePerformanceScore;

@Service
@Slf4j
@RequiredArgsConstructor
public class StudentSummaryService {
    private final StudentAttendanceRepository studentAttendanceRepository;

    /**
     * Hàm tính điểm hàng loạt (Batch Processing) cho Bảng xếp hạng.
     * Xử lý N học viên nhưng chỉ dùng đúng 1 câu lệnh SQL.
     */
    public Map<String, YearlySummaryDTO.QuarterSummary> calculateBatchQuarterSummary(
            List<Student> activeStudents, int year, int quarter) {

        LocalDate startDate = LocalDate.of(year, (quarter - 1) * 3 + 1, 1);
        LocalDate endDate = YearMonth.from(startDate.plusMonths(2)).atEndOfMonth();

        // CHỌC SQL 1 LẦN DUY NHẤT ĐỂ LẤY STATS CỦA TẤT CẢ HỌC VIÊN!
        Map<String, StudentAttendanceDTO.AttendanceStats> statsMap =
                studentAttendanceRepository.getStatisticsGroupedByStudent(startDate, endDate);

        Map<String, YearlySummaryDTO.QuarterSummary> resultMap = new HashMap<>();

        for (Student student : activeStudents) {
            // Nếu học viên không có điểm danh (statsMap trả về null), ta tự tạo 1 object 0 records
            StudentAttendanceDTO.AttendanceStats stats = statsMap.getOrDefault(
                    student.getStudentCode(),
                    StudentAttendanceDTO.AttendanceStats.builder().totalRecords(0).build()
            );

            double attScore = calculateAttendanceScore(stats);
            double perfScore = calculatePerformanceScore(stats);

            boolean isPending = !LocalDate.now().isAfter(endDate) || stats.getEvalPendingCount() > 0;

            // TODO: Chỗ này bạn có thể truyền cờ isFullWeekStudent sau
            boolean isFullWeekStudent = false;

            YearlySummaryDTO.QuarterSummary summary = YearlySummaryDTO.QuarterSummary.builder()
                    .quarterNumber(quarter)
                    .attendanceStats(stats)
                    .attendanceScore(attScore)
                    .performanceScore(perfScore)
                    .totalQuarterScore(attScore + perfScore)
                    .eligibility(determineEligibility(stats, attScore, perfScore, isFullWeekStudent, isPending))
                    .build();

            resultMap.put(student.getStudentCode(), summary);
        }

        return resultMap;
    }

    public YearlySummaryDTO.YearlySummaryResponse getYearlySummary(String studentCode, int year) {
        List<YearlySummaryDTO.QuarterSummary> quarterSummaries = new ArrayList<>();


        for (int i = 1; i <= 4; i++) {
            quarterSummaries.add(getQuarterSummary(studentCode, year, i));
        }

        return YearlySummaryDTO.YearlySummaryResponse.builder()
                .year(year)
                .quarters(quarterSummaries)
                .build();
    }

    public YearlySummaryDTO.QuarterSummary getQuarterSummary(String studentCode, int year, int quarter) {
        // LƯU Ý: Bạn cần inject StudentRepository vào đây để check xem học viên
        // có phải là học viên "tập cả tuần" hay không. Tạm thời tôi gán biến ở đây.
        boolean isFullWeekStudent = false; // TODO: = studentRepository.findById(studentId).get().isFullWeek();

        LocalDate startDate = LocalDate.of(year, (quarter - 1) * 3 + 1, 1);
        LocalDate endDate = YearMonth.from(startDate.plusMonths(2)).atEndOfMonth();

        Specification<StudentAttendance> spec = StudentAttendanceSpecification.filterBy(
                studentCode,
                null, null, null, null, null, null, null,
                startDate,
                endDate
        );

        StudentAttendanceDTO.AttendanceStats stats = studentAttendanceRepository.getStatistics(spec);
        double attScore = calculateAttendanceScore(stats);
        double perfScore = calculatePerformanceScore(stats);

        boolean isPending = !LocalDate.now().isAfter(endDate)
                || stats.getEvalPendingCount() == 0;

        return YearlySummaryDTO.QuarterSummary.builder()
                .quarterNumber(quarter)
                .attendanceStats(stats)
                .attendanceScore(attScore)
                .performanceScore(perfScore)
                .totalQuarterScore(attScore + perfScore)
                .eligibility(determineEligibility(stats, attScore, perfScore, isFullWeekStudent, isPending)) // Tạm thời chưa check được có phải học viên tập cả tuần hay không
                .build();
    }

    /**
     * Hàm xét điều kiện thi thử dựa theo bộ Rules nghiệp vụ của trung tâm
     */
    private ExamEligibility determineEligibility(
            StudentAttendanceDTO.AttendanceStats stats,
            double attendanceScore,
            double performanceScore,
            boolean isFullWeekStudent,
            boolean isPending
    ) {
        // CHECK CỨNG: Nếu không có buổi học nào, trả về NONE ngay lập tức
        if (stats.getTotalRecords() == 0) {
            return ExamEligibility.NONE;
        }

        // 0. XỬ LÝ THEO THỜI GIAN (Dành cho những bạn chưa đủ điểm)
        // Nếu hôm nay vẫn trong quý (hoặc trước ngày kết thúc) -> Cho trạng thái chờ để cố gắng
        if (isPending) {
            return ExamEligibility.PENDING;
        }

        // 1. KIỂM TRA VI PHẠM "CỨNG" (Hard Fail)
        if (!isFullWeekStudent &&
                (stats.getAbsentCount() > 2 ||
                        attendanceScore < 0 ||
                        stats.getEvalWeakCount() > 3)) {
            return ExamEligibility.NOT_ELIGIBLE;
        }

        // 2. KIỂM TRA ĐIỀU KIỆN MIỄN THI (Ưu tiên số 1)
        long totalAttended = stats.getPresentCount() + stats.getLateCount() + stats.getMakeupCount();
        if (totalAttended >= 16 && attendanceScore == 5.0 && performanceScore >= 85.0) {
            return ExamEligibility.EXEMPTED;
        }

        // 3. KIỂM TRA ĐIỀU KIỆN ĐỦ ĐỂ THI (Đạt mốc tối thiểu)
        if (stats.getEvalGoodCount() >= 8) {
            return ExamEligibility.ELIGIBLE;
        }

        // Nếu đã qua ngày kết thúc quý mà vẫn rơi xuống đây (tức là chưa đủ 8 buổi Tốt)
        // -> Chốt là KHÔNG ĐỦ ĐIỀU KIỆN
        return ExamEligibility.NOT_ELIGIBLE;
    }
}