package com.dat.backend_v2_1.enums.Core;

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

    public String getCode() {
        return code;
    }
}
