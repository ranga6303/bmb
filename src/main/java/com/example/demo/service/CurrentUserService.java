package com.example.demo.service;

import com.example.demo.entity.Student;
import com.example.demo.entity.Teacher;
import com.example.demo.entity.User;
import com.example.demo.exception.CustomException;
import com.example.demo.repository.StudentRepository;
import com.example.demo.repository.TeacherRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;

    public CurrentUserService(
        UserRepository userRepository,
        TeacherRepository teacherRepository,
        StudentRepository studentRepository
    ) {
        this.userRepository = userRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new CustomException("Unauthenticated");
        }
        return userRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new CustomException("User not found"));
    }

    public Teacher getCurrentTeacher() {
        return teacherRepository.findByUser(getCurrentUser())
            .orElseThrow(() -> new CustomException("Teacher profile not found"));
    }

    public Student getCurrentStudent() {
        return studentRepository.findByUser(getCurrentUser())
            .orElseThrow(() -> new CustomException("Student profile not found"));
    }
}
