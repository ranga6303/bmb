package com.example.demo.entity;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum Role {
    STUDENT(EnumSet.of(Permission.VIEW_OWN_ATTENDANCE)),
    SUBJECT_TEACHER(EnumSet.of(
        Permission.CREATE_SESSION,
        Permission.LOCK_SESSION,
        Permission.APPROVE_SESSION,
        Permission.CANCEL_SESSION,
        Permission.MANUAL_MARK_ATTENDANCE,
        Permission.VIEW_SECTION_ATTENDANCE
    )),
    CLASS_TEACHER(EnumSet.of(
        Permission.CREATE_SESSION,
        Permission.LOCK_SESSION,
        Permission.APPROVE_SESSION,
        Permission.CANCEL_SESSION,
        Permission.MANUAL_MARK_ATTENDANCE,
        Permission.VIEW_SECTION_ATTENDANCE
    )),
    HOD(EnumSet.of(
        Permission.VIEW_DEPARTMENT_ANALYTICS,
        Permission.VIEW_SECTION_ATTENDANCE,
        Permission.ASSIGN_TEACHER_SECTION,
        Permission.ASSIGN_CLASS_TEACHER,
        Permission.CREATE_SESSION,
        Permission.LOCK_SESSION,
        Permission.APPROVE_SESSION,
        Permission.CANCEL_SESSION,
        Permission.MANUAL_MARK_ATTENDANCE
    )),
    ADMIN(EnumSet.allOf(Permission.class));

    private final Set<Permission> permissions;

    Role(Set<Permission> permissions) {
        this.permissions = Collections.unmodifiableSet(permissions);
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }
}
