package com.dat.ai_receptionist_web.mapper.Training;

import com.dat.ai_receptionist_web.domain.Training.StudentAttendance;
import com.dat.ai_receptionist_web.dto.Training.StudentAttendanceDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface StudentAttendanceMapper {
    @Mapping(target = "classSessionId", source = "classSession.classSessionId")
    @Mapping(target = "studentEnrollmentId", source = "studentEnrollment.studentEnrollmentId")
    @Mapping(target = "evaluatedByCoachId", source = "evaluatedByCoach.personId")
    StudentAttendanceDTO.Response toResponse(StudentAttendance entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "classSession", ignore = true)
    @Mapping(target = "studentEnrollment", ignore = true)
    @Mapping(target = "evaluatedByCoach", ignore = true)
    @Mapping(target = "checkInTime", source = "checkInTime")
    @Mapping(target = "attendanceStatus", source = "attendanceStatus")
    @Mapping(target = "evaluationStatus", source = "evaluationStatus")
    @Mapping(target = "note", source = "note")
    void updateEntity(StudentAttendanceDTO.UpdateRequest request, @MappingTarget StudentAttendance entity);
}
