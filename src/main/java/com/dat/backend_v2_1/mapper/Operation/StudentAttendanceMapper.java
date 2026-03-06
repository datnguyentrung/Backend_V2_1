package com.dat.backend_v2_1.mapper.Operation;

import com.dat.backend_v2_1.domain.Operation.StudentAttendance;
import com.dat.backend_v2_1.dto.Operation.StudentAttendanceDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface StudentAttendanceMapper {

    @Mapping(source = "studentEnrollment.student.userId", target = "studentId")
    @Mapping(source = "studentEnrollment.student.fullName", target = "studentName")
    @Mapping(source = "studentEnrollment.classSchedule.scheduleId", target = "classScheduleId")
    @Mapping(source = "recordedByCoach.fullName", target = "recordedByCoachName")
    @Mapping(source = "evaluatedByCoach.fullName", target = "evaluatedByCoachName")
    @Mapping(source = "studentEnrollment.enrollmentId", target = "enrollmentId")
    StudentAttendanceDTO.Response toResponse(StudentAttendance entity);

    @Mapping(source = "studentEnrollment.student.userId", target = "studentId")
    @Mapping(source = "studentEnrollment.enrollmentId", target = "enrollmentId")
    @Mapping(source = "recordedByCoach.fullName", target = "recordedByCoachName")
    @Mapping(source = "evaluatedByCoach.fullName", target = "evaluatedByCoachName")
    StudentAttendanceDTO.SimpleResponse toSimpleResponse(StudentAttendance entity);

    List<StudentAttendanceDTO.SimpleResponse> toSimpleResponseList(List<StudentAttendance> entities);

    List<StudentAttendanceDTO.Response> toResponseList(List<StudentAttendance> entities);
}
