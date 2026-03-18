# 🗄️ BÁO CÁO THIẾT KẾ CƠ SỞ DỮ LIỆU

> **Dự án:** AI Receptionist – Backend API  
> **Ngày xuất:** 08/03/2026  
> **Phụ trách:** Database Architect / Senior Backend Developer  
> **Hệ quản trị CSDL:** PostgreSQL  
> **ORM:** Spring Data JPA / Hibernate 6

---

## 1. 🏛️ Tổng Quan Kiến Trúc Dữ Liệu

Hệ thống được tổ chức theo **3 PostgreSQL Schema** độc lập, phản ánh đúng kiến trúc phân lớp nghiệp vụ:

| Schema      | Số Bảng                  | Vai Trò                                                                                |
|-------------|--------------------------|----------------------------------------------------------------------------------------|
| `security`  | 3                        | Quản lý xác thực, phân quyền, phiên đăng nhập đa thiết bị                              |
| `core`      | 4                        | Dữ liệu nghiệp vụ cốt lõi: chi nhánh, người dùng chuyên biệt (HLV, học viên), lịch học |
| `operation` | 5 *(+2 chưa triển khai)* | Dữ liệu vận hành hàng ngày: đăng ký lớp, điểm danh, phân công, chấm công, lên đai      |

### Chiến lược kế thừa Entity (Inheritance Strategy)

```
security.user          ← Base Entity (JOINED Inheritance)
    ├── core.coach     ← PrimaryKeyJoinColumn(user_id)
    └── core.student   ← PrimaryKeyJoinColumn(user_id)
```

> Sử dụng **`InheritanceType.JOINED`**: Mỗi lớp con có bảng riêng, khóa chính đồng thời là FK trỏ về bảng cha
`security.user`. Chiến lược này tối ưu cho truy vấn đa hình và tránh dư thừa cột NULL.

---

## 2. 📋 Chi Tiết Cấu Trúc Các Bảng

### 🔐 Schema: `security`

---

#### Bảng: `security.role`

| Tên Cột       | Kiểu SQL       | PK | FK | Ràng Buộc        | Ghi Chú                                             |
|---------------|----------------|----|----|------------------|-----------------------------------------------------|
| `role_code`   | `VARCHAR(50)`  | ✅  |    | NOT NULL         | Format: `ROLE_ADMIN`, `ROLE_COACH`, `ROLE_STUDENT`… |
| `role_name`   | `VARCHAR(100)` |    |    | NOT NULL, UNIQUE | Tên hiển thị                                        |
| `description` | `VARCHAR(255)` |    |    | NULLABLE         | Mô tả vai trò                                       |

---

#### Bảng: `security.user`

| Tên Cột         | Kiểu SQL       | PK | FK                            | Ràng Buộc                    | Ghi Chú                                            |
|-----------------|----------------|----|-------------------------------|------------------------------|----------------------------------------------------|
| `user_id`       | `UUID`         | ✅  |                               | NOT NULL, NOT UPDATABLE      | Auto-generated bởi Hibernate UuidGenerator         |
| `national_code` | `VARCHAR(50)`  |    |                               | NULLABLE, UNIQUE             | Số CCCD/CMND                                       |
| `full_name`     | `VARCHAR(100)` |    |                               | NOT NULL                     |                                                    |
| `password_hash` | `TEXT`         |    |                               | NOT NULL                     | BCrypt hash                                        |
| `status`        | `VARCHAR(20)`  |    |                               | NOT NULL, DEFAULT `'ACTIVE'` | Enum: `ACTIVE`, `BANNED`, `PENDING`, `DEACTIVATED` |
| `phone_number`  | `VARCHAR(10)`  |    |                               | NOT NULL                     | Regex VN format, dùng làm username đăng nhập       |
| `birth_date`    | `DATE`         |    |                               | NOT NULL                     | Phải là ngày trong quá khứ                         |
| `belt`          | `VARCHAR(20)`  |    |                               | NOT NULL, DEFAULT `'C10'`    | Enum: `C10`→`C1`, `D1`→`D10` (20 cấp đai)          |
| `role_code`     | `VARCHAR(50)`  |    | ✅ → `security.role.role_code` | NOT NULL                     |                                                    |
| `created_at`    | `TIMESTAMPTZ`  |    |                               | NOT NULL, NOT UPDATABLE      | JPA Auditing `@CreatedDate`                        |
| `updated_at`    | `TIMESTAMPTZ`  |    |                               | NULLABLE                     | JPA Auditing `@LastModifiedDate`                   |
| `last_login_at` | `TIMESTAMPTZ`  |    |                               | NULLABLE                     | Cập nhật thủ công khi login                        |

