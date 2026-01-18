package com.dat.backend_v2_1.domain.Core;

import com.dat.backend_v2_1.enums.Core.BranchStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
// Sử dụng AuditingEntityListener để tự động cập nhật ngày tạo/sửa
@EntityListeners(AuditingEntityListener.class)
@Table(name = "branch", schema = "core") // Tên bảng thường để số ít hoặc số nhiều thống nhất (VD: branches)
@FieldDefaults(level = AccessLevel.PRIVATE) // Mặc định các field là private
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Tự động tăng ID (Auto Increment)
    @Column(name = "branch_id")
    Long branchId; // Nên dùng Long thay vì int để tránh tràn số trong tương lai

    @NotBlank(message = "Tên chi nhánh không được để trống")
    @Size(max = 100, message = "Tên chi nhánh không quá 100 ký tự")
    @Column(name = "branch_name", nullable = false, length = 100)
    String branchName;

    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(max = 255)
    @Column(name = "address", nullable = false)
    String address;

    @Size(max = 20, message = "Hotline không quá 20 ký tự")
    @Column(name = "hotline", length = 20)
    String hotline;

    @NotNull(message = "Ngày khai trương không được để trống")
    @PastOrPresent(message = "Ngày khai trương phải trong quá khứ hoặc hôm nay")
    @Column(name = "opened_date", nullable = false)
    LocalDate openedDate; // Đổi tên biến thành camelCase

    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    BranchStatus status = BranchStatus.OPERATING;

    // --- Audit Fields (Nên tách ra BaseEntity nếu dùng cho nhiều bảng) ---

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}
