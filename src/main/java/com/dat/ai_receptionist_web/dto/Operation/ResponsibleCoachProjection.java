package com.dat.ai_receptionist_web.dto.Operation;

import java.util.UUID;

public interface ResponsibleCoachProjection {
    UUID getAssignmentId();

    UUID getCoachPersonId();

    String getCoachName();
}