---

#### Bảng: `security.auth_tokens`

| Tên Cột         | Kiểu SQL        | PK | FK                          | Ràng Buộc                 | Ghi Chú                        |
|-----------------|-----------------|----|-----------------------------|---------------------------|--------------------------------|
| `token_id`      | `UUID`          | ✅  |                             | NOT NULL, NOT UPDATABLE   | Auto-generated                 |
| `user_id`       | `UUID`          |    | ✅ → `security.user.user_id` | NOT NULL                  |                                |
| `refresh_token` | `VARCHAR(1024)` |    |                             | NOT NULL, UNIQUE          | JWT Refresh Token              |
| `device_info`   | `TEXT`          |    |                             | NULLABLE                  | Device ID / User-Agent         |
| `fcm_token`     | `VARCHAR(500)`  |    |                             | NULLABLE                  | Firebase Cloud Messaging token |
| `expires_at`    | `TIMESTAMPTZ`   |    |                             | NOT NULL                  | Thời hạn hết hạn token         |
| `revoked`       | `BOOLEAN`       |    |                             | NOT NULL, DEFAULT `false` | Token đã bị thu hồi hay chưa   |
| `created_at`    | `TIMESTAMPTZ`   |    |                             | NOT NULL                  | Set bởi `@PrePersist`          |

**Index:** `idx_refresh_token` trên cột `refresh_token`

---

### 🏢 Schema: `core`

---

#### Bảng: `core.branch`

| Tên Cột       | Kiểu SQL       | PK | FK | Ràng Buộc                       | Ghi Chú                                    |
|---------------|----------------|----|----|---------------------------------|--------------------------------------------|
| `branch_id`   | `BIGINT`       | ✅  |    | NOT NULL, AUTO_INCREMENT        | IDENTITY strategy                          |
| `branch_name` | `VARCHAR(100)` |    |    | NOT NULL                        |                                            |
| `address`     | `VARCHAR(255)` |    |    | NOT NULL                        |                                            |
| `hotline`     | `VARCHAR(20)`  |    |    | NULLABLE                        |                                            |
| `opened_date` | `DATE`         |    |    | NOT NULL                        | Phải là quá khứ hoặc hôm nay               |
| `status`      | `VARCHAR(20)`  |    |    | NOT NULL, DEFAULT `'OPERATING'` | Enum: `OPERATING`, `CLOSED`, `MAINTENANCE` |
| `created_at`  | `TIMESTAMP`    |    |    | NOT NULL, NOT UPDATABLE         | JPA Auditing                               |
| `updated_at`  | `TIMESTAMP`    |    |    | NULLABLE                        | JPA Auditing                               |

---

#### Bảng: `core.coach`

> Kế thừa từ `security.user` qua `InheritanceType.JOINED`. Bảng này chỉ chứa cột riêng của Coach.

| Tên Cột        | Kiểu SQL      | PK / FK                           | Ràng Buộc                    | Ghi Chú                                            |
|----------------|---------------|-----------------------------------|------------------------------|----------------------------------------------------|
| `user_id`      | `UUID`        | PK + FK → `security.user.user_id` | NOT NULL                     | Khóa chính đồng thời là khóa ngoại                 |
| `staff_code`   | `VARCHAR(20)` |                                   | NOT NULL, UNIQUE             | Mã nhân viên                                       |
| `coach_status` | `VARCHAR(20)` |                                   | NOT NULL, DEFAULT `'ACTIVE'` | Enum: `ACTIVE`, `INACTIVE`, `SUSPENDED`, `RETIRED` |

