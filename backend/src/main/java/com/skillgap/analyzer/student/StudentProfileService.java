package com.skillgap.analyzer.student;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class StudentProfileService {

    private final StudentProfileRepository studentProfileRepository;

    public StudentProfileService(StudentProfileRepository studentProfileRepository) {
        this.studentProfileRepository = studentProfileRepository;
    }

    public StudentProfile createStudent(StudentProfile studentProfile) {
        return studentProfileRepository.save(studentProfile);
    }

    public List<StudentProfile> getAllStudents() {
        return studentProfileRepository.findAll();
    }

    public StudentProfile getStudentById(Long id) {
        return studentProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
    }

    public StudentProfile updateStudent(Long id, StudentProfile updatedStudent) {
        StudentProfile existingStudent = getStudentById(id);

        existingStudent.setFullName(updatedStudent.getFullName());
        existingStudent.setEmail(updatedStudent.getEmail());
        existingStudent.setCollege(updatedStudent.getCollege());
        existingStudent.setBranch(updatedStudent.getBranch());
        existingStudent.setGraduationYear(updatedStudent.getGraduationYear());
        existingStudent.setCgpa(updatedStudent.getCgpa());

        return studentProfileRepository.save(existingStudent);
    }

    public void deleteStudent(Long id) {
        StudentProfile existingStudent = getStudentById(id);
        studentProfileRepository.delete(existingStudent);
    }
}