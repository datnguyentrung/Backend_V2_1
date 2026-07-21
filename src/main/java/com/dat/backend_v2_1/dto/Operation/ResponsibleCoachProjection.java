package com.dat.backend_v2_1.dto.Operation;

import java.util.UUID;

public interface ResponsibleCoachProjection {
    UUID getAssignmentId();

    UUID getCoachPersonId();

    String getCoachName();
}
