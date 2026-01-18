package com.dat.backend_v2_1.enums.Core;

public enum EvaluationStatus {
    PENDING("P"),       // Chờ đánh giá
    GOOD("T"),          // Tốt
    AVERAGE("TB"),      // Trung bình
    WEAK("Y");          // Yếu

    private final String displayName;

    EvaluationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
