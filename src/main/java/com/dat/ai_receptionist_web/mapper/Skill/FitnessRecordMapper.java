package com.dat.ai_receptionist_web.mapper.Skill;

import com.dat.ai_receptionist_web.domain.Skill.FitnessRecord;
import com.dat.ai_receptionist_web.dto.Skill.FitnessRecordDTO;
import com.dat.ai_receptionist_web.mapper.Core.CoachMapper;
import com.dat.ai_receptionist_web.mapper.Core.StudentMapper;
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

    @Mapping(target = "studentSummary", source = "student")
    @Mapping(target = "id", source = "id")

    @Mapping(target = "metrics.createdAt", source = "createdAt")
    @Mapping(target = "metrics.assessmentDate", source = "assessmentDate")
    @Mapping(target = "metrics.duration", source = "duration")
    @Mapping(target = "metrics.amount", source = "amount")
    @Mapping(target = "metrics.skillLevel", source = "skillLevel")
    FitnessRecordDTO.Response toResponse(FitnessRecord fitnessRecord);

    FitnessRecordDTO.Metrics toMetrics(FitnessRecord fitnessRecord);
}
