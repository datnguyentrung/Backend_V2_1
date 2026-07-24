package com.dat.ai_receptionist_web.dto.Operation;

import com.dat.ai_receptionist_web.enums.Core.StudentStatus;

import java.util.UUID;

public interface CheckInStudentProjection {
    UUID getPersonId();

    String getStudentCode();

    StudentStatus getStudentStatus();

    String getFullName();
}
