package com.dat.backend_v2_1.enums.Operation;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CoachAssignmentStatus {

    /**
     * 🟢 ĐANG GIẢNG DẠY
     * HLV đang phụ trách lớp này bình thường.
     * Dữ liệu này sẽ được dùng để tính lương/chấm công hàng tháng.
     */
    ACTIVE("Đang giảng dạy"),

    /**
     * 🟡 TẠM NGƯNG / BẢO LƯU
     * HLV tạm thời không dạy lớp này trong một khoảng thời gian ngắn
     * (Ví dụ: Bị chấn thương, đi thi đấu, nghỉ phép, việc riêng).
     * Hệ thống sẽ không tính công trong thời gian này, nhưng chưa cắt hẳn assignment.
     */
    SUSPENDED("Tạm ngưng"),

    /**
     * 🔵 HOÀN THÀNH
     * Nhiệm vụ kết thúc một cách tự nhiên và tốt đẹp.
     * Ví dụ: Lớp học kết thúc khóa, hoặc HLV chuyển sang dạy lớp khác theo kế hoạch.
     */
    COMPLETED("Hoàn thành nhiệm vụ"),

    /**
     * 🔴 CHẤM DỨT / HỦY BỎ
     * Nhiệm vụ bị dừng đột ngột hoặc cưỡng chế.
     * Ví dụ: HLV bị kỷ luật, HLV tự ý bỏ lớp, hoặc lớp bị giải tán giữa chừng.
     */
    TERMINATED("Chấm dứt"),

    /**
     * ⚪ DỰ KIẾN (Optional)
     * Đã lên lịch phân công nhưng chưa đến ngày bắt đầu dạy.
     * Dùng cho việc Admin sắp xếp lịch giảng dạy cho tháng sau.
     */
    PENDING("Chờ nhận lớp");

    private final String description;
}