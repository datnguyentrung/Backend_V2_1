# Business Rule

Tài liệu này mô tả các chỉ số và rule nghiệp vụ để Figma AI hiểu card nào quan trọng, chart nào quan trọng và các số liệu liên hệ với nhau như thế nào.

## Quy ước

- "Tháng đang xem" = tháng/năm được chọn trên màn hình.
- "Chi nhánh đang xem" = Branch được chọn; nếu không chọn thì tính toàn hệ thống.
- "Lớp đang xem" = ClassSchedule được chọn; nếu không chọn thì tính toàn bộ lớp trong phạm vi lọc.
- Với học phí, ưu tiên dùng TuitionPaymentDetail vì entity này có forMonth, forYear và enrollment.
- Với điểm danh, ưu tiên dùng StudentAttendance vì entity này có sessionDate, attendanceStatus và evaluationStatus.

## Student

"Học viên đang tập"

=
COUNT DISTINCT Student
WHERE Student.studentStatus = ACTIVE

----------------------------------

"Học viên mới"

=
COUNT DISTINCT Student
WHERE Student.startDate thuộc tháng đang xem

----------------------------------

"Học viên bảo lưu"

=
COUNT DISTINCT Student
WHERE Student.studentStatus = RESERVED

----------------------------------

"Học viên đã nghỉ"

=
COUNT DISTINCT Student
WHERE Student.studentStatus = DROPPED

----------------------------------

"Học viên theo chi nhánh"

=
COUNT DISTINCT Student
GROUP BY Student.branch

----------------------------------

"Học viên theo cấp đai"

=
COUNT DISTINCT Student
GROUP BY Student.belt

## Enrollment / Class

"Học viên đang học lớp"

=
COUNT DISTINCT StudentEnrollment.student
WHERE StudentEnrollment.status = ACTIVE

----------------------------------

"Sĩ số lớp"

=
COUNT DISTINCT StudentEnrollment.student
WHERE StudentEnrollment.classSchedule = lớp đang xem
AND StudentEnrollment.status = ACTIVE

----------------------------------

"Học viên chuyển lớp"

=
COUNT DISTINCT StudentEnrollment.student
WHERE StudentEnrollment.status = TRANSFERRED

----------------------------------

"Lớp đang hoạt động"

=
COUNT ClassSchedule
WHERE ClassSchedule.scheduleStatus = ACTIVE

----------------------------------

"Số buổi học trong tháng"

=
COUNT ClassSession
WHERE ClassSession.sessionDate thuộc tháng đang xem

----------------------------------

"Buổi chưa đóng điểm danh"

=
COUNT ClassSession
WHERE ClassSession.isAttendanceClosed = false
AND ClassSession.sessionDate <= hôm nay

## Attendance

"Tổng lượt điểm danh"

=
COUNT StudentAttendance
WHERE StudentAttendance.sessionDate thuộc tháng đang xem

----------------------------------

"Tỷ lệ điểm danh"

=
(COUNT attendanceStatus = PRESENT + COUNT attendanceStatus = LATE)
/
COUNT StudentAttendance

----------------------------------

"Tỷ lệ vắng"

=
COUNT StudentAttendance WHERE attendanceStatus = ABSENT
/
COUNT StudentAttendance

----------------------------------

"Học viên nghỉ 3 buổi"

=
Student có 3 StudentAttendance liên tiếp
WHERE attendanceStatus = ABSENT
ORDER BY sessionDate

----------------------------------

"Học viên đi muộn nhiều"

=
Student có COUNT StudentAttendance attendanceStatus = LATE >= 3
trong tháng đang xem

----------------------------------

"Điểm danh cần đánh giá"

=
COUNT StudentAttendance
WHERE evaluationStatus = PENDING
OR evaluationStatus IS NULL

----------------------------------

"Tỷ lệ đánh giá tốt"

=
COUNT StudentAttendance WHERE evaluationStatus = GOOD
/
COUNT StudentAttendance WHERE evaluationStatus IS NOT NULL

## Tuition

"Doanh thu tháng"

=
SUM TuitionPaymentDetail.amountAllocated
WHERE forMonth = tháng đang xem
AND forYear = năm đang xem

----------------------------------

"Tiền thực nhận trong tháng"

=
SUM TuitionPayment.totalAmount
WHERE TuitionPayment.createdAt thuộc tháng đang xem

----------------------------------

"Doanh thu theo chi nhánh"

=
SUM TuitionPaymentDetail.amountAllocated
GROUP BY TuitionPaymentDetail.enrollment.classSchedule.branch

----------------------------------

"Doanh thu theo lớp"

=
SUM TuitionPaymentDetail.amountAllocated
GROUP BY TuitionPaymentDetail.enrollment.classSchedule

----------------------------------

"Chưa ghi nhận học phí"

=
StudentEnrollment.status = ACTIVE
AND không tồn tại TuitionPaymentDetail
WHERE TuitionPaymentDetail.enrollment = StudentEnrollment
AND forMonth = tháng đang xem
AND forYear = năm đang xem

----------------------------------

"Số tiền còn thiếu dự kiến"

=
SUM ClassSchedule.monthlyFee
FROM StudentEnrollment ACTIVE chưa có TuitionPaymentDetail của tháng đang xem

## Coach

"HLV đang hoạt động"

=
COUNT Coach
WHERE Coach.coachStatus = ACTIVE

----------------------------------

"HLV đang phụ trách lớp"

=
COUNT DISTINCT CoachAssignment.coach
WHERE CoachAssignment.status = ACTIVE
AND assignedDate <= ngày đang xem
AND (endDate IS NULL OR endDate >= ngày đang xem)

----------------------------------

"Ngày công HLV"

=
COUNT CoachTimesheet
WHERE workingDate thuộc tháng đang xem

----------------------------------

"HLV chưa check-out"

=
COUNT CoachTimesheet
WHERE checkInTime IS NOT NULL
AND checkOutTime IS NULL

## Progress

"Bản ghi thể lực trong tháng"

=
COUNT FitnessRecord
WHERE assessmentDate thuộc tháng đang xem

----------------------------------

"Tiến bộ kỹ năng"

=
So sánh FitnessRecord.amount hoặc FitnessRecord.duration
giữa lần đánh giá mới nhất và lần đánh giá trước đó
của cùng Student và SkillLevel

----------------------------------

"Học viên thi đai trong tháng"

=
COUNT DISTINCT BeltPromotion.student
WHERE examDate thuộc tháng đang xem

----------------------------------

"Tỷ lệ đậu thi đai"

=
COUNT BeltPromotion WHERE result = PASSED
/
COUNT BeltPromotion WHERE result IN (PASSED, FAILED)

----------------------------------

"Hồ sơ chờ kết quả thi đai"

=
COUNT BeltPromotion
WHERE result = PENDING

## Dashboard Priority

Card quan trọng:

- Học viên đang tập
- Học viên mới
- Tỷ lệ điểm danh
- Học viên nghỉ 3 buổi
- Doanh thu tháng
- Chưa ghi nhận học phí
- Buổi chưa đóng điểm danh

Chart quan trọng:

- Học viên theo chi nhánh
- Học viên theo cấp đai
- Tỷ lệ điểm danh theo tháng
- Doanh thu theo tháng
- Doanh thu theo lớp / chi nhánh
- Kết quả thi đai

Bảng quan trọng:

- Danh sách học viên chưa đóng học phí tháng đang xem.
- Danh sách học viên vắng 3 buổi liên tiếp.
- Danh sách buổi học chưa đóng điểm danh.
- Danh sách HLV chưa check-out.
- Danh sách hồ sơ chờ kết quả thi đai.
