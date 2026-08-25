package com.dat.ai_receptionist_web.mapper.Training;

import com.dat.ai_receptionist_web.domain.Training.StudentEnrollment;
import com.dat.ai_receptionist_web.dto.Training.StudentEnrollmentDTO;
import org.springframework.stereotype.Component;

@Component
public class StudentEnrollmentMapper {
    public StudentEnrollmentDTO.Response toResponse(StudentEnrollment entity) {
        if (entity == null) return null;
        return new StudentEnrollmentDTO.Response(entity.getStudentEnrollmentId(), entity.getStudentPerson() == null ? null : entity.getStudentPerson().getPersonId(), entity.getCoursePurchase() == null ? null : entity.getCoursePurchase().getCoursePurchaseId(), entity.getClassSchedule() == null ? null : entity.getClassSchedule().getScheduleId(), entity.getStartDate(), entity.getEndDate(), entity.getStatus(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public void updateEntity(StudentEnrollmentDTO.UpdateRequest request, StudentEnrollment entity) {
        entity.setStartDate(request.startDate());
        entity.setEndDate(request.endDate());
        entity.setStatus(request.status());
    }
}