---

#### Bảng: `core.student`

> Kế thừa từ `security.user` qua `InheritanceType.JOINED`. Bảng này chỉ chứa cột riêng của Student.

| Tên Cột          | Kiểu SQL      | PK / FK                           | Ràng Buộc                    | Ghi Chú                                   |
|------------------|---------------|-----------------------------------|------------------------------|-------------------------------------------|
| `user_id`        | `UUID`        | PK + FK → `security.user.user_id` | NOT NULL                     | Khóa chính đồng thời là khóa ngoại        |
| `student_code`   | `VARCHAR(50)` |                                   | NOT NULL, UNIQUE             | Mã học viên, auto-generated               |
| `start_date`     | `DATE`        |                                   | NOT NULL, DEFAULT `NOW()`    | Ngày bắt đầu tập, phải là quá khứ/hôm nay |
| `student_status` | `VARCHAR(20)` |                                   | NOT NULL, DEFAULT `'ACTIVE'` | Enum: `ACTIVE`, `RESERVED`, `DROPPED`     |
| `branch_id`      | `BIGINT`      | FK → `core.branch.branch_id`      | NOT NULL                     | Chi nhánh học viên đang theo học          |

**Index:**

- `idx_student_status` trên `student_status`
- `idx_student_branch` trên `branch_id`

---

#### Bảng: `core.class_schedule`

| Tên Cột           | Kiểu SQL      | PK | FK                          | Ràng Buộc | Ghi Chú                                                           |
|-------------------|---------------|----|-----------------------------|-----------|-------------------------------------------------------------------|
| `schedule_id`     | `VARCHAR(5)`  | ✅  |                             | NOT NULL  | Mã lịch học thủ công (VD: `A101`)                                 |
| `branch_id`       | `BIGINT`      |    | ✅ → `core.branch.branch_id` | NOT NULL  |                                                                   |
| `weekday`         | `INTEGER`     |    |                             | NOT NULL  | Lưu số nguyên (1=CN, 2=Hai…7=Bảy) qua `WeekdayConverter`          |
| `level`           | `VARCHAR(20)` |    |                             | NOT NULL  | Enum: `BASIC`, `KID`, `ADULT`, `DAN`, `SPARRING_TEAM_TIER_1`…     |
| `start_time`      | `TIME`        |    |                             | NOT NULL  | Giờ bắt đầu                                                       |
| `end_time`        | `TIME`        |    |                             | NOT NULL  | Giờ kết thúc; validated `end_time > start_time` qua `@PrePersist` |
| `shift`           | `VARCHAR(20)` |    |                             | NOT NULL  | Enum: `CA_1`, `CA_2`                                              |
| `location`        | `VARCHAR(50)` |    |                             | NOT NULL  | Enum: `INDOOR`, `OUTDOOR`, `ONLINE`                               |
| `schedule_status` | `VARCHAR(20)` |    |                             | NOT NULL  | Enum: `ACTIVE`, `INACTIVE`                                        |

---

### ⚙️ Schema: `operation`

---

#### Bảng: `operation.student_enrollment`

