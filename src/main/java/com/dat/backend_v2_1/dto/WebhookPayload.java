package com.dat.backend_v2_1.dto;

import com.dat.backend_v2_1.dto.Skill.FitnessRecordDTO;
import lombok.Data;

@Data
public class WebhookPayload {
    private String action; // "INSERT", "UPDATE", "DELETE"
    private String studentCode;
    private int year;
    private int quarter;
    private String skillLevel;
    private FitnessRecordDTO.Metrics metrics; // Chứa duration, amount,...
}