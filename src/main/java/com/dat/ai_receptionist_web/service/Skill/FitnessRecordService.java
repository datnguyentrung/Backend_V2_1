package com.dat.ai_receptionist_web.service.Skill;

import com.dat.ai_receptionist_web.domain.Core.Coach;
import com.dat.ai_receptionist_web.domain.Core.Fitness;
import com.dat.ai_receptionist_web.domain.Core.Student;
import com.dat.ai_receptionist_web.domain.Skill.FitnessRecord;
import com.dat.ai_receptionist_web.dto.Core.StudentResDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Skill.FitnessRecordDTO;
import com.dat.ai_receptionist_web.enums.Skill.SkillLevel;
import com.dat.ai_receptionist_web.mapper.Skill.FitnessRecordMapper;
import com.dat.ai_receptionist_web.repository.Core.FitnessRepository;
import com.dat.ai_receptionist_web.repository.Skill.FitnessRecordRepository;
import com.dat.ai_receptionist_web.repository.Skill.FitnessRecordRepository.FitnessRecordListRow;
import com.dat.ai_receptionist_web.service.Core.CoachService;
import com.dat.ai_receptionist_web.service.Core.FitnessService;
import com.dat.ai_receptionist_web.service.Core.StudentService;
import com.dat.ai_receptionist_web.service.Projection.ProjectionOutboxService;
import com.dat.ai_receptionist_web.util.Helper.SkillCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FitnessRecordService {
    private final FitnessRecordRepository fitnessRecordRepository;
    private final FitnessRepository fitnessRepository;
    private final StudentService studentService;
    private final CoachService coachService;
    private final FitnessRecordMapper fitnessRecordMapper;
    private final FitnessService fitnessService;
    private final SkillCalculator skillCalculator;
    private final ProjectionOutboxService projectionOutboxService;

    @Transactional
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

        // 3. LÃ†Â°u vÃƒÂ o database
        FitnessRecord savedRecord = fitnessRecordRepository.save(fitnessRecord);

        FitnessRecordDTO.Response response = toResponseWithMetrics(savedRecord, fitnessRepository.findAllForProjection());

        markLeaderboardDirty(student.getStudentCode(), scopeOf(savedRecord));

        return response;
    }

    @Transactional
    public FitnessRecordDTO.Response updateFitnessRecord(Long id, FitnessRecordDTO.UpdateRequest request) {
        FitnessRecord record = findRecord(id);
        FitnessScope oldScope = scopeOf(record);

        record.setAssessmentDate(request.getAssessmentDate());
        record.setDuration(request.getDuration());
        record.setAmount(request.getAmount());
        record.setSkillLevel(request.getSkillLevel());

        FitnessRecord savedRecord = fitnessRecordRepository.save(record);
        FitnessRecordDTO.Response response = toResponseWithMetrics(savedRecord, fitnessRepository.findAllForProjection());
        markLeaderboardDirty(record.getStudent().getStudentCode(), oldScope, scopeOf(savedRecord));
        return response;
    }

    @Transactional
    public void deleteFitnessRecord(Long id) {
        FitnessRecord record = findRecord(id);
        String studentCode = record.getStudent().getStudentCode();
        FitnessScope oldScope = scopeOf(record);
        fitnessRecordRepository.delete(record);
        markLeaderboardDirty(studentCode, oldScope);
    }

    @Cacheable(
            value = "fitnessRecords",
            key = "#search + '-' + #skillLevel + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort",
            cacheManager = "redisCacheManager"
    )
    public PageResponse<FitnessRecordDTO.ListResponse> listFitnessRecords(
            String search, SkillLevel skillLevel, Pageable pageable) {

        // 1. Lấy dữ liệu phân trang từ DB
        Page<FitnessRecordListRow> pageResult = fitnessRecordRepository.findListRows(
                normalizeSearch(search),
                skillLevel,
                pageable
        );

        // 2. Lấy toàn bộ mức chuẩn (Hàm này đã có @Cacheable nên rất nhanh)
        List<Fitness> benchmarkList = fitnessService.getAllFitness();

        // 3. Map sang DTO và tính toán level cho từng record
        List<FitnessRecordDTO.ListResponse> content = pageResult.getContent().stream()
                .map(row -> toListResponse(row, benchmarkList))
                .toList();

        // 4. Trả về PageResponse custom của bạn
        return PageResponse.<FitnessRecordDTO.ListResponse>builder()
                .content(content)
                .pageNumber(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .first(pageResult.isFirst())
                .last(pageResult.isLast())
                .empty(pageResult.isEmpty())
                .build();
    }

    private FitnessRecord findRecord(Long id) {
        return fitnessRecordRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Fitness record not found: " + id));
    }

    private FitnessRecordDTO.Response toResponseWithMetrics(FitnessRecord record, List<Fitness> benchmarks) {
        FitnessRecordDTO.Response response = fitnessRecordMapper.toResponse(record);
        if (response.getMetrics() != null) {
            response.getMetrics().setFitnessLevel(
                    skillCalculator.calculateAndSetLevels(response.getMetrics(), benchmarks)
            );
        }
        return response;
    }

    private FitnessRecordDTO.ListResponse toListResponse(FitnessRecordListRow row, List<Fitness> benchmarks) {
        FitnessRecordDTO.Metrics metrics = FitnessRecordDTO.Metrics.builder()
                .createdAt(row.getCreatedAt())
                .assessmentDate(row.getAssessmentDate())
                .duration(row.getDuration())
                .amount(row.getAmount())
                .skillLevel(row.getSkillLevel())
                .build();
        metrics.setFitnessLevel(skillCalculator.calculateAndSetLevels(metrics, benchmarks));

        return FitnessRecordDTO.ListResponse.builder()
                .id(row.getId())
                .studentSummary(StudentResDTO.StudentSummary.builder()
                        .personId(row.getStudentPersonId())
                        .fullName(row.getStudentFullName())
                        .code(row.getStudentCode())
                        .belt(row.getStudentBelt())
                        .build())
                .metrics(FitnessRecordDTO.ListMetrics.builder()
                        .assessmentDate(metrics.getAssessmentDate())
                        .duration(metrics.getDuration())
                        .amount(metrics.getAmount())
                        .skillLevel(metrics.getSkillLevel())
                        .durationLevel(metrics.getDurationLevel())
                        .amountLevel(metrics.getAmountLevel())
                        .fitnessLevel(metrics.getFitnessLevel())
                        .isQualified(metrics.getIsQualified())
                        .build())
                .build();
    }

    private String normalizeSearch(String search) {
        return search == null ? null : search.trim();
    }

    private FitnessScope scopeOf(FitnessRecord record) {
        LocalDate date = record.getAssessmentDate();
        return new FitnessScope(
                date.getYear(),
                (date.getMonthValue() - 1) / 3 + 1,
                record.getSkillLevel()
        );
    }

    private void markLeaderboardDirty(String studentCode, FitnessScope... scopes) {
        Set<FitnessScope> affectedScopes = new LinkedHashSet<>(List.of(scopes));
        for (FitnessScope scope : affectedScopes) {
            projectionOutboxService.markFitnessDirty(studentCode, scope.year(), scope.quarter(), scope.skillLevel());
        }
        projectionOutboxService.markFitnessRecordsCacheDirty();
    }

    private record FitnessScope(int year, int quarter, SkillLevel skillLevel) {
    }
}
