package com.dat.ai_receptionist_web.mapper.Operation;

import com.dat.ai_receptionist_web.domain.Operation.ClassSession;
import com.dat.ai_receptionist_web.dto.Operation.ClassSessionDTO;
import com.dat.ai_receptionist_web.mapper.Core.ClassScheduleMapper;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {ClassScheduleMapper.class} // Sử dụng ClassScheduleMapper để map classSchedule nếu cần
)
public interface ClassSessionMapper {
    @Mapping(target = "classSchedule", source = "classSchedule")
    ClassSessionDTO.SessionResponse toSessionResponse(ClassSession session);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(ClassSessionDTO.SessionUpdateRequest request, @MappingTarget ClassSession entity);
}
