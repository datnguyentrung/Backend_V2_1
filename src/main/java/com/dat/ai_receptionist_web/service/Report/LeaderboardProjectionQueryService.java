package com.dat.ai_receptionist_web.service.Report;

import com.dat.ai_receptionist_web.domain.Core.Fitness;
import com.dat.ai_receptionist_web.domain.Core.Student;
import com.dat.ai_receptionist_web.domain.Skill.FitnessRecord;
import com.dat.ai_receptionist_web.dto.Report.LeaderboardMember;
import com.dat.ai_receptionist_web.dto.Report.YearlySummaryDTO;
import com.dat.ai_receptionist_web.enums.Core.StudentStatus;
import com.dat.ai_receptionist_web.enums.Skill.SkillLevel;
import com.dat.ai_receptionist_web.mapper.Report.YearlySummaryMapper;
import com.dat.ai_receptionist_web.repository.Core.FitnessRepository;
import com.dat.ai_receptionist_web.repository.Core.StudentRepository;
import com.dat.ai_receptionist_web.repository.Skill.FitnessRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeaderboardProjectionQueryService {
    private final StudentRepository studentRepository;
    private final StudentSummaryService studentSummaryService;
    private final YearlySummaryMapper yearlySummaryMapper;
    private final FitnessRecordRepository fitnessRecordRepository;
    private final FitnessRepository fitnessRepository;
    private final FitnessLeaderboardScorer fitnessLeaderboardScorer;

    @Transactional(readOnly = true)
    public ProjectionState loadConduct(String studentCode, int year, int quarter) {
        LeaderboardScope scope = LeaderboardScope.quarter(year, quarter);
        Student student = studentRepository.findByStudentCode(studentCode).orElse(null);
        if (!isActive(student)) {
            return ProjectionState.remove(scope, studentCode);
        }
        YearlySummaryDTO.QuarterSummary summary = studentSummaryService.getQuarterSummary(studentCode, year, quarter);
        YearlySummaryDTO.QuarterSummaryForRedis detail = yearlySummaryMapper.toQuarterSummaryForRedis(summary);
        return ProjectionState.upsert(scope, studentCode, detail.getTotalQuarterScore(), detail, student);
    }

    @Transactional(readOnly = true)
    public ProjectionState loadFitness(String studentCode, int year, int quarter, SkillLevel skillLevel) {
        LeaderboardScope scope = LeaderboardScope.fitness(year, quarter, skillLevel);
        Student student = studentRepository.findByStudentCode(studentCode).orElse(null);
        if (!isActive(student)) {
            return ProjectionState.remove(scope, studentCode);
        }
        List<FitnessRecord> records = fitnessRecordRepository.findRecordsForSingleStudent(
                year, quarter, skillLevel, studentCode
        );
        if (records.isEmpty()) {
            return ProjectionState.remove(scope, studentCode);
        }
        List<Fitness> benchmarks = fitnessRepository.findAllForProjection();
        FitnessLeaderboardScorer.ScoredMetrics best = fitnessLeaderboardScorer
                .selectBest(records, benchmarks)
                .orElseThrow();
        return ProjectionState.upsert(scope, studentCode, best.score(), best.metrics(), student);
    }

    @Transactional(readOnly = true)
    public MemberState loadMember(String studentCode) {
        Student student = studentRepository.findByStudentCode(studentCode).orElse(null);
        if (!isActive(student)) {
            return new MemberState(studentCode, false, null, null, null);
        }
        return new MemberState(
                studentCode,
                true,
                new LeaderboardMember(student.getPersonId(), student.getStudentCode(), student.getFullName(), student.getBelt()),
                student.getPersonId(),
                student.getFaceImagePath()
        );
    }

    private boolean isActive(Student student) {
        return student != null && student.getStudentStatus() == StudentStatus.ACTIVE;
    }

    public record ProjectionState(
            LeaderboardScope scope,
            String studentCode,
            boolean remove,
            double score,
            Object data,
            LeaderboardMember member,
            UUID personId,
            String faceImagePath
    ) {
        static ProjectionState remove(LeaderboardScope scope, String studentCode) {
            return new ProjectionState(scope, studentCode, true, 0, null, null, null, null);
        }

        static ProjectionState upsert(LeaderboardScope scope, String studentCode, double score, Object data, Student student) {
            LeaderboardMember member = new LeaderboardMember(
                    student.getPersonId(), student.getStudentCode(), student.getFullName(), student.getBelt());
            return new ProjectionState(scope, studentCode, false, score, data, member,
                    student.getPersonId(), student.getFaceImagePath());
        }
    }

    public record MemberState(
            String studentCode,
            boolean active,
            LeaderboardMember member,
            UUID personId,
            String faceImagePath
    ) {
    }
}
