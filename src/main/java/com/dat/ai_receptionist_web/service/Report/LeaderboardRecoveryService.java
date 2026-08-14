package com.dat.ai_receptionist_web.service.Report;

import com.dat.ai_receptionist_web.domain.Core.Fitness;
import com.dat.ai_receptionist_web.domain.Core.Student;
import com.dat.ai_receptionist_web.domain.Skill.FitnessRecord;
import com.dat.ai_receptionist_web.dto.Report.LeaderboardMember;
import com.dat.ai_receptionist_web.dto.Report.YearlySummaryDTO;
import com.dat.ai_receptionist_web.enums.Core.StudentStatus;
import com.dat.ai_receptionist_web.mapper.Report.YearlySummaryMapper;
import com.dat.ai_receptionist_web.repository.Core.StudentRepository;
import com.dat.ai_receptionist_web.repository.Report.LeaderboardLockRepository;
import com.dat.ai_receptionist_web.repository.Skill.FitnessRecordRepository;
import com.dat.ai_receptionist_web.service.Core.FitnessService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class LeaderboardRecoveryService {
    private static final int PAGE_SIZE = 500;

    private final LeaderboardLockRepository lockRepository;
    private final StudentRepository studentRepository;
    private final StudentSummaryService studentSummaryService;
    private final YearlySummaryMapper yearlySummaryMapper;
    private final FitnessRecordRepository fitnessRecordRepository;
    private final FitnessService fitnessService;
    private final FitnessLeaderboardScorer fitnessLeaderboardScorer;
    private final LeaderboardRedisStore redisStore;
    private final MeterRegistry meterRegistry;

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public RebuildResult rebuild(LeaderboardScope scope) {
        Timer.Sample timer = Timer.start(meterRegistry);
        String generation = redisStore.startRebuild();
        log.info("LEADERBOARD_REBUILD_STARTED scope={} generation={}", scope.registryValue(), generation);
        try {
            lockRepository.lock(scope.registryValue());
            int entries = scope.type() == LeaderboardScope.Type.QUARTER
                    ? rebuildQuarter(scope, generation)
                    : rebuildFitness(scope, generation);
            int stored = redisStore.completeRebuild(scope, generation, entries);
            meterRegistry.counter("leaderboard.rebuild", "type", scope.type().name().toLowerCase(), "result", "success")
                    .increment();
            log.info("LEADERBOARD_REBUILD_COMPLETED scope={} generation={} entries={}",
                    scope.registryValue(), generation, stored);
            return new RebuildResult(scope, generation, stored);
        } catch (RuntimeException exception) {
            try {
                redisStore.abortRebuild(scope, generation);
            } catch (RuntimeException cleanupFailure) {
                exception.addSuppressed(cleanupFailure);
            }
            meterRegistry.counter("leaderboard.rebuild", "type", scope.type().name().toLowerCase(), "result", "failure")
                    .increment();
            log.error("LEADERBOARD_REBUILD_FAILED scope={} generation={}", scope.registryValue(), generation, exception);
            throw exception;
        } finally {
            timer.stop(meterRegistry.timer("leaderboard.rebuild.duration", "type", scope.type().name().toLowerCase()));
        }
    }

    private int rebuildQuarter(LeaderboardScope scope, String generation) {
        int pageNumber = 0;
        int totalEntries = 0;
        Page<Student> page;
        do {
            page = activeStudents(pageNumber++);
            Map<String, YearlySummaryDTO.QuarterSummary> summaries = studentSummaryService
                    .calculateBatchQuarterSummary(page.getContent(), scope.year(), scope.quarter());
            List<LeaderboardRedisStore.ProjectionEntry> entries = page.getContent().stream()
                    .map(student -> {
                        YearlySummaryDTO.QuarterSummaryForRedis detail = yearlySummaryMapper
                                .toQuarterSummaryForRedis(summaries.get(student.getStudentCode()));
                        return entry(student, detail.getTotalQuarterScore(), detail);
                    })
                    .toList();
            redisStore.appendRebuildBatch(scope, generation, entries);
            totalEntries += entries.size();
        } while (page.hasNext());
        return totalEntries;
    }

    private int rebuildFitness(LeaderboardScope scope, String generation) {
        List<Fitness> benchmarks = fitnessService.getAllFitness();
        int pageNumber = 0;
        int totalEntries = 0;
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

            List<LeaderboardRedisStore.ProjectionEntry> entries = new ArrayList<>();
            for (Student student : page.getContent()) {
                fitnessLeaderboardScorer.selectBest(
                                byStudent.getOrDefault(student.getStudentCode(), List.of()), benchmarks
                        )
                        .ifPresent(best -> entries.add(entry(student, best.score(), best.metrics())));
            }
            redisStore.appendRebuildBatch(scope, generation, entries);
            totalEntries += entries.size();
        } while (page.hasNext());
        return totalEntries;
    }

    private Page<Student> activeStudents(int pageNumber) {
        return studentRepository.findAllByStudentStatus(
                StudentStatus.ACTIVE,
                PageRequest.of(pageNumber, PAGE_SIZE, Sort.by("studentCode").ascending())
        );
    }

    private LeaderboardRedisStore.ProjectionEntry entry(Student student, double score, Object data) {
        return new LeaderboardRedisStore.ProjectionEntry(
                student.getStudentCode(),
                score,
                data,
                new LeaderboardMember(student.getPersonId(), student.getStudentCode(), student.getFullName(), student.getBelt())
        );
    }

    public record RebuildResult(LeaderboardScope scope, String generation, int entries) {
    }
}
