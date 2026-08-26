package com.dat.ai_receptionist_web.mapper.Training;

import com.dat.ai_receptionist_web.domain.Training.StudentEnrollment;
import com.dat.ai_receptionist_web.dto.Training.StudentEnrollmentDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface StudentEnrollmentMapper {
    @Mapping(target = "studentPersonId", source = "studentPerson.personId")
    @Mapping(target = "coursePurchaseId", source = "coursePurchase.coursePurchaseId")
    @Mapping(target = "classScheduleId", source = "classSchedule.scheduleId")
    StudentEnrollmentDTO.Response toResponse(StudentEnrollment entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "studentPerson", ignore = true)
    @Mapping(target = "coursePurchase", ignore = true)
    @Mapping(target = "classSchedule", ignore = true)
    @Mapping(target = "startDate", source = "startDate")
    @Mapping(target = "endDate", source = "endDate")
    @Mapping(target = "status", source = "status")
    void updateEntity(StudentEnrollmentDTO.UpdateRequest request, @MappingTarget StudentEnrollment entity);
}
