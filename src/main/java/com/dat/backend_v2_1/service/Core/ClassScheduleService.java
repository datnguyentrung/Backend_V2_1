package com.dat.backend_v2_1.service.Core;

import com.dat.backend_v2_1.domain.Core.ClassSchedule;
import com.dat.backend_v2_1.repository.Core.ClassScheduleRepository;
import com.nimbusds.oauth2.sdk.util.CollectionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClassScheduleService {
    private final ClassScheduleRepository classScheduleRepository;

    public ClassSchedule getClassScheduleById(String scheduleId) {
        return classScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> {
                    log.error("Class schedule not found with id: {}", scheduleId);
                    return new RuntimeException("Class schedule not found");
                });
    }

    public List<ClassSchedule> findByScheduleIds(List<String> scheduleIds) {
        if (CollectionUtils.isEmpty(scheduleIds)) {
            return Collections.emptyList();
        }
        return classScheduleRepository.findAllById(scheduleIds);
    }
}
