package com.dat.backend_v2_1.mapper.Operation;

import com.dat.backend_v2_1.domain.Operation.ClassSession;
import com.dat.backend_v2_1.dto.Operation.ClassSessionDTO;
import com.dat.backend_v2_1.mapper.Core.ClassScheduleMapper;
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
