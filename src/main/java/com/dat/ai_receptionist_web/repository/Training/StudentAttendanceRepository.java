package com.dat.ai_receptionist_web.repository.Training;

import com.dat.ai_receptionist_web.domain.Training.StudentAttendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StudentAttendanceRepository extends JpaRepository<StudentAttendance, UUID> {
    boolean existsByClassSession_ClassSessionIdAndStudentEnrollment_StudentEnrollmentId(
            UUID classSessionId, UUID studentEnrollmentId);

    List<StudentAttendance> findByClassSession_ClassSessionId(UUID classSessionId);
}
