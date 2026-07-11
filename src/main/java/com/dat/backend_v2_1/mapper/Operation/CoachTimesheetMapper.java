package com.dat.backend_v2_1.mapper.Operation;

import com.dat.backend_v2_1.domain.Core.ClassSchedule;
import com.dat.backend_v2_1.domain.Core.Coach;
import com.dat.backend_v2_1.domain.Operation.CoachTimesheet;
import com.dat.backend_v2_1.dto.Core.ClassScheduleResDTO;
import com.dat.backend_v2_1.dto.Core.CoachResDTO;
import com.dat.backend_v2_1.dto.Operation.CoachTimesheetDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.time.LocalTime;
import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface CoachTimesheetMapper {

    @Mapping(target = "coachAssignmentId", source = "coachAssignment.assignmentId")
    @Mapping(target = "coach", source = "coachAssignment.coach", qualifiedByName = "toCoachSummary")
    @Mapping(target = "classSchedule", source = "coachAssignment.classSchedule", qualifiedByName = "toClassScheduleSummary")
    CoachTimesheetDTO.Response toResponse(CoachTimesheet entity);

    List<CoachTimesheetDTO.Response> toResponseList(List<CoachTimesheet> entities);

    @Named("timeToString")
    default String timeToString(LocalTime value) {
        return value == null ? null : value.toString();
    }

    @Named("toCoachSummary")
    default CoachResDTO.CoachSummary toCoachSummary(Coach coach) {
        if (coach == null) {
            return null;
        }
        return CoachResDTO.CoachSummary.builder()
                .userId(coach.getUserId())
                .fullName(coach.getFullName())
                .staffCode(coach.getStaffCode())
                .build();
    }

    @Named("toClassScheduleSummary")
    default ClassScheduleResDTO.ClassScheduleSummary toClassScheduleSummary(ClassSchedule classSchedule) {
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
