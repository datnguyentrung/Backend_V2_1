package com.dat.ai_receptionist_web.dto.Report;

import com.dat.ai_receptionist_web.enums.Core.Belt;

import java.util.UUID;

public record LeaderboardMember(UUID personId, String studentCode, String fullName, Belt belt) {
}
