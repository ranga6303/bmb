package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;

public class AssignSectionRequest {
    @NotNull
    private Long sectionId;

    public Long getSectionId() {
        return sectionId;
    }

    public void setSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }
}
