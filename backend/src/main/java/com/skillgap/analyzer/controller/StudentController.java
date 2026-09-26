package com.skillgap.analyzer.controller;

import java.util.List;

import com.skillgap.analyzer.student.StudentProfile;
import com.skillgap.analyzer.student.StudentProfileService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentProfileService studentProfileService;

    public StudentController(StudentProfileService studentProfileService) {
        this.studentProfileService = studentProfileService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentProfile createStudent(@RequestBody StudentProfile studentProfile) {
        return studentProfileService.createStudent(studentProfile);
    }

    @GetMapping
    public List<StudentProfile> getAllStudents() {
        return studentProfileService.getAllStudents();
    }

    @GetMapping("/{id}")
    public StudentProfile getStudentById(@PathVariable Long id) {
        return studentProfileService.getStudentById(id);
    }

    @PutMapping("/{id}")
    public StudentProfile updateStudent(
            @PathVariable Long id,
            @RequestBody StudentProfile studentProfile) {
        return studentProfileService.updateStudent(id, studentProfile);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStudent(@PathVariable Long id) {
        studentProfileService.deleteStudent(id);
    }
}