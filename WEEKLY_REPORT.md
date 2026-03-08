# 📋 BÁO CÁO TIẾN ĐỘ BACKEND - TUẦN 08/03/2026

> **Dự án:** AI Receptionist – Backend API (Java Spring Boot)
> **Ngày báo cáo:** 08/03/2026
> **Phụ trách:** Backend Developer
> **Công nghệ:** Java 21 · Spring Boot · PostgreSQL · Redis · RabbitMQ · Firebase FCM

---

## 1. 🚀 Tính Năng Mới & API Đã Hoàn Thiện

### 🔐 Security Module – Xác thực & Phân quyền

| API Endpoint                       | Method | Mô tả                                                                                                  |
|------------------------------------|--------|--------------------------------------------------------------------------------------------------------|
| `/api/v1/auth/login`               | POST   | Đăng nhập bằng số điện thoại + mật khẩu, trả về Access Token & Refresh Token (lưu vào HttpOnly Cookie) |
| `/api/v1/auth/logout`              | POST   | Đăng xuất, vô hiệu hoá Refresh Token                                                                   |
| `/api/v1/auth/refresh`             | GET    | Làm mới Access Token từ Refresh Token trong cookie                                                     |
| `/api/v1/users/me`                 | GET    | Lấy thông tin profile người dùng hiện tại (phân nhánh theo role: STUDENT / COACH)                      |
| `/api/v1/users/me/change-password` | POST   | Đổi mật khẩu (có kiểm tra mật khẩu cũ, policy thời hạn đổi mật khẩu)                                   |

**Services xây dựng:**

- `UserService` – Quản lý tài khoản người dùng, xác thực, đổi mật khẩu.
- `AuthTokenService` – Quản lý Refresh Token đa thiết bị (lưu FCM token theo device ID).
- `UserDetailCustom` – Custom `UserDetailsService` tích hợp Spring Security.

---

### 👥 Core Module – Quản lý Dữ liệu Nghiệp Vụ Cốt Lõi

#### Coach (Huấn luyện viên)

| API Endpoint                         | Method | Mô tả                                    |
|--------------------------------------|--------|------------------------------------------|
| `/api/v1/coaches`                    | POST   | Tạo mới HLV (kèm tạo tài khoản user)     |
| `/api/v1/coaches`                    | GET    | Lấy danh sách toàn bộ HLV                |
| `/api/v1/coaches/{userId}`           | GET    | Lấy chi tiết thông tin HLV               |
| `/api/v1/coaches/{userId}`           | PUT    | Cập nhật thông tin HLV                   |
| `/api/v1/coaches/{userId}`           | DELETE | Xoá mềm (Soft Delete) HLV                |
| `/api/v1/coaches/{userId}/permanent` | DELETE | Xoá vĩnh viễn (Hard Delete) – Admin Only |

#### Student (Học viên)

| API Endpoint                | Method | Mô tả                                                                                      |
|-----------------------------|--------|--------------------------------------------------------------------------------------------|
| `/api/v1/students`          | POST   | Tạo mới học viên (auto-generate `studentCode`)                                             |
| `/api/v1/students`          | GET    | Lấy danh sách học viên có phân trang, tìm kiếm, lọc theo trạng thái, kèm thống kê tổng hợp |
| `/api/v1/students/{userId}` | GET    | Lấy chi tiết thông tin học viên                                                            |
| `/api/v1/students/{userId}` | PUT    | Cập nhật thông tin học viên                                                                |
| `/api/v1/students/{userId}` | DELETE | Xoá mềm (Soft Delete) học viên                                                             |

#### Class Schedule (Lịch học)

| API Endpoint                           | Method | Mô tả                                                                                    |
|----------------------------------------|--------|------------------------------------------------------------------------------------------|
| `/api/v1/class-schedules`              | GET    | Lấy danh sách lịch học với đa bộ lọc (branchId, weekday, level, shift, location, status) |
| `/api/v1/class-schedules/{scheduleId}` | GET    | Lấy chi tiết lịch học (kèm danh sách HLV phân công & số học viên)                        |
| `/api/v1/class-schedules`              | POST   | Tạo mới lịch học                                                                         |
| `/api/v1/class-schedules/{scheduleId}` | PUT    | Cập nhật lịch học                                                                        |

---

### ⚙️ Operation Module – Quản lý Vận Hành

#### Coach Assignment (Phân công HLV)

| API Endpoint                                | Method | Mô tả                                                                     |
|---------------------------------------------|--------|---------------------------------------------------------------------------|
| `/api/v1/coach-assignments`                 | POST   | Phân công HLV vào một hoặc nhiều lớp học cùng lúc (batch)                 |
| `/api/v1/coach-assignments/{id}`            | PUT    | Cập nhật trạng thái phân công (ACTIVE / PENDING / COMPLETED / TERMINATED) |
| `/api/v1/coach-assignments/{id}`            | DELETE | Xoá phân công HLV                                                         |
| `/api/v1/coach-assignments/coach/{coachId}` | GET    | Lấy danh sách lớp học đang phụ trách của một HLV                          |

