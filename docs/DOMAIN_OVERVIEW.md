# Domain Overview

Tài liệu này mô tả domain ở mức nghiệp vụ để designer, product và Figma AI hiểu hệ thống mà không cần đọc code Java.

## Bối cảnh

Hệ thống quản lý trung tâm võ thuật / thể thao với các luồng chính:

- Quản lý học viên, huấn luyện viên và chi nhánh.
- Quản lý lịch lớp, buổi học thực tế và phân công HLV.
- Theo dõi ghi danh lớp, điểm danh, đánh giá buổi học.
- Quản lý học phí theo học viên, lớp và từng tháng.
- Theo dõi thể lực, kỹ năng và lịch sử lên đai.

## Core Domain

### Branch

Chi nhánh / cơ sở vận hành lớp học.

- branchName
- address
- hotline
- openedDate
- status

Quan hệ:

- Một chi nhánh có nhiều Student.
- Một chi nhánh có nhiều ClassSchedule.

### Student

Học viên của trung tâm.

- studentCode
- fullName
- gender
- birthDate
- email
- nationalCode
- belt
- startDate
- studentStatus
- branch

Trạng thái:

- ACTIVE: đang tập
- RESERVED: bảo lưu
- DROPPED: đã nghỉ

Quan hệ:

- Thuộc một Branch.
- Ghi danh vào lớp qua StudentEnrollment.
- Có Attendance, TuitionPayment, FitnessRecord và BeltPromotion.

### Coach

Huấn luyện viên / nhân sự đứng lớp.

- staffCode
- fullName
- gender
- birthDate
- email
- nationalCode
- belt
- coachStatus

Quan hệ:

- Được phân công lớp qua CoachAssignment.
- Có thể ghi nhận điểm danh hoặc đánh giá học viên.
- Có bảng công CoachTimesheet.

### ClassSchedule

Lịch lớp cố định, ví dụ: thứ 2, ca 1, tại một chi nhánh.

- scheduleId
- branch
- weekday
- level
- startTime
- endTime
- shift
- location
- scheduleStatus
- monthlyFee
- quarterlyFee

Quan hệ:

- Thuộc một Branch.
- Có nhiều StudentEnrollment.
- Sinh ra nhiều ClassSession.
- Có nhiều CoachAssignment.

## Operation Domain

### StudentEnrollment

Việc một học viên tham gia một lịch lớp.

- student
- classSchedule
- joinDate
- endDate
- status
- note

Trạng thái:

- ACTIVE: đang học lớp này
- RESERVED: bảo lưu
- TRANSFERRED: chuyển lớp
- DROPPED: nghỉ lớp

Ghi chú:

- Đây là entity chính để biết học viên đang học lớp nào.
- TuitionPaymentDetail gắn với Enrollment để biết học phí thuộc lớp nào.

### ClassSession

Một buổi học thực tế được sinh ra từ ClassSchedule.

- sessionDate
- classSchedule
- status
- isAttendanceClosed
- startTime
- endTime
- note

Trạng thái:

- SCHEDULED
- ACTIVE
- COMPLETED
- CANCELLED
- POSTPONED
- TERMINATED

### StudentAttendance

Bản ghi điểm danh của một học viên trong một buổi học.

- studentEnrollment
- classSession
- sessionDate
- attendanceStatus
- checkInTime
- recordedByCoach
- evaluationStatus
- evaluatedByCoach
- note

Trạng thái điểm danh:

- PRESENT
- LATE
- ABSENT
- EXCUSED
- MAKEUP

Ghi chú:

- Một Enrollment chỉ nên có một Attendance trong một ClassSession.
- `recordedByCoach` là người ghi nhận điểm danh.
- `evaluatedByCoach` là người đánh giá buổi học.

### CoachAssignment

Phân công HLV vào một lịch lớp.

- coach
- classSchedule
- assignedDate
- endDate
- status
- note

Ghi chú:

- Dùng để biết HLV nào đang phụ trách lớp tại một thời điểm.
- Assignment còn hiệu lực khi status ACTIVE và ngày xem nằm trong khoảng assignedDate đến endDate.

### CoachTimesheet

Bảng công / chấm công của HLV theo ngày.

- coachAssignment
- workingDate
- checkInTime
- checkOutTime
- status
- note

### TuitionPayment

Phiếu thu học phí của một học viên.

- student
- totalAmount
- createdAt
- note

Ghi chú:

- `totalAmount` là tổng tiền thực nhận trên một lần đóng.
- Một Payment có thể chia thành nhiều PaymentDetail.

### TuitionPaymentDetail

Chi tiết phân bổ học phí theo tháng và lớp.

- tuitionPayment
- enrollment
- forMonth
- forYear
- amountAllocated

Ghi chú:

- Đây là entity quan trọng nhất để xác định học phí tháng nào đã được ghi nhận.
- Gắn với Enrollment để biết học phí thuộc lớp nào.

## Progress Domain

### FitnessRecord

Bản ghi thể lực hoặc kỹ năng của học viên.

- assessmentDate
- student
- duration
- amount
- skillLevel
- recordByCoach

### BeltPromotion

Lịch sử thi hoặc xét lên đai.

- student
- examDate
- currentBelt
- targetBelt
- result
- note

Kết quả:

- PENDING
- PASSED
- FAILED

## Notification Domain

### Notification

Nội dung thông báo được tạo bởi hệ thống.

- title
- body
- notificationType
- referenceType
- referenceId
- payload
- recipients

### NotificationRecipient

Trạng thái nhận / đọc thông báo của từng người dùng.

- notification
- recipientUser
- read
- readAt
- deliveredAt
- recipientStatus

## Quan hệ tổng quan

- Branch -> Student
- Branch -> ClassSchedule
- Student -> StudentEnrollment -> ClassSchedule
- ClassSchedule -> ClassSession
- StudentEnrollment + ClassSession -> StudentAttendance
- Coach -> CoachAssignment -> ClassSchedule
- CoachAssignment -> CoachTimesheet
- Student -> TuitionPayment -> TuitionPaymentDetail -> StudentEnrollment
- Student -> FitnessRecord
- Student -> BeltPromotion
- Notification -> NotificationRecipient -> User

## Entity ưu tiên cho dashboard

- Student: số học viên, học viên mới, trạng thái học viên.
- StudentEnrollment: sĩ số lớp, học viên đang học, học viên bảo lưu / nghỉ.
- StudentAttendance: tỷ lệ điểm danh, vắng nhiều, đi muộn, chất lượng buổi học.
- TuitionPaymentDetail: học phí theo tháng, lớp, chi nhánh.
- ClassSession: buổi học, buổi đã hoàn thành, buổi chưa đóng điểm danh.
- CoachTimesheet: ngày công, check-in/check-out.
- BeltPromotion: kết quả thi đai, học viên chờ xét đai.
