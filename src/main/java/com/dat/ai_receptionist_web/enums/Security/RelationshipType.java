package com.dat.ai_receptionist_web.enums.Security;

public enum RelationshipType {

    /**
     * Tài khoản truy cập hồ sơ của chính mình.
     */
    OWNER,

    /**
     * Phụ huynh hoặc người bảo hộ.
     */
    GUARDIAN,

    /**
     * Người quản lý được cấp quyền truy cập.
     */
    MANAGER
}