package com.dat.ai_receptionist_web.mapper.Training;

import com.dat.ai_receptionist_web.domain.Training.ClassSession;
import com.dat.ai_receptionist_web.dto.Training.ClassSessionDTO;
import org.springframework.stereotype.Component;

@Component
public class ClassSessionMapper {
    public ClassSessionDTO.Response toResponse(ClassSession entity) {
        if (entity == null) return null;
        return new ClassSessionDTO.Response(entity.getClassSessionId(), entity.getCourse() == null ? null : entity.getCourse().getCourseId(), entity.getSessionDate(), entity.getStatus(), entity.isAttendanceClosed(), entity.getStartTime(), entity.getEndTime(), entity.getNote());
    }

    public void updateEntity(ClassSessionDTO.UpdateRequest request, ClassSession entity) {
        entity.setSessionDate(request.sessionDate());
        entity.setStatus(request.status());
        entity.setAttendanceClosed(request.attendanceClosed());
        entity.setStartTime(request.startTime());
        entity.setEndTime(request.endTime());
        entity.setNote(request.note());
    }
}
