package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
    name = "teachers",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_teachers_user_id", columnNames = "user_id"),
        @UniqueConstraint(name = "uk_teachers_teacher_id", columnNames = "teacher_id")
    }
)
public class Teacher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, unique = true, length = 50)
    private String teacherId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String email;

    @ManyToMany
    @JoinTable(
        name = "teacher_subjects",
        joinColumns = @JoinColumn(name = "teacher_id"),
        inverseJoinColumns = @JoinColumn(name = "subject_id")
    )
    private Set<Subject> mappedSubjects = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "teacher_sections",
        joinColumns = @JoinColumn(name = "teacher_id"),
        inverseJoinColumns = @JoinColumn(name = "section_id")
    )
    private Set<Section> mappedSections = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Set<Subject> getMappedSubjects() {
        return mappedSubjects;
    }

    public void setMappedSubjects(Set<Subject> mappedSubjects) {
        this.mappedSubjects = mappedSubjects;
    }

    public Set<Section> getMappedSections() {
        return mappedSections;
    }

    public void setMappedSections(Set<Section> mappedSections) {
        this.mappedSections = mappedSections;
    }
}
