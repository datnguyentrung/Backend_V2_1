package com.dat.ai_receptionist_web.mapper.Training;

import com.dat.ai_receptionist_web.domain.Training.StudentAttendance;
import com.dat.ai_receptionist_web.dto.Training.StudentAttendanceDTO;
import org.springframework.stereotype.Component;

@Component
public class StudentAttendanceMapper {
    public StudentAttendanceDTO.Response toResponse(StudentAttendance entity) {
        if (entity == null) return null;
        return new StudentAttendanceDTO.Response(entity.getStudentAttendanceId(), entity.getClassSession() == null ? null : entity.getClassSession().getClassSessionId(), entity.getStudentEnrollment() == null ? null : entity.getStudentEnrollment().getStudentEnrollmentId(), entity.getEvaluatedByCoach() == null ? null : entity.getEvaluatedByCoach().getPersonId(), entity.getCheckInTime(), entity.getAttendanceStatus(), entity.getEvaluationStatus(), entity.getNote(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public void updateEntity(StudentAttendanceDTO.UpdateRequest request, StudentAttendance entity) {
        entity.setCheckInTime(request.checkInTime());
        entity.setAttendanceStatus(request.attendanceStatus());
        entity.setEvaluationStatus(request.evaluationStatus());
        entity.setNote(request.note());
    }
}
