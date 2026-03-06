package com.dat.backend_v2_1.enums.Core;

import lombok.Getter;

@Getter
public enum ScheduleLocation {
    INDOOR("Trong nhà"),
    OUTDOOR("Ngoài trời"),
    ONLINE("Trực tuyến"); // Tùy chọn thêm nếu bạn có dạy online (Zoom/Meet)

    private final String displayName;

    ScheduleLocation(String displayName) {
        this.displayName = displayName;
    }

}
