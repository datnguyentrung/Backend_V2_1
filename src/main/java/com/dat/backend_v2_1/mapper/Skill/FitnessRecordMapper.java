package com.dat.backend_v2_1.mapper.Skill;

import com.dat.backend_v2_1.domain.Skill.FitnessRecord;
import com.dat.backend_v2_1.dto.Skill.FitnessRecordDTO;
import com.dat.backend_v2_1.mapper.Core.CoachMapper;
import com.dat.backend_v2_1.mapper.Core.StudentMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {StudentMapper.class, CoachMapper.class}
)
public interface FitnessRecordMapper {
    @Mapping(target = "metrics.duration", source = "duration")
    @Mapping(target = "metrics.amount", source = "amount")
    @Mapping(target = "metrics.skillLevel", source = "skillLevel")
    @Mapping(target = "id", source = "id")
    FitnessRecordDTO.Response toResponse(FitnessRecord fitnessRecord);

    FitnessRecordDTO.Metrics toMetrics(FitnessRecord fitnessRecord);
}
