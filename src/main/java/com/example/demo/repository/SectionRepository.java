package com.example.demo.repository;

import com.example.demo.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Long> {
    List<Section> findByDepartmentName(String departmentName);
}