#### Student Enrollment (Đăng ký học)

| API Endpoint                                        | Method | Mô tả                                                                   |
|-----------------------------------------------------|--------|-------------------------------------------------------------------------|
| `/api/v1/student-enrollments`                       | POST   | Đăng ký học viên vào một hoặc nhiều lớp học (batch)                     |
| `/api/v1/student-enrollments/{id}`                  | PUT    | Cập nhật trạng thái đăng ký (ACTIVE / RESERVED / TRANSFERRED / DROPPED) |
| `/api/v1/student-enrollments/{id}`                  | DELETE | Xoá bản ghi đăng ký                                                     |
| `/api/v1/student-enrollments/student/{studentCode}` | GET    | Lấy danh sách lớp học của một học viên                                  |
| `/api/v1/student-enrollments/schedule/{scheduleId}` | GET    | Lấy danh sách học viên trong một lớp học                                |

#### Student Attendance (Điểm danh)

| API Endpoint                                  | Method | Mô tả                                                                                                          |
|-----------------------------------------------|--------|----------------------------------------------------------------------------------------------------------------|
| `/api/v1/student-attendances`                 | POST   | Tạo bản ghi điểm danh thủ công cho một học viên                                                                |
| `/api/v1/student-attendances/batch-init`      | POST   | Khởi tạo danh sách điểm danh hàng loạt cho một buổi học (idempotent: chỉ tạo học viên còn thiếu)               |
| `/api/v1/student-attendances/{id}/status`     | PATCH  | Cập nhật trạng thái điểm danh (PRESENT / ABSENT / LATE / EXCUSED)                                              |
| `/api/v1/student-attendances/{id}/evaluation` | PATCH  | Cập nhật đánh giá học viên trong buổi học                                                                      |
| `/api/v1/student-attendances`                 | GET    | Lọc & phân trang bản ghi điểm danh theo nhiều tiêu chí (search, date, status, belt, branch, level, scheduleId) |

**Services xây dựng:**

- `CoachAssignmentService` – Xử lý logic phân công, chống trùng lặp.
- `StudentEnrollmentService` – Xử lý đăng ký hàng loạt với `@Transactional`, kiểm tra trùng.
- `StudentAttendanceService` – Logic điểm danh nâng cao, gửi Push Notification sau điểm danh, tích hợp
  `NotificationService`.
- `NotificationService` – Tích hợp **Firebase Cloud Messaging (FCM)**: gửi đơn lẻ và multicast.

---

## 2. 🗄️ Cấu Trúc Dữ Liệu

### Database Schema (PostgreSQL)

Hệ thống được chia thành **3 schema** độc lập:

#### `security` schema

| Bảng         | Mô tả                                                            |
|--------------|------------------------------------------------------------------|
| `user`       | Bảng người dùng chung – base entity, chiến lược kế thừa `JOINED` |
| `role`       | Bảng vai trò (STUDENT, COACH, ADMIN…)                            |
| `auth_token` | Bảng lưu Refresh Token theo device ID & FCM Token                |

#### `core` schema

| Bảng             | Mô tả                                                        |
|------------------|--------------------------------------------------------------|
| `branch`         | Chi nhánh (Branch)                                           |
| `student`        | Học viên – kế thừa `user` qua `@PrimaryKeyJoinColumn`        |
| `coach`          | Huấn luyện viên – kế thừa `user` qua `@PrimaryKeyJoinColumn` |
| `class_schedule` | Lịch học: ngày trong tuần, ca, cấp độ, phòng tập, trạng thái |

#### `operation` schema

| Bảng                 | Mô tả                                                                                            |
|----------------------|--------------------------------------------------------------------------------------------------|
| `student_enrollment` | Đăng ký lớp học của học viên; unique constraint theo `(studentId, scheduleId, status)`           |
| `student_attendance` | Điểm danh; unique constraint `uk_student_enrollment_date(enrollment_id, session_date)`           |
| `coach_assignment`   | Phân công HLV vào lớp học                                                                        |
| `coach_timesheet`    | Bảng chấm công HLV; unique constraint `uk_coach_schedule_date(coachId, scheduleId, workingDate)` |
| `belt_promotion`     | Lịch sử thi lên đai của học viên                                                                 |

### Entities & Relationships

- `User` ←(JOINED)→ `Student` / `Coach`: Tái sử dụng thông tin user, tách biệt nghiệp vụ.
- `ClassSchedule` ←(ManyToOne)→ `Branch`
- `StudentEnrollment` ←(ManyToOne)→ `Student`, `ClassSchedule`
- `StudentAttendance` ←(ManyToOne)→ `StudentEnrollment`, `Coach` (recorded & evaluated)
- `CoachAssignment` ←(ManyToOne)→ `Coach`, `ClassSchedule`
- `CoachTimesheet` ←(ManyToOne)→ `Coach`, `ClassSchedule`; Unique: `(coach, schedule, date)`

### Index Được Thiết Kế

