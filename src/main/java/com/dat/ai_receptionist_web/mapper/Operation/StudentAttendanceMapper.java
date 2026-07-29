package com.dat.ai_receptionist_web.mapper.Operation;

import com.dat.ai_receptionist_web.domain.Core.ClassSchedule;
import com.dat.ai_receptionist_web.domain.Operation.ClassSession;
import com.dat.ai_receptionist_web.domain.Operation.StudentAttendance;
import com.dat.ai_receptionist_web.domain.Operation.StudentEnrollment;
import com.dat.ai_receptionist_web.dto.Core.ClassScheduleResDTO;
import com.dat.ai_receptionist_web.dto.Core.StudentResDTO;
import com.dat.ai_receptionist_web.dto.Operation.CheckInStudentProjection;
import com.dat.ai_receptionist_web.dto.Operation.StudentAttendanceDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface StudentAttendanceMapper {

    @Mapping(source = "studentEnrollment.student.personId", target = "studentSummary.personId")
    @Mapping(source = "studentEnrollment.student.fullName", target = "studentSummary.fullName")
    @Mapping(source = "studentEnrollment.student.studentCode", target = "studentSummary.code")
    @Mapping(source = "studentEnrollment.student.belt", target = "studentSummary.belt")
    @Mapping(source = "studentEnrollment.classSchedule.scheduleId", target = "classSchedule.scheduleId")
    @Mapping(source = "studentEnrollment.classSchedule.branch.branchName", target = "classSchedule.branchName")
    @Mapping(source = "studentEnrollment.classSchedule.location", target = "classSchedule.scheduleLocation")
    @Mapping(source = "studentEnrollment.classSchedule.level", target = "classSchedule.scheduleLevel")
    @Mapping(source = "studentEnrollment.classSchedule.shift", target = "classSchedule.scheduleShift")
    @Mapping(source = "studentEnrollment.classSchedule.monthlyFee", target = "classSchedule.monthlyFee")
    @Mapping(source = "studentEnrollment.classSchedule.quarterlyFee", target = "classSchedule.quarterlyFee")
    @Mapping(source = "studentEnrollment.classSchedule.startTime", target = "classSchedule.startTime")
    @Mapping(source = "studentEnrollment.classSchedule.endTime", target = "classSchedule.endTime")
    @Mapping(source = "studentEnrollment.classSchedule.weekday", target = "classSchedule.weekday")
    @Mapping(source = "evaluatedByCoach.fullName", target = "evaluatedByCoachName")
    @Mapping(source = "studentEnrollment.enrollmentId", target = "enrollmentId")
    StudentAttendanceDTO.Response toResponse(StudentAttendance entity);

    @Mapping(source = "studentEnrollment.student.personId", target = "studentId")
    @Mapping(source = "studentEnrollment.enrollmentId", target = "enrollmentId")
    @Mapping(source = "evaluatedByCoach.fullName", target = "evaluatedByCoachName")
    StudentAttendanceDTO.SimpleResponse toSimpleResponse(StudentAttendance entity);

    List<StudentAttendanceDTO.SimpleResponse> toSimpleResponseList(List<StudentAttendance> entities);

    List<StudentAttendanceDTO.Response> toResponseList(List<StudentAttendance> entities);

    default StudentAttendanceDTO.Response toCheckInResponse(
            StudentAttendance attendance,
            CheckInStudentProjection student,
            StudentEnrollment enrollment,
            ClassSession classSession,
            boolean alreadyCheckedIn
    ) {
        return StudentAttendanceDTO.Response.builder()
                .attendanceId(attendance.getAttendanceId())
                .enrollmentId(enrollment.getEnrollmentId())
                .studentSummary(StudentResDTO.StudentSummary.builder()
                        .personId(student.getPersonId())
                        .fullName(student.getFullName())
                        .code(student.getStudentCode())
                        .belt(enrollment.getStudent().getBelt())
                        .build())
                .classSchedule(toClassScheduleSummary(classSession.getClassSchedule()))
                .sessionDate(attendance.getSessionDate())
                .attendanceStatus(attendance.getAttendanceStatus())
                .checkInTime(attendance.getCheckInTime())
                .alreadyCheckedIn(alreadyCheckedIn)
                .evaluationStatus(attendance.getEvaluationStatus())
                .note(attendance.getNote())
                .updatedAt(attendance.getUpdatedAt())
                .build();
    }

    default ClassScheduleResDTO.ClassScheduleSummary toClassScheduleSummary(ClassSchedule schedule) {
        if (schedule == null) {
            return null;
        }
        return ClassScheduleResDTO.ClassScheduleSummary.builder()
                .scheduleId(schedule.getScheduleId())
                .branchName(schedule.getBranch() == null ? null : schedule.getBranch().getBranchName())
                .scheduleLocation(schedule.getLocation())
                .scheduleLevel(schedule.getLevel())
                .scheduleShift(schedule.getShift())
                .monthlyFee(schedule.getMonthlyFee())
                .quarterlyFee(schedule.getQuarterlyFee())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .weekday(schedule.getWeekday())
                .build();
    }
}
