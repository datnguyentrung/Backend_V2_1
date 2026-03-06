package com.dat.backend_v2_1.enums.Operation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum AttendanceStatus {
    PRESENT("X"),       // Có mặt
    ABSENT("V"),        // Vắng mặt (Không phép)
    EXCUSED("P"),       // Vắng có phép (Permission)
    MAKEUP("B"),        // Học bù
    LATE("M");          // Đi muộn

    private final String code;

    AttendanceStatus(String code) {
        this.code = code;
    }

    // @JsonValue: Khi Backend trả về JSON cho FE, nó sẽ trả về code ("X") thay vì tên Enum ("PRESENT")
    @JsonValue
    public String getCode() {
        return code;
    }

    // @JsonCreator: Khi FE gửi JSON lên ("X"), Jackson dùng hàm này để tìm ra Enum (PRESENT)
    @JsonCreator
    public static AttendanceStatus fromCode(String code) {
        if (code == null) return null;
        for (AttendanceStatus status : values()) {
            if (status.getCode().equalsIgnoreCase(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid Attendance Code: " + code);
    }
}