- `idx_student_status`, `idx_student_branch` trên bảng `student`
- `idx_enrollment_student`, `idx_enrollment_schedule`, `idx_enrollment_status` trên `student_enrollment`
- `idx_student_enrollment`, `idx_session_date DESC` trên `student_attendance`
- `idx_assignment_coach`, `idx_assignment_schedule` trên `coach_assignment`

### Repository Pattern

- Triển khai **Custom Repository** (`StudentAttendanceRepositoryCustom` + `StudentAttendanceRepositoryCustomImpl`) dùng
  `EntityManager` & Criteria API để áp dụng `NamedEntityGraph` kết hợp với `Specification` và `Pageable` – giải quyết
  bài toán **N+1 query**.
- `WeekdayConverter` – Custom JPA `AttributeConverter` để lưu enum `Weekday` dưới dạng số (2, 3, 4…) thay vì chuỗi.

---

## 3. 🐛 Bug Fixes & Refactoring

### Bug Fixes

- **N+1 Query Problem:** Xử lý triệt để bài toán N+1 khi query `StudentAttendance` với Specification + Pageable bằng
  cách triển khai `CustomRepositoryImpl` dùng `EntityGraph` (hint: `jakarta.persistence.fetchgraph`) thay vì
  `JOIN FETCH` gây conflict với `Pageable`.
- **Trùng lặp phân công / đăng ký:** Thêm kiểm tra `exists()` trước khi INSERT trong `StudentEnrollmentService` và
  `CoachAssignmentService`, ném `AppException` với `ErrorCode` rõ ràng thay vì lỗi constraint database.
- **Validation thời gian lịch học:** Thêm `@PrePersist` / `@PreUpdate` trong entity `ClassSchedule` để đảm bảo
  `endTime > startTime` tại tầng JPA, không phụ thuộc vào FE.
- **Xung đột `status` field kế thừa:** Đổi tên field trong `Student` từ `status` → `studentStatus` để tránh xung đột với
  `User.status` trong chiến lược kế thừa `JOINED`.

### Refactoring & Tối Ưu Hoá

- **Tách biệt Module:** Toàn bộ codebase được tổ chức theo 3 package: `Security` / `Core` / `Operation` — nhất quán từ
  Controller → Service → Repository → DTO → Mapper → Enum.
- **Batch Insert:** Cấu hình Hibernate `jdbc.batch_size=50`, `order_inserts=true`, `order_updates=true` trong
  `application.yml` để tối ưu ghi dữ liệu hàng loạt.
- **Cache Layer:** Tích hợp **Redis** với `RedisCacheManager`, cấu hình TTL động (`CacheTtlConfig`) có jitter ngẫu
  nhiên (random offset) để tránh hiện tượng **Cache Stampede** khi nhiều key hết hạn cùng lúc.
- **Message Queue:** Cấu hình **RabbitMQ** với `TopicExchange` + 2 queue riêng biệt (`attendance_queue`,
  `evaluation_queue`) để xử lý bất đồng bộ tác vụ sau điểm danh và đánh giá.
- **Global Exception Handler:** Xây dựng `GlobalException` (`@RestControllerAdvice`) xử lý tập trung:
  `UsernameNotFoundException`, `BadCredentialsException`, `MethodArgumentNotValidException`, `AppException` (custom),
  trả về cấu trúc `RestResponse<T>` chuẩn hoá.
- **Security Filter:** Thêm `UserStatusValidationFilter` (chạy sau JWT decode) để từ chối request từ tài khoản bị
  `LOCKED` mà không cần truy vấn database ở mỗi request.
- **Hibernate Query Plan Cache:** Tăng `plan_cache_max_size=2048` và `plan_parameter_metadata_max_size=128` để tối ưu
  hiệu suất query động từ Specification.
- **ConnectionPool (HikariCP):** Cấu hình `maximum-pool-size=3`, `leak-detection-threshold=60s`, `auto-commit=false` phù
  hợp với môi trường có tài nguyên giới hạn.
- **Soft Delete Pattern:** Áp dụng nhất quán Soft Delete trên `Coach` và `Student` – không xoá dữ liệu thực, chỉ đổi
  trạng thái.
- **DTO Naming Convention:** Đổi tên method `createStudentEnrollement` → `createStudentEnrollment` (sửa lỗi chính tả) và
  chuẩn hoá naming cho toàn bộ DTO (`ReqDTO` / `ResDTO`).

---

## 4. 📝 Tóm Tắt

Backend tuần này đã hoàn thiện **toàn bộ lớp API nghiệp vụ cốt lõi** của hệ thống, bao gồm xác thực JWT đa thiết bị,
quản lý học viên/HLV/lịch học, điểm danh hàng loạt và phân công lớp học — với kiến trúc được tổ chức sạch theo module và
các cơ chế tối ưu hoá hiệu năng (Redis Cache, RabbitMQ async, N+1 fix, Batch Insert). Hệ thống đã sẵn sàng cho giai đoạn
tích hợp và kiểm thử với Frontend.

---

*Generated: 08/03/2026*

