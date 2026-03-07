package com.dat.backend_v2_1.enums.Core;

import lombok.Getter;

@Getter
public enum ScheduleLevel {
    BASIC("Lớp Cơ Bản"),
    KID("Lớp Kid"),
    ADULT("Lớp Người Lớn"),
    ASSISTANT("Lớp Trợ Giảng"),
    PERFORMANCE("Lớp Biểu Diễn"),
    DAN("Lớp Đẳng"),

    // Đội tuyển đối kháng (Sparring/Kumite)
    SPARRING_TEAM_TIER_1("Đội Tuyển Đối Kháng Tuyến 1"),
    SPARRING_TEAM_TIER_2("Đội Tuyển Đối Kháng Tuyến 2"),
    SPARRING_TEAM_TIER_3("Đội Tuyển Đối Kháng Tuyến 3"),

    // Đội tuyển quyền (Forms/Poomsae/Kata)
    FORMS_TEAM_TIER_1("Đội Tuyển Quyền Tuyến 1"),
    FORMS_TEAM_TIER_2("Đội Tuyển Quyền Tuyến 2"),
    FORMS_TEAM_TIER_3("Đội Tuyển Quyền Tuyến 3");

    private final String displayName;

    ScheduleLevel(String displayName) {
        this.displayName = displayName;
    }

}