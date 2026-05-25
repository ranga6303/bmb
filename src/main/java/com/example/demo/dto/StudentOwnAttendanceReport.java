package com.example.demo.dto;

import java.util.ArrayList;
import java.util.List;

public class StudentOwnAttendanceReport {
    private String studentId;
    private String studentName;
    private String sectionName;
    private List<SubjectAttendance> subjects = new ArrayList<>();

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }

    public List<SubjectAttendance> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<SubjectAttendance> subjects) {
        this.subjects = subjects;
    }

    public static class SubjectAttendance {
        private Long subjectId;
        private String subjectName;
        private long attended;
        private long total;
        private double percentage;

        public Long getSubjectId() {
            return subjectId;
        }

        public void setSubjectId(Long subjectId) {
            this.subjectId = subjectId;
        }

        public String getSubjectName() {
            return subjectName;
        }

        public void setSubjectName(String subjectName) {
            this.subjectName = subjectName;
        }

        public long getAttended() {
            return attended;
        }

        public void setAttended(long attended) {
            this.attended = attended;
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public double getPercentage() {
            return percentage;
        }

        public void setPercentage(double percentage) {
            this.percentage = percentage;
        }
    }
}
