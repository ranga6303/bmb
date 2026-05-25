package com.example.demo.dto;

public class StudentAttendanceSummary {
    private String studentId;
    private String studentName;
    private long attended;
    private long total;
    private double percentage;

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
