package com.dat.backend_v2_1.mapper.Operation;

import com.dat.backend_v2_1.domain.Operation.CoachAssignment;
import com.dat.backend_v2_1.dto.Core.ClassScheduleResDTO;
import com.dat.backend_v2_1.dto.Core.CoachResDTO;
import com.dat.backend_v2_1.dto.Operation.CoachAssignmentReqDTO;
import com.dat.backend_v2_1.dto.Operation.CoachAssignmentResDTO;
import com.dat.backend_v2_1.mapper.Core.CoachMapper;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {CoachMapper.class}
)
public interface CoachAssignmentMapper {
    @Mapping(target = "coach", ignore = true)
    @Mapping(target = "classSchedule", ignore = true)
    @Mapping(target = "assignedDate", source = "assignmentDate")
    @Mapping(target = "endDate", source = "endDate")
    @Mapping(target = "note", source = "note")
    CoachAssignment toEntity(CoachAssignmentReqDTO.CreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(CoachAssignmentReqDTO.UpdateRequest request, @MappingTarget CoachAssignment entity);

    // Mapping entity to Response DTO (Detail)
    @Mapping(target = "coach", source = "coach", qualifiedByName = "toCoachSummary")
    @Mapping(target = "classSchedule", source = "classSchedule", qualifiedByName = "toClassScheduleSummary")
    CoachAssignmentResDTO.Response toResponse(CoachAssignment entity);

    // Mapping entity to SimpleResponse DTO
    @Mapping(target = "coach", source = "coach", qualifiedByName = "toCoachSummary")
    CoachAssignmentResDTO.SimpleResponse toSimpleResponse(CoachAssignment entity);

    // Named method to map Coach to CoachSummary
    @Named("toCoachSummary")
    default CoachResDTO.CoachSummary toCoachSummary(com.dat.backend_v2_1.domain.Core.Coach coach) {
        if (coach == null) {
            return null;
        }
        return CoachResDTO.CoachSummary.builder()
                .userId(coach.getUserId())
                .fullName(coach.getFullName())
                .staffCode(coach.getStaffCode())
                .build();
    }

    // Named method to map ClassSchedule to ClassScheduleSummary
    @Named("toClassScheduleSummary")
    default ClassScheduleResDTO.ClassScheduleSummary toClassScheduleSummary(com.dat.backend_v2_1.domain.Core.ClassSchedule classSchedule) {
        if (classSchedule == null) {
            return null;
        }
        return ClassScheduleResDTO.ClassScheduleSummary.builder()
                .scheduleId(classSchedule.getScheduleId())
                .branchName(classSchedule.getBranch() != null ? classSchedule.getBranch().getBranchName() : null)
                .scheduleLocation(classSchedule.getLocation())
                .scheduleLevel(classSchedule.getLevel())
                .scheduleShift(classSchedule.getShift())
                .startTime(classSchedule.getStartTime())
                .endTime(classSchedule.getEndTime())
                .weekday(classSchedule.getWeekday())
                .build();
    }
}
