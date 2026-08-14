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
import com.dat.ai_receptionist_web.service.Core.PersonAvatarUrlCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaderboardProjectionUpdater {
    private final LeaderboardLockRepository lockRepository;
    private final StudentRepository studentRepository;
    private final StudentSummaryService studentSummaryService;
    private final YearlySummaryMapper yearlySummaryMapper;
    private final FitnessRecordRepository fitnessRecordRepository;
    private final FitnessService fitnessService;
    private final FitnessLeaderboardScorer fitnessLeaderboardScorer;
    private final LeaderboardRedisStore redisStore;
    private final PersonAvatarUrlCacheService avatarUrlCacheService;

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void refreshQuarter(String studentCode, int year, int quarter) {
        LeaderboardScope scope = LeaderboardScope.quarter(year, quarter);
        lockRepository.lock(scope.registryValue());
        Student student = studentRepository.findByStudentCode(studentCode).orElse(null);
        if (!isActive(student)) {
            redisStore.remove(scope, studentCode);
            return;
        }

        YearlySummaryDTO.QuarterSummary summary = studentSummaryService.getQuarterSummary(studentCode, year, quarter);
        YearlySummaryDTO.QuarterSummaryForRedis detail = yearlySummaryMapper.toQuarterSummaryForRedis(summary);
        redisStore.upsert(scope, studentCode, detail.getTotalQuarterScore(), detail, memberOf(student));
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void refreshFitness(String studentCode, int year, int quarter,
                               com.dat.ai_receptionist_web.enums.Skill.SkillLevel skillLevel) {
        LeaderboardScope scope = LeaderboardScope.fitness(year, quarter, skillLevel);
        lockRepository.lock(scope.registryValue());
        Student student = studentRepository.findByStudentCode(studentCode).orElse(null);
        if (!isActive(student)) {
            redisStore.remove(scope, studentCode);
            return;
        }

        List<FitnessRecord> records = fitnessRecordRepository.findRecordsForSingleStudent(
                year, quarter, skillLevel, studentCode
        );
        if (records.isEmpty()) {
            redisStore.remove(scope, studentCode);
            return;
        }

        List<Fitness> benchmarks = fitnessService.getAllFitness();
        FitnessLeaderboardScorer.ScoredMetrics best = fitnessLeaderboardScorer
                .selectBest(records, benchmarks)
                .orElseThrow();
        redisStore.upsert(scope, studentCode, best.score(), best.metrics(), memberOf(student));
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void refreshMember(String studentCode, LeaderboardScope scope) {
        lockRepository.lock(scope.registryValue());
        studentRepository.findByStudentCode(studentCode)
                .filter(this::isActive)
                .map(this::memberOf)
                .ifPresent(member -> redisStore.updateMemberIfPresent(scope, member));
    }

    private boolean isActive(Student student) {
        return student != null && student.getStudentStatus() == StudentStatus.ACTIVE;
    }

    private LeaderboardMember memberOf(Student student) {
        avatarUrlCacheService.putFromFaceImagePath(student.getPersonId(), student.getFaceImagePath());
        return new LeaderboardMember(student.getPersonId(), student.getStudentCode(), student.getFullName(), student.getBelt());
    }
}
