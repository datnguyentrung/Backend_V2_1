package com.dat.backend_v2_1.dto;

import lombok.Data;

@Data
public class WebhookPayload<T> {
    private String action; // "INSERT", "UPDATE", "DELETE"
    private String studentCode;
    private int year;
    private int quarter;
    private String skillLevel;
    private T data; // Chứa duration, amount,...
}