package com.dat.backend_v2_1.mapper.Core;

import com.dat.backend_v2_1.domain.Core.ClassSchedule;
import com.dat.backend_v2_1.domain.Core.Coach;
import com.dat.backend_v2_1.domain.Operation.CoachAssignment;
import com.dat.backend_v2_1.dto.Core.ClassScheduleReqDTO;
import com.dat.backend_v2_1.dto.Core.ClassScheduleResDTO;
import com.dat.backend_v2_1.dto.Core.CoachResDTO;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ClassScheduleMapper {

    // === MAPPING TO RESPONSE DTO ===

    @Mapping(target = "scheduleId", source = "scheduleId")
    @Mapping(target = "branchId", source = "branch.branchId")
    @Mapping(target = "branchName", source = "branch.branchName")
    @Mapping(target = "scheduleLocation", source = "location")
    @Mapping(target = "scheduleLevel", source = "level")
    @Mapping(target = "scheduleShift", source = "shift")
    @Mapping(target = "startTime", source = "startTime")
    @Mapping(target = "endTime", source = "endTime")
    @Mapping(target = "weekday", source = "weekday")
    @Mapping(target = "coaches", ignore = true) // Sẽ được set từ service
    @Mapping(target = "totalStudents", ignore = true) // Sẽ được set từ service
    ClassScheduleResDTO.ClassScheduleDetail toClassScheduleDetail(ClassSchedule classSchedule);

    /**
     * Map ClassSchedule entity với CoachAssignment list sang ClassScheduleDetail
     * Đây là overload method để hỗ trợ map cả coaches
     */
    default ClassScheduleResDTO.ClassScheduleDetail toClassScheduleDetail(
            ClassSchedule classSchedule,
            List<CoachAssignment> coachAssignments) {

        ClassScheduleResDTO.ClassScheduleDetail detail = toClassScheduleDetail(classSchedule);

        if (coachAssignments != null && !coachAssignments.isEmpty()) {
            List<CoachResDTO.CoachSummary> coaches = coachAssignments.stream()
                    .map(ca -> {
                        Coach coach = ca.getCoach();
                        return CoachResDTO.CoachSummary.builder()
                                .userId(coach.getUserId())
                                .fullName(coach.getFullName())
                                .staffCode(coach.getStaffCode())
                                .build();
                    })
                    .toList();
            detail.setCoaches(coaches);
        }

        return detail;
    }

    @Mapping(target = "scheduleId", source = "scheduleId")
    @Mapping(target = "branchName", source = "branch.branchName")
    @Mapping(target = "scheduleLocation", source = "location")
    @Mapping(target = "scheduleLevel", source = "level")
    @Mapping(target = "scheduleShift", source = "shift")
    ClassScheduleResDTO.ClassScheduleSummary toClassScheduleSummary(ClassSchedule classSchedule);

    // === MAPPING FROM REQUEST DTO ===

    @Mapping(target = "branch", ignore = true) // Sẽ được set từ service
    @Mapping(target = "location", source = "location")
    @Mapping(target = "level", source = "level")
    @Mapping(target = "shift", source = "shift")
    @Mapping(target = "scheduleStatus", source = "scheduleStatus")
    ClassSchedule toEntity(ClassScheduleReqDTO.CreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "scheduleId", ignore = true) // Không cho phép update ID
    @Mapping(target = "branch", ignore = true) // Sẽ được set từ service nếu có
    @Mapping(target = "location", source = "location")
    @Mapping(target = "level", source = "level")
    @Mapping(target = "shift", source = "shift")
    @Mapping(target = "scheduleStatus", source = "scheduleStatus")
    void updateEntityFromDto(ClassScheduleReqDTO.UpdateRequest request, @MappingTarget ClassSchedule entity);
}