| Tên Cột             | Kiểu SQL       | PK | FK                                    | Ràng Buộc                    | Ghi Chú                                              |
|---------------------|----------------|----|---------------------------------------|------------------------------|------------------------------------------------------|
| `enrollment_id`     | `UUID`         | ✅  |                                       | NOT NULL, NOT UPDATABLE      | Auto-generated                                       |
| `student_user_id`   | `UUID`         |    | ✅ → `core.student.user_id`            | NOT NULL                     |                                                      |
| `schedule_id`       | `VARCHAR(5)`   |    | ✅ → `core.class_schedule.schedule_id` | NOT NULL                     |                                                      |
| `join_date`         | `DATE`         |    |                                       | NOT NULL                     | Ngày nhập học                                        |
| `end_date`          | `DATE`         |    |                                       | NULLABLE                     | Ngày kết thúc (null nếu đang học)                    |
| `enrollment_status` | `VARCHAR(20)`  |    |                                       | NOT NULL, DEFAULT `'ACTIVE'` | Enum: `ACTIVE`, `RESERVED`, `TRANSFERRED`, `DROPPED` |
| `note`              | `VARCHAR(500)` |    |                                       | NULLABLE                     | Lý do chuyển/bảo lưu/nghỉ                            |
| `created_at`        | `TIMESTAMPTZ`  |    |                                       | NOT NULL, NOT UPDATABLE      | JPA Auditing                                         |
| `updated_at`        | `TIMESTAMPTZ`  |    |                                       | NULLABLE                     | JPA Auditing                                         |

**Index:**

- `idx_enrollment_student` trên `student_user_id`
- `idx_enrollment_schedule` trên `schedule_id`
- `idx_enrollment_status` trên `enrollment_status`

---

#### Bảng: `operation.student_attendance`

| Tên Cột                 | Kiểu SQL       | PK | FK                                               | Ràng Buộc                    | Ghi Chú                                                |
|-------------------------|----------------|----|--------------------------------------------------|------------------------------|--------------------------------------------------------|
| `attendance_id`         | `UUID`         | ✅  |                                                  | NOT NULL, NOT UPDATABLE      | Auto-generated                                         |
| `student_enrollment_id` | `UUID`         |    | ✅ → `operation.student_enrollment.enrollment_id` | NULLABLE                     | Null khi điểm danh thử                                 |
| `session_date`          | `DATE`         |    |                                                  | NOT NULL                     | Ngày diễn ra buổi học                                  |
| `attendance_status`     | `VARCHAR(20)`  |    |                                                  | NOT NULL, DEFAULT `'ABSENT'` | Enum: `PRESENT`, `ABSENT`, `EXCUSED`, `MAKEUP`, `LATE` |
| `check_in_time`         | `TIMESTAMPTZ`  |    |                                                  | NULLABLE                     | Thời điểm thực tế check-in                             |
| `attendance_coach_id`   | `UUID`         |    | ✅ → `core.coach.user_id`                         | NULLABLE                     | HLV thực hiện điểm danh                                |
| `evaluation_status`     | `VARCHAR(20)`  |    |                                                  | NULLABLE                     | Enum: `PENDING`, `GOOD`, `AVERAGE`, `WEAK`             |
| `evaluation_coach_id`   | `UUID`         |    | ✅ → `core.coach.user_id`                         | NULLABLE                     | HLV đánh giá (có thể khác người điểm danh)             |
| `note`                  | `VARCHAR(500)` |    |                                                  | NULLABLE                     |                                                        |
| `created_at`            | `TIMESTAMPTZ`  |    |                                                  | NOT NULL, NOT UPDATABLE      | JPA Auditing                                           |
| `updated_at`            | `TIMESTAMPTZ`  |    |                                                  | NULLABLE                     | JPA Auditing                                           |

**Unique Constraint:** `uk_student_enrollment_date (student_enrollment_id, session_date)` — Một học viên chỉ có một bản
ghi điểm danh cho mỗi buổi học.

**Index:**

- `idx_student_enrollment` trên `student_enrollment_id`
- `idx_session_date DESC` trên `session_date`

**Named Entity Graph:** `StudentAttendance.withDetails` — Fetch eagerly
`studentEnrollment → (student, classSchedule → branch)`, `recordedByCoach`, `evaluatedByCoach`.

---

#### Bảng: `operation.coach_assignment`

