package com.example.demo.controller;

import com.example.demo.dto.AssignClassTeacherRequest;
import com.example.demo.dto.AssignSectionRequest;
import com.example.demo.dto.MessageResponse;
import com.example.demo.entity.Role;
import com.example.demo.entity.Section;
import com.example.demo.entity.Subject;
import com.example.demo.entity.Teacher;
import com.example.demo.exception.CustomException;
import com.example.demo.repository.SectionRepository;
import com.example.demo.repository.SubjectRepository;
import com.example.demo.repository.TeacherRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hod")
@Transactional
public class HodController {
    private final TeacherRepository teacherRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;

    public HodController(TeacherRepository teacherRepository, SectionRepository sectionRepository, SubjectRepository subjectRepository) {
        this.teacherRepository = teacherRepository;
        this.sectionRepository = sectionRepository;
        this.subjectRepository = subjectRepository;
    }

    @PreAuthorize("hasAuthority('ASSIGN_TEACHER_SECTION')")
    @PostMapping("/teachers/{teacherId}/assign-section")
    public ResponseEntity<MessageResponse> assignSection(@PathVariable String teacherId, @Valid @RequestBody AssignSectionRequest request) {
        Teacher teacher = teacherRepository.findByTeacherId(teacherId)
            .orElseThrow(() -> new CustomException("Teacher not found"));
        Section section = sectionRepository.findById(request.getSectionId())
            .orElseThrow(() -> new CustomException("Section not found"));
        teacher.getMappedSections().add(section);
        teacherRepository.save(teacher);
        return ResponseEntity.ok(new MessageResponse("Section assigned to teacher."));
    }

    @PreAuthorize("hasAuthority('ASSIGN_CLASS_TEACHER')")
    @PostMapping("/sections/{sectionId}/assign-class-teacher")
    public ResponseEntity<MessageResponse> assignClassTeacher(@PathVariable Long sectionId, @Valid @RequestBody AssignClassTeacherRequest request) {
        Teacher teacher = teacherRepository.findByTeacherId(request.getTeacherId())
            .orElseThrow(() -> new CustomException("Teacher not found"));
        if (teacher.getUser() == null || teacher.getUser().getRole() != Role.CLASS_TEACHER) {
            throw new CustomException("Assigned teacher must have CLASS_TEACHER role.");
        }
        Section section = sectionRepository.findById(sectionId)
            .orElseThrow(() -> new CustomException("Section not found"));
        if (teacherRepository.existsBySectionAndRole(section, Role.CLASS_TEACHER)) {
            throw new CustomException("Section already has a class teacher assigned.");
        }
        teacher.getMappedSections().add(section);
        teacherRepository.save(teacher);
        return ResponseEntity.ok(new MessageResponse("Class teacher assigned to section."));
    }

    @PreAuthorize("hasAuthority('ASSIGN_TEACHER_SECTION')")
    @PostMapping("/sections/{sectionId}/assign-subject")
    public ResponseEntity<MessageResponse> assignSubjectToSection(@PathVariable Long sectionId, @RequestParam Long subjectId) {
        Section section = sectionRepository.findById(sectionId)
            .orElseThrow(() -> new CustomException("Section not found"));
        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> new CustomException("Subject not found"));

        if (subject.getMappedSections().contains(section)) {
            throw new CustomException("Subject is already assigned to this section.");
        }

        subject.getMappedSections().add(section);
        subjectRepository.save(subject);
        return ResponseEntity.ok(new MessageResponse("Subject assigned to section."));
    }

    @PreAuthorize("hasAuthority('ASSIGN_TEACHER_SECTION')")
    @PostMapping("/sections/{sectionId}/remove-subject")
    public ResponseEntity<MessageResponse> removeSubjectFromSection(@PathVariable Long sectionId, @RequestParam Long subjectId) {
        Section section = sectionRepository.findById(sectionId)
            .orElseThrow(() -> new CustomException("Section not found"));
        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> new CustomException("Subject not found"));

        if (!subject.getMappedSections().contains(section)) {
            throw new CustomException("Subject is not assigned to this section.");
        }

        subject.getMappedSections().remove(section);
        subjectRepository.save(subject);
        return ResponseEntity.ok(new MessageResponse("Subject removed from section."));
    }
}
