package com.dat.ai_receptionist_web.mapper.Operation;

import com.dat.ai_receptionist_web.domain.Operation.StudentEnrollment;
import com.dat.ai_receptionist_web.dto.Core.ClassScheduleResDTO;
import com.dat.ai_receptionist_web.dto.Operation.StudentEnrollmentReqDTO;
import com.dat.ai_receptionist_web.dto.Operation.StudentEnrollmentResDTO;
import com.dat.ai_receptionist_web.mapper.Core.ClassScheduleMapper;
import com.dat.ai_receptionist_web.mapper.Core.StudentMapper;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {StudentMapper.class, ClassScheduleMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface StudentEnrollmentMapper { // 1. Đổi thành interface

    // Mapping cho Create (Như cũ)
    @Mapping(target = "student", ignore = true)
    @Mapping(target = "classSchedule", ignore = true)
    @Mapping(target = "joinDate", source = "joinDate")
    @Mapping(target = "note", source = "note")
    StudentEnrollment toEntity(StudentEnrollmentReqDTO.CreateRequest request);

    // Mapping cho Update
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(StudentEnrollmentReqDTO.UpdateRequest request, @MappingTarget StudentEnrollment entity);

    // Mapping cho Response (Quan trọng)
    @Mapping(target = "student", source = "student") // Map entity Student sang StudentSummary
    @Mapping(target = "classSchedule", source = "classSchedule")
    // Map entity Class sang ClassSummary
    StudentEnrollmentResDTO.Response toResponse(StudentEnrollment entity);

    // Mapping cho SimpleResponse
    @Mapping(target = "classScheduleSummary", source = "classSchedule")
    @Mapping(target = "studentSummary", source = "student")
    @Mapping(target = "joinDate", source = "joinDate")
    @Mapping(target = "status", source = "status")
    StudentEnrollmentResDTO.SimpleResponse toSimpleResponse(StudentEnrollment entity);

    List<StudentEnrollmentResDTO.SimpleResponse> toSimpleResponseList(List<StudentEnrollment> entities);

    @Mapping(target = "studentSummary", source = "student")
    @Mapping(target = "joinDate", source = "joinDate")
    @Mapping(target = "status", source = "status")
    StudentEnrollmentResDTO.EnrolledStudentItem toEnrolledStudentItem(StudentEnrollment entity);

    List<StudentEnrollmentResDTO.EnrolledStudentItem> toEnrolledStudentItemList(List<StudentEnrollment> entities);

    List<ClassScheduleResDTO.ClassScheduleSummary> toClassScheduleSummaryList(List<StudentEnrollment> entities);
}