| Tên Cột             | Kiểu SQL      | PK | FK                                    | Ràng Buộc                    | Ghi Chú                                              |
|---------------------|---------------|----|---------------------------------------|------------------------------|------------------------------------------------------|
| `assignment_id`     | `UUID`        | ✅  |                                       | NOT NULL, NOT UPDATABLE      | Auto-generated                                       |
| `coach_user_id`     | `UUID`        |    | ✅ → `core.coach.user_id`              | NOT NULL                     |                                                      |
| `schedule_id`       | `VARCHAR(5)`  |    | ✅ → `core.class_schedule.schedule_id` | NOT NULL                     |                                                      |
| `assigned_date`     | `DATE`        |    |                                       | NOT NULL                     | Ngày bắt đầu nhận lớp                                |
| `end_date`          | `DATE`        |    |                                       | NULLABLE                     | Ngày kết thúc (null nếu đang dạy)                    |
| `assignment_status` | `VARCHAR(20)` |    |                                       | NOT NULL, DEFAULT `'ACTIVE'` | Enum: `ACTIVE`, `PENDING`, `COMPLETED`, `TERMINATED` |
| `note`              | `TEXT`        |    |                                       | NULLABLE                     |                                                      |
| `created_at`        | `TIMESTAMPTZ` |    |                                       | NOT NULL, NOT UPDATABLE      | JPA Auditing                                         |
| `updated_at`        | `TIMESTAMPTZ` |    |                                       | NULLABLE                     | JPA Auditing                                         |

**Index:**

- `idx_assignment_coach` trên `coach_user_id`
- `idx_assignment_schedule` trên `schedule_id`

---

#### Bảng: `operation.coach_timesheet`

| Tên Cột         | Kiểu SQL       | PK | FK                                    | Ràng Buộc                     | Ghi Chú                                 |
|-----------------|----------------|----|---------------------------------------|-------------------------------|-----------------------------------------|
| `timesheet_id`  | `UUID`         | ✅  |                                       | NOT NULL, NOT UPDATABLE       | Auto-generated                          |
| `coach_user_id` | `UUID`         |    | ✅ → `core.coach.user_id`              | NOT NULL                      |                                         |
| `schedule_id`   | `VARCHAR(5)`   |    | ✅ → `core.class_schedule.schedule_id` | NOT NULL                      |                                         |
| `working_date`  | `DATE`         |    |                                       | NOT NULL                      | Ngày làm việc, phải là quá khứ/hôm nay  |
| `check_in_time` | `TIMESTAMPTZ`  |    |                                       | NULLABLE                      | Thời gian HLV bấm Check-in              |
| `status`        | `VARCHAR(20)`  |    |                                       | NOT NULL, DEFAULT `'PENDING'` | Enum: `PENDING`, `APPROVED`, `REJECTED` |
| `note`          | `VARCHAR(500)` |    |                                       | NULLABLE                      |                                         |
| `created_at`    | `TIMESTAMPTZ`  |    |                                       | NOT NULL, NOT UPDATABLE       | JPA Auditing                            |
| `updated_at`    | `TIMESTAMPTZ`  |    |                                       | NULLABLE                      | JPA Auditing                            |

**Unique Constraint:** `uk_coach_schedule_date (coach_user_id, schedule_id, working_date)` — Một HLV chỉ có một bản ghi
chấm công cho mỗi ca dạy.

---

#### Bảng: `operation.belt_promotion`

