package com.dat.ai_receptionist_web.service.Catalog;

import com.dat.ai_receptionist_web.repository.Catalog.ClassScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClassScheduleService {
    private final ClassScheduleRepository classScheduleRepository;


}
