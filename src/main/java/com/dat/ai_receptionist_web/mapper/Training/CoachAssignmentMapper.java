package com.dat.ai_receptionist_web.mapper.Training;

import com.dat.ai_receptionist_web.domain.Training.CoachAssignment;
import com.dat.ai_receptionist_web.dto.Training.CoachAssignmentDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CoachAssignmentMapper {
    @Mapping(target = "coachId", source = "coach.personId")
    @Mapping(target = "courseId", source = "course.courseId")
    CoachAssignmentDTO.Response toResponse(CoachAssignment entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "coach", ignore = true)
    @Mapping(target = "course", ignore = true)
    @Mapping(target = "assignedDate", source = "assignedDate")
    @Mapping(target = "endDate", source = "endDate")
    @Mapping(target = "coachAssignmentStatus", source = "coachAssignmentStatus")
    @Mapping(target = "note", source = "note")
    void updateEntity(CoachAssignmentDTO.UpdateRequest request, @MappingTarget CoachAssignment entity);
}
