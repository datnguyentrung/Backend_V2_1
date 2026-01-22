package com.dat.backend_v2_1.service.Core;

import com.dat.backend_v2_1.domain.Core.Coach;
import com.dat.backend_v2_1.repository.Core.CoachRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CoachService {
    private final CoachRepository coachRepository;

    public Coach getCoachById(String coachId){
        return coachRepository.findById(UUID.fromString(coachId))
                .orElseThrow(() -> new IllegalArgumentException("Coach with id " + coachId + " not found"));
    }

    public Coach getCoachById(UUID coachId){
        return coachRepository.findById(coachId)
                .orElseThrow(() -> new IllegalArgumentException("Coach with id " + coachId + " not found"));
    }
}
