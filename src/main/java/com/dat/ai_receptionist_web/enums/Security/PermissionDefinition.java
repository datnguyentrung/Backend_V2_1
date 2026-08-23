package com.dat.ai_receptionist_web.enums.Security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PermissionDefinition {
    USER_READ("USER_READ", "USER", PermissionAction.READ, "View users"),
    USER_CREATE("USER_CREATE", "USER", PermissionAction.CREATE, "Create users"),
    USER_ROLE_UPDATE("USER_ROLE_UPDATE", "USER", PermissionAction.UPDATE, "Change user roles"),
    ROLE_READ("ROLE_READ", "ROLE", PermissionAction.READ, "View roles and permissions"),
    ROLE_PERMISSION_UPDATE("ROLE_PERMISSION_UPDATE", "ROLE", PermissionAction.UPDATE, "Change role permissions"),
    PERSON_READ("PERSON_READ", "PERSON", PermissionAction.READ, "View people"),
    PERSON_CREATE("PERSON_CREATE", "PERSON", PermissionAction.CREATE, "Create people"),
    PERSON_UPDATE("PERSON_UPDATE", "PERSON", PermissionAction.UPDATE, "Update people"),
    PERSON_DELETE("PERSON_DELETE", "PERSON", PermissionAction.DELETE, "Delete people"),
    BRANCH_READ("BRANCH_READ", "BRANCH", PermissionAction.READ, "View branches"),
    BRANCH_CREATE("BRANCH_CREATE", "BRANCH", PermissionAction.CREATE, "Create branches"),
    BRANCH_UPDATE("BRANCH_UPDATE", "BRANCH", PermissionAction.UPDATE, "Update branches"),
    BRANCH_DELETE("BRANCH_DELETE", "BRANCH", PermissionAction.DELETE, "Delete branches"),
    CLASS_SCHEDULE_READ("CLASS_SCHEDULE_READ", "CLASS_SCHEDULE", PermissionAction.READ, "View class schedules"),
    CLASS_SCHEDULE_CREATE("CLASS_SCHEDULE_CREATE", "CLASS_SCHEDULE", PermissionAction.CREATE, "Create class schedules"),
    CLASS_SCHEDULE_UPDATE("CLASS_SCHEDULE_UPDATE", "CLASS_SCHEDULE", PermissionAction.UPDATE, "Update class schedules"),
    CLASS_SCHEDULE_DELETE("CLASS_SCHEDULE_DELETE", "CLASS_SCHEDULE", PermissionAction.DELETE, "Delete class schedules"),
    COURSE_READ("COURSE_READ", "COURSE", PermissionAction.READ, "View courses"),
    COURSE_CREATE("COURSE_CREATE", "COURSE", PermissionAction.CREATE, "Create courses"),
    COURSE_UPDATE("COURSE_UPDATE", "COURSE", PermissionAction.UPDATE, "Update courses"),
    COURSE_PRICE_READ("COURSE_PRICE_READ", "COURSE_PRICE", PermissionAction.READ, "View course prices"),
    COURSE_PRICE_CREATE("COURSE_PRICE_CREATE", "COURSE_PRICE", PermissionAction.CREATE, "Create course prices"),
    COURSE_PRICE_UPDATE("COURSE_PRICE_UPDATE", "COURSE_PRICE", PermissionAction.UPDATE, "Update course prices"),
    ENROLLMENT_READ("ENROLLMENT_READ", "ENROLLMENT", PermissionAction.READ, "View enrollments"),
    ENROLLMENT_UPDATE("ENROLLMENT_UPDATE", "ENROLLMENT", PermissionAction.UPDATE, "Update enrollments"),
    ATTENDANCE_READ("ATTENDANCE_READ", "ATTENDANCE", PermissionAction.READ, "View attendance"),
    ATTENDANCE_CREATE("ATTENDANCE_CREATE", "ATTENDANCE", PermissionAction.CREATE, "Create attendance"),
    ATTENDANCE_UPDATE("ATTENDANCE_UPDATE", "ATTENDANCE", PermissionAction.UPDATE, "Update attendance"),
    COACH_ASSIGNMENT_READ("COACH_ASSIGNMENT_READ", "COACH_ASSIGNMENT", PermissionAction.READ, "View coach assignments"),
    COACH_ASSIGNMENT_UPDATE("COACH_ASSIGNMENT_UPDATE", "COACH_ASSIGNMENT", PermissionAction.UPDATE, "Manage coach assignments"),
    COACH_TIMESHEET_READ("COACH_TIMESHEET_READ", "COACH_TIMESHEET", PermissionAction.READ, "View coach timesheets"),
    COACH_TIMESHEET_UPDATE("COACH_TIMESHEET_UPDATE", "COACH_TIMESHEET", PermissionAction.UPDATE, "Manage coach timesheets"),
    WALLET_READ("WALLET_READ", "WALLET", PermissionAction.READ, "View wallets and transactions"),
    WALLET_TOP_UP_CREATE("WALLET_TOP_UP_CREATE", "WALLET", PermissionAction.CREATE, "Top up wallets"),
    WALLET_REFUND_CREATE("WALLET_REFUND_CREATE", "WALLET", PermissionAction.CREATE, "Refund course purchases"),
    COURSE_PURCHASE_CREATE("COURSE_PURCHASE_CREATE", "COURSE_PURCHASE", PermissionAction.CREATE, "Purchase courses"),
    COURSE_PURCHASE_READ("COURSE_PURCHASE_READ", "COURSE_PURCHASE", PermissionAction.READ, "View course purchases"),
    NOTIFICATION_READ("NOTIFICATION_READ", "NOTIFICATION", PermissionAction.READ, "View notifications"),
    NOTIFICATION_CREATE("NOTIFICATION_CREATE", "NOTIFICATION", PermissionAction.CREATE, "Create notifications"),
    FITNESS_READ("FITNESS_READ", "FITNESS", PermissionAction.READ, "View fitness benchmarks"),
    FITNESS_UPDATE("FITNESS_UPDATE", "FITNESS", PermissionAction.UPDATE, "Manage fitness benchmarks"),
    FITNESS_RECORD_READ("FITNESS_RECORD_READ", "FITNESS_RECORD", PermissionAction.READ, "View fitness records"),
    FITNESS_RECORD_CREATE("FITNESS_RECORD_CREATE", "FITNESS_RECORD", PermissionAction.CREATE, "Create fitness records"),
    FITNESS_RECORD_UPDATE("FITNESS_RECORD_UPDATE", "FITNESS_RECORD", PermissionAction.UPDATE, "Update fitness records"),
    FITNESS_RECORD_DELETE("FITNESS_RECORD_DELETE", "FITNESS_RECORD", PermissionAction.DELETE, "Delete fitness records");

    private final String code;
    private final String model;
    private final PermissionAction action;
    private final String description;
}
