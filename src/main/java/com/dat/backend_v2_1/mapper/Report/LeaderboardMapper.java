package com.dat.backend_v2_1.mapper.Report;

import com.dat.backend_v2_1.dto.Core.StudentResDTO;
import com.dat.backend_v2_1.dto.Report.LeaderboardDTO;
import com.dat.backend_v2_1.dto.Report.YearlySummaryDTO;
import com.dat.backend_v2_1.dto.Skill.FitnessRecordDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = org.mapstruct.NullValuePropertyMappingStrategy.IGNORE,
        uses = {YearlySummaryMapper.class} // Sử dụng StudentMapper để map Student sang Student
)
public interface LeaderboardMapper {

    @Mapping(target = "rank", source = "rank")
    @Mapping(target = "studentCode", source = "student.studentCode")
    @Mapping(target = "fullName", source = "student.fullName")
    @Mapping(target = "belt", source = "student.belt")
    @Mapping(target = ".", source = "summary")
        // Đập toàn bộ field của summary vào RankItemForRedis
    LeaderboardDTO.RankItemForRedis toRankItemForRedis(int rank, StudentResDTO.StudentRankInfo student, YearlySummaryDTO.QuarterSummaryForRedis summary);

    @Mapping(target = "data", source = "rankItemForRedis")
        // Lôi toàn bộ field trong RankItemForRedis ra ngoài class cha
    LeaderboardDTO.RankItem<YearlySummaryDTO.QuarterSummary> toRankItem(LeaderboardDTO.RankItemForRedis rankItemForRedis);

    
    List<LeaderboardDTO.RankItem<YearlySummaryDTO.QuarterSummary>> toRankItemList(List<LeaderboardDTO.RankItemForRedis> rankItemForRedisList);

    @Mapping(target = "rank", source = "rank")
    @Mapping(target = "studentCode", source = "student.studentCode")
    @Mapping(target = "fullName", source = "student.fullName")
    @Mapping(target = "belt", source = "student.belt")
    @Mapping(target = "data", source = "data")
        // Map toàn bộ object vào field summary
    LeaderboardDTO.RankItem<FitnessRecordDTO.Metrics> toRankItemFromFitness(int rank, StudentResDTO.StudentRankInfo student, FitnessRecordDTO.Metrics data);
}