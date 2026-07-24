package com.dat.ai_receptionist_web.mapper.Operation;

import com.dat.ai_receptionist_web.domain.Operation.StudentAttendance;
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

    @Mapping(source = "studentEnrollment.student.personId", target = "studentId")
    @Mapping(source = "studentEnrollment.student.fullName", target = "studentName")
    @Mapping(source = "studentEnrollment.classSchedule.scheduleId", target = "classScheduleId")
    @Mapping(source = "evaluatedByCoach.fullName", target = "evaluatedByCoachName")
    @Mapping(source = "studentEnrollment.enrollmentId", target = "enrollmentId")
    StudentAttendanceDTO.Response toResponse(StudentAttendance entity);

    @Mapping(source = "studentEnrollment.student.personId", target = "studentId")
    @Mapping(source = "studentEnrollment.enrollmentId", target = "enrollmentId")
    @Mapping(source = "evaluatedByCoach.fullName", target = "evaluatedByCoachName")
    StudentAttendanceDTO.SimpleResponse toSimpleResponse(StudentAttendance entity);

    List<StudentAttendanceDTO.SimpleResponse> toSimpleResponseList(List<StudentAttendance> entities);

    List<StudentAttendanceDTO.Response> toResponseList(List<StudentAttendance> entities);
}
