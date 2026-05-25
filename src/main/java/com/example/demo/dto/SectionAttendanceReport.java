package com.example.demo.dto;

import java.util.ArrayList;
import java.util.List;

public class SectionAttendanceReport {
    private Long sectionId;
    private Long subjectId;
    private List<StudentAttendanceSummary> students = new ArrayList<>();

    public Long getSectionId() {
        return sectionId;
    }

    public void setSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public List<StudentAttendanceSummary> getStudents() {
        return students;
    }

    public void setStudents(List<StudentAttendanceSummary> students) {
        this.students = students;
    }
}