| Tên Cột        | Kiểu SQL       | PK | FK                         | Ràng Buộc                     | Ghi Chú                             |
|----------------|----------------|----|----------------------------|-------------------------------|-------------------------------------|
| `promotion_id` | `UUID`         | ✅  |                            | NOT NULL, NOT UPDATABLE       | Auto-generated                      |
| `student_id`   | `UUID`         |    | ✅ → `core.student.user_id` | NOT NULL                      |                                     |
| `exam_date`    | `DATE`         |    |                            | NOT NULL                      | Phải là quá khứ/hôm nay             |
| `current_belt` | `VARCHAR(20)`  |    |                            | NOT NULL                      | Đai hiện tại trước khi thi          |
| `target_belt`  | `VARCHAR(20)`  |    |                            | NOT NULL                      | Đai mục tiêu muốn đạt               |
| `result`       | `VARCHAR(20)`  |    |                            | NOT NULL, DEFAULT `'PENDING'` | Enum: `PASSED`, `FAILED`, `PENDING` |
| `note`         | `VARCHAR(500)` |    |                            | NULLABLE                      | Nhận xét của HLV                    |
| `created_at`   | `TIMESTAMPTZ`  |    |                            | NOT NULL, NOT UPDATABLE       | JPA Auditing                        |
| `updated_at`   | `TIMESTAMPTZ`  |    |                            | NULLABLE                      | JPA Auditing                        |

**Validation:** `@AssertTrue` tại tầng Java đảm bảo `target_belt ≠ current_belt`.

**Index:**

- `idx_promotion_student` trên `student_id`
- `idx_promotion_exam_date` trên `exam_date`

---

#### Bảng: `TrialRegistration` & `TrialAttendance` *(Chưa triển khai)*

> Hai entity này đang ở trạng thái **commented-out** trong source code. Schema và cấu trúc cột chưa được định nghĩa. Đây
> là điểm cần phát triển cho giai đoạn tiếp theo (quản lý học thử).

---

## 3. 🔗 Sơ Đồ Mối Quan Hệ (Entity Relationships)

### Toàn cảnh quan hệ

```
security.role ──(1:N)──► security.user
security.user ──(1:N)──► security.auth_tokens
security.user ──(1:1, JOINED)──► core.coach
security.user ──(1:1, JOINED)──► core.student
core.branch ──(1:N)──► core.student
core.branch ──(1:N)──► core.class_schedule
core.student ──(1:N)──► operation.student_enrollment
core.class_schedule ──(1:N)──► operation.student_enrollment
operation.student_enrollment ──(1:N)──► operation.student_attendance
core.coach ──(1:N)──► operation.student_attendance  [ghi điểm danh]
core.coach ──(1:N)──► operation.student_attendance  [đánh giá]
core.coach ──(1:N)──► operation.coach_assignment
core.class_schedule ──(1:N)──► operation.coach_assignment
core.coach ──(1:N)──► operation.coach_timesheet
core.class_schedule ──(1:N)──► operation.coach_timesheet
core.student ──(1:N)──► operation.belt_promotion
```

---

### Chi tiết từng quan hệ

| Từ Bảng                        | Kiểu Quan Hệ | Đến Bảng                       | Thông Qua Cột                              | Ghi Chú                                   |
|--------------------------------|--------------|--------------------------------|--------------------------------------------|-------------------------------------------|
| `security.role`                | **1 - N**    | `security.user`                | `user.role_code`                           | Một role có nhiều user                    |
| `security.user`                | **1 - N**    | `security.auth_tokens`         | `auth_tokens.user_id`                      | Một user có nhiều token (đa thiết bị)     |
| `security.user`                | **1 - 1**    | `core.coach`                   | `coach.user_id` (PK+FK)                    | Inheritance JOINED                        |
| `security.user`                | **1 - 1**    | `core.student`                 | `student.user_id` (PK+FK)                  | Inheritance JOINED                        |
| `core.branch`                  | **1 - N**    | `core.student`                 | `student.branch_id`                        | Một chi nhánh có nhiều học viên           |
| `core.branch`                  | **1 - N**    | `core.class_schedule`          | `class_schedule.branch_id`                 | Một chi nhánh có nhiều lịch học           |
| `core.student`                 | **1 - N**    | `operation.student_enrollment` | `student_enrollment.student_user_id`       | Một học viên đăng ký nhiều lớp            |
| `core.class_schedule`          | **1 - N**    | `operation.student_enrollment` | `student_enrollment.schedule_id`           | Một lịch học có nhiều học viên đăng ký    |
| `operation.student_enrollment` | **1 - N**    | `operation.student_attendance` | `student_attendance.student_enrollment_id` | Một enrollment có nhiều bản ghi điểm danh |
| `core.coach`                   | **1 - N**    | `operation.student_attendance` | `student_attendance.attendance_coach_id`   | HLV ghi nhận điểm danh                    |
| `core.coach`                   | **1 - N**    | `operation.student_attendance` | `student_attendance.evaluation_coach_id`   | HLV đánh giá buổi học                     |
| `core.coach`                   | **1 - N**    | `operation.coach_assignment`   | `coach_assignment.coach_user_id`           | Một HLV được phân công nhiều lớp          |
| `core.class_schedule`          | **1 - N**    | `operation.coach_assignment`   | `coach_assignment.schedule_id`             | Một lịch học được phân công nhiều HLV     |
| `core.coach`                   | **1 - N**    | `operation.coach_timesheet`    | `coach_timesheet.coach_user_id`            | Một HLV có nhiều bản ghi chấm công        |
| `core.class_schedule`          | **1 - N**    | `operation.coach_timesheet`    | `coach_timesheet.schedule_id`              | Một lịch học có nhiều bản chấm công       |
| `core.student`                 | **1 - N**    | `operation.belt_promotion`     | `belt_promotion.student_id`                | Một học viên có nhiều kỳ thi lên đai      |

