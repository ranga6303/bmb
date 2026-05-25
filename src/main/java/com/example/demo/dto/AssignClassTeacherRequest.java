package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

public class AssignClassTeacherRequest {
    @NotBlank
    private String teacherId;

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }
}
