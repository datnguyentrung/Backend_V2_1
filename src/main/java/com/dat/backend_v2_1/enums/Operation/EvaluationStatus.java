package com.dat.backend_v2_1.enums.Operation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum EvaluationStatus {
    PENDING("P"),       // Chờ đánh giá
    GOOD("T"),          // Tốt
    AVERAGE("TB"),      // Trung bình
    WEAK("Y");          // Yếu

    private final String displayName;

    EvaluationStatus(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName(){return displayName;}

    @JsonCreator
    public static EvaluationStatus fromValue(String value){
        if (value.equals("P")){
            return PENDING;
        }
        for (EvaluationStatus status : EvaluationStatus.values()){
            if (status.getDisplayName().equals(value)){
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid Evaluation Status value: " + value);
    }
}
