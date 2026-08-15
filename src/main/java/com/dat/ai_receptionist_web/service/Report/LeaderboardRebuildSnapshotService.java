package com.dat.ai_receptionist_web.service.Report;

import com.dat.ai_receptionist_web.domain.Core.Fitness;
import com.dat.ai_receptionist_web.domain.Core.Student;
import com.dat.ai_receptionist_web.domain.Skill.FitnessRecord;
import com.dat.ai_receptionist_web.dto.Report.LeaderboardMember;
import com.dat.ai_receptionist_web.dto.Report.YearlySummaryDTO;
import com.dat.ai_receptionist_web.enums.Core.StudentStatus;
import com.dat.ai_receptionist_web.mapper.Report.YearlySummaryMapper;
import com.dat.ai_receptionist_web.repository.Core.FitnessRepository;
import com.dat.ai_receptionist_web.repository.Core.StudentRepository;
import com.dat.ai_receptionist_web.repository.Skill.FitnessRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LeaderboardRebuildSnapshotService {
    private static final int PAGE_SIZE = 500;

    private final StudentRepository studentRepository;
    private final StudentSummaryService studentSummaryService;
    private final YearlySummaryMapper yearlySummaryMapper;
    private final FitnessRecordRepository fitnessRecordRepository;
    private final FitnessRepository fitnessRepository;
    private final FitnessLeaderboardScorer fitnessLeaderboardScorer;

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<LeaderboardRedisStore.ProjectionEntry> load(LeaderboardScope scope) {
        return scope.type() == LeaderboardScope.Type.QUARTER
                ? loadQuarter(scope)
                : loadFitness(scope);
    }

    private List<LeaderboardRedisStore.ProjectionEntry> loadQuarter(LeaderboardScope scope) {
        List<LeaderboardRedisStore.ProjectionEntry> result = new ArrayList<>();
        int pageNumber = 0;
        Page<Student> page;
        do {
            page = activeStudents(pageNumber++);
            Map<String, YearlySummaryDTO.QuarterSummary> summaries = studentSummaryService
                    .calculateBatchQuarterSummary(page.getContent(), scope.year(), scope.quarter());
            for (Student student : page.getContent()) {
                YearlySummaryDTO.QuarterSummaryForRedis detail = yearlySummaryMapper
                        .toQuarterSummaryForRedis(summaries.get(student.getStudentCode()));
                result.add(entry(student, detail.getTotalQuarterScore(), detail));
            }
        } while (page.hasNext());
        return result;
    }

    private List<LeaderboardRedisStore.ProjectionEntry> loadFitness(LeaderboardScope scope) {
        List<Fitness> benchmarks = fitnessRepository.findAllForProjection();
        List<LeaderboardRedisStore.ProjectionEntry> result = new ArrayList<>();
        int pageNumber = 0;
        Page<Student> page;
        do {
            page = activeStudents(pageNumber++);
            List<String> studentCodes = page.getContent().stream().map(Student::getStudentCode).toList();
            List<FitnessRecord> records = studentCodes.isEmpty()
                    ? List.of()
                    : fitnessRecordRepository.findRecordsForQuarterAndStudents(
                            scope.year(), scope.quarter(), scope.skillLevel(), studentCodes
                    );
            Map<String, List<FitnessRecord>> byStudent = new HashMap<>();
            for (FitnessRecord record : records) {
                byStudent.computeIfAbsent(record.getStudent().getStudentCode(), ignored -> new ArrayList<>()).add(record);
            }
            for (Student student : page.getContent()) {
                fitnessLeaderboardScorer.selectBest(
                                byStudent.getOrDefault(student.getStudentCode(), List.of()), benchmarks
                        )
                        .ifPresent(best -> result.add(entry(student, best.score(), best.metrics())));
            }
        } while (page.hasNext());
        return result;
    }

    private Page<Student> activeStudents(int pageNumber) {
        return studentRepository.findAllByStudentStatus(
                StudentStatus.ACTIVE,
                PageRequest.of(pageNumber, PAGE_SIZE, Sort.by("studentCode").ascending())
        );
    }

    private LeaderboardRedisStore.ProjectionEntry entry(Student student, double score, Object data) {
        return new LeaderboardRedisStore.ProjectionEntry(
                student.getStudentCode(), score, data,
                new LeaderboardMember(student.getPersonId(), student.getStudentCode(), student.getFullName(), student.getBelt())
        );
    }
}