---

## 4. 💡 Đề Xuất & Tối Ưu

### 🔴 Vấn đề cần xử lý (Critical)

#### 1. `student_attendance.student_enrollment_id` cho phép NULL

```java
// StudentAttendance.java
@ManyToOne
@JoinColumn(name = "student_enrollment_id", nullable = true) // ⚠️
        StudentEnrollment studentEnrollment;
```

**Vấn đề:** Cột này `nullable = true` nhưng `FetchType` mặc định là `EAGER` (không khai báo LAZY). Khi enrollment bị
null, bản ghi điểm danh mất đi ngữ cảnh lớp học. Nếu đây là cho học thử thì cần có bảng `TrialAttendance` riêng, không
nên dùng chung bảng.

**Đề xuất:** Tách điểm danh thử thành entity riêng (đang comment-out), hoặc thêm cột `schedule_id` trực tiếp vào
`student_attendance` làm fallback khi `enrollment_id` null + thêm `CHECK constraint`.

---

#### 2. Thiếu Unique Constraint trên `coach_assignment`

```java
// CoachAssignment.java — Unique constraint đang bị comment-out!
//        uniqueConstraints = {
//                @UniqueConstraint(columnNames = {"coach_user_id", "schedule_id"})
//        }
```

**Vấn đề:** Không có gì ngăn cùng một HLV được phân công vào một lớp hai lần. Logic kiểm tra trùng đang nằm ở tầng
Service (dễ bị bỏ qua khi thêm endpoint mới).

**Đề xuất:** Bật lại unique constraint, hoặc dùng composite unique `(coach_user_id, schedule_id, assignment_status)` nếu
muốn cho phép lịch sử phân công.

---

### 🟡 Lưu ý về Hiệu năng (Performance)

#### 3. Thiếu Index trên `security.user` cho trường đăng nhập

```sql
-- phone_number là trường dùng để đăng nhập nhưng không có index
SELECT *
FROM security.user
WHERE phone_number = ?
```

**Đề xuất:** Thêm index:

```java
@Table(name = "user", schema = "security",
        indexes = {
                @Index(name = "idx_user_phone", columnList = "phone_number"),
                @Index(name = "idx_user_status", columnList = "status")
        }
)
```

---

#### 4. Thiếu Index trên `security.auth_tokens` cho `user_id` và `revoked`

Truy vấn phổ biến: "Tìm tất cả token còn hiệu lực của user X" — hiện không có index hỗ trợ.

```java
// Đề xuất thêm composite index
@Index(name = "idx_auth_token_user_revoked", columnList = "user_id, revoked")
```

---

#### 5. `coach_assignment` — Cột `note` không giới hạn độ dài

