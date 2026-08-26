package com.dat.ai_receptionist_web.mapper.Training;

import com.dat.ai_receptionist_web.domain.Training.CoachTimesheet;
import com.dat.ai_receptionist_web.dto.Training.CoachTimesheetDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CoachTimesheetMapper {
    @Mapping(target = "coachAssignmentId", source = "coachAssignment.coachAssignmentId")
    @Mapping(target = "classSessionId", source = "classSession.classSessionId")
    CoachTimesheetDTO.Response toResponse(CoachTimesheet entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "coachAssignment", ignore = true)
    @Mapping(target = "classSession", ignore = true)
    @Mapping(target = "checkInTime", source = "checkInTime")
    @Mapping(target = "checkOutTime", source = "checkOutTime")
    @Mapping(target = "note", source = "note")
    void updateEntity(CoachTimesheetDTO.UpdateRequest request, @MappingTarget CoachTimesheet entity);
}
