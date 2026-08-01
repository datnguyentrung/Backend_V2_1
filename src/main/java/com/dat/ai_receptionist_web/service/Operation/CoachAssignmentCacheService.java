package com.dat.ai_receptionist_web.service.Operation;

import com.dat.ai_receptionist_web.dto.Operation.CoachAssignmentResDTO;
import com.dat.ai_receptionist_web.enums.ErrorCode;
import com.dat.ai_receptionist_web.enums.Operation.CoachAssignmentStatus;
import com.dat.ai_receptionist_web.mapper.Operation.CoachAssignmentMapper;
import com.dat.ai_receptionist_web.repository.Core.CoachRepository;
import com.dat.ai_receptionist_web.repository.Operation.CoachAssignmentRepository;
import com.dat.ai_receptionist_web.util.error.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Owns the single canonical cache payload for coach assignments. Keeping this
 * in a separate bean ensures @Cacheable is invoked through a Spring proxy.
 */
@Service
@RequiredArgsConstructor
public class CoachAssignmentCacheService {

    private final CoachAssignmentRepository coachAssignmentRepository;
    private final CoachRepository coachRepository;
    private final CoachAssignmentMapper coachAssignmentMapper;

    @Cacheable(value = "coachAssignments", key = "#coachId.toString() + '_' + #status.name()", unless = "#result == null || #result.isEmpty()", cacheManager = "redisCacheManager")
    @Transactional(readOnly = true)
    public List<CoachAssignmentResDTO.CacheResponse> findByCoachId(UUID coachId, CoachAssignmentStatus status) {
        coachRepository.findById(coachId).orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));
        return coachAssignmentRepository.findByCoach_PersonIdAndStatusWithClassSchedule(coachId, status)
                .stream()
                .map(coachAssignmentMapper::toCacheResponse)
                .toList();
    }
}
