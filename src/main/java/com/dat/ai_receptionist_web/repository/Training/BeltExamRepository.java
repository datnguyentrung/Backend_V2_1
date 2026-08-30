package com.dat.ai_receptionist_web.repository.Training;

import com.dat.ai_receptionist_web.domain.Training.BeltExam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BeltExamRepository extends JpaRepository<BeltExam, UUID> {
}
