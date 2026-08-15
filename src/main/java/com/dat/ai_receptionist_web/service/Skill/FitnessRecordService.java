package com.dat.ai_receptionist_web.service.Skill;

import com.dat.ai_receptionist_web.domain.Core.Coach;
import com.dat.ai_receptionist_web.domain.Core.Fitness;
import com.dat.ai_receptionist_web.domain.Core.Student;
import com.dat.ai_receptionist_web.domain.Skill.FitnessRecord;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Skill.FitnessRecordDTO;
import com.dat.ai_receptionist_web.enums.Skill.SkillLevel;
import com.dat.ai_receptionist_web.mapper.Skill.FitnessRecordMapper;
import com.dat.ai_receptionist_web.repository.Core.FitnessRepository;
import com.dat.ai_receptionist_web.repository.Skill.FitnessRecordRepository;
import com.dat.ai_receptionist_web.service.Core.CoachService;
import com.dat.ai_receptionist_web.service.Core.FitnessService;
import com.dat.ai_receptionist_web.service.Core.StudentService;
import com.dat.ai_receptionist_web.service.Projection.ProjectionOutboxService;
import com.dat.ai_receptionist_web.specification.FitnessRecordSpecification;
import com.dat.ai_receptionist_web.util.Helper.SkillCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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

    @Cacheable(value = "fitnessRecords", key = "#search + '-' + #skillLevel + '-' + #pageable.pageNumber + '-' + #pageable.pageSize", cacheManager = "redisCacheManager")
    public PageResponse<FitnessRecordDTO.Response> listFitnessRecords(
            String search, SkillLevel skillLevel, Pageable pageable) {

        // 1. LÃ¡ÂºÂ¥y dÃ¡Â»Â¯ liÃ¡Â»â€¡u phÃƒÂ¢n trang tÃ¡Â»Â« DB
        Specification<FitnessRecord> spec = Specification.where(FitnessRecordSpecification.hasSearch(search))
                .and(FitnessRecordSpecification.hasSkillLevel(skillLevel));
        Page<FitnessRecord> pageResult = fitnessRecordRepository.findAll(spec, pageable);

        // 2. LÃ¡ÂºÂ¥y toÃƒÂ n bÃ¡Â»â„¢ mÃ¡Â»â€˜c chuÃ¡ÂºÂ©n (HÃƒÂ m nÃƒÂ y Ã„â€˜ÃƒÂ£ cÃƒÂ³ @Cacheable nÃƒÂªn rÃ¡ÂºÂ¥t nhanh)
        List<Fitness> benchmarkList = fitnessService.getAllFitness();

        // 3. Map sang DTO vÃƒÂ  tÃƒÂ­nh toÃƒÂ¡n level cho tÃ¡Â»Â«ng record
        List<FitnessRecordDTO.Response> content = pageResult.getContent().stream()
                .map(entity -> {
                    return toResponseWithMetrics(entity, benchmarkList);
                })
                .toList();

        // 4. TrÃ¡ÂºÂ£ vÃ¡Â»Â PageResponse custom cÃ¡Â»Â§a bÃ¡ÂºÂ¡n
        return PageResponse.<FitnessRecordDTO.Response>builder()
                .content(content)
                .pageNumber(pageable.getPageNumber())
                .pageSize(pageable.getPageSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
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
