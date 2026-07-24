package com.dat.ai_receptionist_web.service.Skill;

import com.dat.ai_receptionist_web.domain.Core.Coach;
import com.dat.ai_receptionist_web.domain.Core.Fitness;
import com.dat.ai_receptionist_web.domain.Core.Student;
import com.dat.ai_receptionist_web.domain.Skill.FitnessRecord;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Skill.FitnessRecordDTO;
import com.dat.ai_receptionist_web.enums.Skill.SkillLevel;
import com.dat.ai_receptionist_web.mapper.Skill.FitnessRecordMapper;
import com.dat.ai_receptionist_web.repository.Skill.FitnessRecordRepository;
import com.dat.ai_receptionist_web.service.Core.CoachService;
import com.dat.ai_receptionist_web.service.Core.FitnessService;
import com.dat.ai_receptionist_web.service.Core.StudentService;
import com.dat.ai_receptionist_web.service.Report.LeaderboardService;
import com.dat.ai_receptionist_web.specification.FitnessRecordSpecification;
import com.dat.ai_receptionist_web.util.Helper.SkillCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FitnessRecordService {
    private final FitnessRecordRepository fitnessRecordRepository;
    private final StudentService studentService;
    private final CoachService coachService;
    private final FitnessRecordMapper fitnessRecordMapper;
    private final FitnessService fitnessService;
    private final SkillCalculator skillCalculator;
    private final LeaderboardService leaderboardService;

    @Transactional
    @CacheEvict(value = "fitnessRecords", allEntries = true) // Xóa sạch cache danh sách khi có record mới
    public FitnessRecordDTO.Response createFitnessRecord(
            FitnessRecordDTO.CreateRequest request) {
        Student student = studentService.getStudentByStudentCode(request.getStudentCode());

        Coach currentCoach = coachService.getCoachByStaffCode(request.getStaffCode());

        FitnessRecord fitnessRecord = FitnessRecord.builder()
                .assessmentDate(request.getAssessmentDate())
                .student(student)
                .duration(request.getDuration())
                .amount(request.getAmount())
                .skillLevel(request.getSkillLevel())
                .recordByCoach(currentCoach)
                .build();

        // 3. Lưu vào database
        FitnessRecord savedRecord = fitnessRecordRepository.save(fitnessRecord);

        FitnessRecordDTO.Response response = fitnessRecordMapper.toResponse(savedRecord);

        // 1. Tính toán level
        List<Fitness> benchmarks = fitnessService.getAllFitness();

        // 2. Hàm này trả về final fitness level, hãy gán nó vào response
        FitnessRecordDTO.Metrics metrics = response.getMetrics();

        // 3. Check an toàn tuyệt đối (Chống NullPointerException)
        if (metrics != null) {
            // Hàm này vừa xử lý gán durationLevel/amountLevel bên trong, vừa trả ra finalLevel
            int finalLevel = skillCalculator.calculateAndSetLevels(metrics, benchmarks);

            // Gán final level vào
            metrics.setFitnessLevel(finalLevel);
        } else {
            // (Tuỳ chọn) Ghi log cảnh báo nếu dữ liệu bị thiếu một cách bất thường
            log.warn("⚠️ Không tìm thấy object Metrics trong response của học viên này!");
        }

        // 2. Cập nhật Leaderboard (Nhớ đổi tên hàm cho khớp với Service)
        leaderboardService.updateFitnessLeaderboard(response, student.getStudentCode());

        return response;
    }

    @Cacheable(value = "fitnessRecords", key = "#search + '-' + #skillLevel + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public PageResponse<FitnessRecordDTO.Response> listFitnessRecords(
            String search, SkillLevel skillLevel, Pageable pageable) {

        // 1. Lấy dữ liệu phân trang từ DB
        Specification<FitnessRecord> spec = Specification.where(FitnessRecordSpecification.hasSearch(search))
                .and(FitnessRecordSpecification.hasSkillLevel(skillLevel));
        Page<FitnessRecord> pageResult = fitnessRecordRepository.findAll(spec, pageable);

        // 2. Lấy toàn bộ mốc chuẩn (Hàm này đã có @Cacheable nên rất nhanh)
        List<Fitness> benchmarkList = fitnessService.getAllFitness();

        // 3. Map sang DTO và tính toán level cho từng record
        List<FitnessRecordDTO.Response> content = pageResult.getContent().stream()
                .map(entity -> {
                    FitnessRecordDTO.Response dto = fitnessRecordMapper.toResponse(entity);

                    // Check an toàn trước khi gọi
                    if (dto.getMetrics() != null) {
                        int finalLevel = skillCalculator.calculateAndSetLevels(dto.getMetrics(), benchmarkList);
                        dto.getMetrics().setFitnessLevel(finalLevel);
                    }

                    return dto;
                })
                .toList();

        // 4. Trả về PageResponse custom của bạn
        return PageResponse.<FitnessRecordDTO.Response>builder()
                .content(content)
                .pageNumber(pageable.getPageNumber())
                .pageSize(pageable.getPageSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }
}
