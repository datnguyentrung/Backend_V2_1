package com.dat.ai_receptionist_web.mapper.Operation;

import com.dat.ai_receptionist_web.domain.Core.ClassSchedule;
import com.dat.ai_receptionist_web.domain.Core.Coach;
import com.dat.ai_receptionist_web.domain.Operation.CoachTimesheet;
import com.dat.ai_receptionist_web.dto.Core.ClassScheduleResDTO;
import com.dat.ai_receptionist_web.dto.Core.CoachResDTO;
import com.dat.ai_receptionist_web.dto.Operation.CoachTimesheetDTO;
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

    @Mapping(target = "classSessionId", source = "classSession.sessionId")
    @Mapping(target = "coachAssignmentId", source = "coachAssignment.assignmentId")
    @Mapping(target = "coach", source = "coach", qualifiedByName = "toCoachSummary")
    @Mapping(target = "classSchedule", source = "classSession.classSchedule", qualifiedByName = "toClassScheduleSummary")
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
                .personId(coach.getPersonId())
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
