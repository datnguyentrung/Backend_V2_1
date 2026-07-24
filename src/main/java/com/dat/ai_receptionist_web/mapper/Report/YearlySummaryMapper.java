package com.dat.ai_receptionist_web.mapper.Report;

import com.dat.ai_receptionist_web.dto.Operation.StudentAttendanceDTO;
import com.dat.ai_receptionist_web.dto.Report.YearlySummaryDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface YearlySummaryMapper {

    @Mapping(target = ".", source = "attendanceStats")
        // Lôi toàn bộ field trong stats ra ngoài class cha
    YearlySummaryDTO.QuarterSummaryForRedis toQuarterSummaryForRedis(YearlySummaryDTO.QuarterSummary quarterSummary);

    @Mapping(target = "attendanceStats", source = "quarterSummaryForRedis")
    YearlySummaryDTO.QuarterSummary toQuarterSummary(YearlySummaryDTO.QuarterSummaryForRedis quarterSummaryForRedis);

    // Thêm phương thức này để ép MapStruct tạo object AttendanceStats mới
    // Nó sẽ chỉ copy những trường có trong AttendanceStats và bỏ qua trường của RankItem
    StudentAttendanceDTO.AttendanceStats toAttendanceStats(YearlySummaryDTO.QuarterSummaryForRedis source);
}