```java

@Column(name = "note") // Không khai báo length → TEXT không giới hạn
String note;
```

**Đề xuất:** Thống nhất với các entity khác, thêm `length = 500`.

---

#### 6. Kiểu dữ liệu `LocalDateTime` vs `LocalDateTime` không nhất quán

- `security.user`, `operation.*`: Dùng `LocalDateTime` (UTC) → **đúng** cho dữ liệu phân tán.
- `core.branch`: Dùng `LocalDateTime` → có thể gây **mơ hồ timezone** khi query cross-region.

**Đề xuất:** Chuẩn hoá toàn bộ về `LocalDateTime` (UTC) và cấu hình
`spring.jpa.properties.hibernate.jdbc.time_zone=UTC` (đã
có `Asia/Ho_Chi_Minh` trong config nhưng cần nhất quán).

---

### 🟢 Đề Xuất Cải Tiến (Enhancement)

#### 7. Thiếu BaseEntity / AuditEntity chung

Hiện tại, các trường `created_at` / `updated_at` được lặp lại ở **mọi entity**.

**Đề xuất:** Tạo `BaseEntity` với `@MappedSuperclass`:

```java

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

---

#### 8. `belt_promotion` nên thêm FK đến `coach` (người chủ trì kỳ thi)

Hiện tại bảng `belt_promotion` chỉ lưu kết quả của học viên nhưng không ghi nhận HLV nào đã chủ trì kỳ thi, ảnh hưởng
đến tính trách nhiệm (accountability).

**Đề xuất:** Thêm:

```java

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "examiner_coach_id")
Coach examiner;
```

---

#### 9. `student_enrollment` thiếu Unique Constraint chống đăng ký trùng lớp đang ACTIVE

Hiện tại kiểm tra trùng lặp chỉ ở tầng Service. Nếu có bug hoặc concurrent request, DB vẫn có thể chứa duplicate.

**Đề xuất:** Thêm partial unique index tại DB (chỉ cho trạng thái ACTIVE):

```sql
CREATE UNIQUE INDEX uk_enrollment_active
    ON operation.student_enrollment (student_user_id, schedule_id) WHERE enrollment_status = 'ACTIVE';
```

---

#### 10. `class_schedule` — `schedule_id` dạng `VARCHAR(5)` dễ conflict nếu scale

Hiện tại ID lịch học là chuỗi thủ công (`VARCHAR(5)`, VD: `A101`). Khi mở rộng nhiều chi nhánh, dễ trùng.

**Đề xuất:** Giữ `schedule_id` làm "mã hiển thị" (business code), thêm `branch_id` vào composite unique:

```java
@UniqueConstraint(columnNames = {"schedule_id", "branch_id"})
```

Hoặc chuyển PK sang UUID và giữ `schedule_id` là mã nhận diện nghiệp vụ riêng.

---

## 📊 Tổng Kết

| Hạng Mục              | Chi Tiết                                                                                                                   |
|-----------------------|----------------------------------------------------------------------------------------------------------------------------|
| Tổng số Schema        | 3 (`security`, `core`, `operation`)                                                                                        |
| Tổng số Bảng (active) | 10                                                                                                                         |
| Bảng chưa triển khai  | 2 (`trial_registration`, `trial_attendance`)                                                                               |
| Tổng số FK quan hệ    | 16                                                                                                                         |
| Unique Constraint     | 3 (`auth_tokens.refresh_token`, `student_attendance.uk_student_enrollment_date`, `coach_timesheet.uk_coach_schedule_date`) |
| Index tường minh      | 10                                                                                                                         |
| Chiến lược kế thừa    | `JOINED` cho `User → Coach / Student`                                                                                      |
| UUID Generator        | Hibernate `@UuidGenerator` (phiên bản 6+)                                                                                  |
| Audit Mechanism       | Spring Data JPA `@CreatedDate` / `@LastModifiedDate`                                                                       |

---

*Generated: 08/03/2026*

