package com.dat.ai_receptionist_web.mapper.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.Course;
import com.dat.ai_receptionist_web.dto.Catalog.CourseDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CourseMapper {
    @Mapping(target = "classScheduleId", source = "classSchedule.scheduleId")
    CourseDTO.Response toResponse(Course entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "classSchedule", ignore = true)
    @Mapping(target = "capacity", source = "capacity")
    @Mapping(target = "status", source = "status")
    void updateEntity(CourseDTO.UpdateRequest request, @MappingTarget Course entity);
}
