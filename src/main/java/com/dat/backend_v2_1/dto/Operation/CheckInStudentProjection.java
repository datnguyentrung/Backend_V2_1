package com.dat.backend_v2_1.dto.Operation;

import com.dat.backend_v2_1.enums.Core.StudentStatus;

import java.util.UUID;

public interface CheckInStudentProjection {
    UUID getPersonId();

    String getStudentCode();

    StudentStatus getStudentStatus();

    String getFullName();
}
