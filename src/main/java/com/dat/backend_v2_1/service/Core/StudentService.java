package com.dat.backend_v2_1.service.Core;

import com.dat.backend_v2_1.domain.Core.Branch;
import com.dat.backend_v2_1.domain.Core.Student;
import com.dat.backend_v2_1.domain.Operation.StudentEnrollment;
import com.dat.backend_v2_1.dto.Core.ClassScheduleResDTO;
import com.dat.backend_v2_1.dto.Core.StudentReqDTO;
import com.dat.backend_v2_1.dto.Core.StudentResDTO;
import com.dat.backend_v2_1.dto.Operation.StudentEnrollmentResDTO;
import com.dat.backend_v2_1.dto.PageResponse;
import com.dat.backend_v2_1.enums.Core.Belt;
import com.dat.backend_v2_1.enums.Core.StudentStatus;
import com.dat.backend_v2_1.enums.Operation.StudentEnrollmentStatus;
import com.dat.backend_v2_1.mapper.Core.StudentMapper;
import com.dat.backend_v2_1.mapper.Operation.StudentEnrollmentMapper;
import com.dat.backend_v2_1.repository.Core.StudentRepository;
import com.dat.backend_v2_1.repository.Core.StudentRepositoryCustom;
import com.dat.backend_v2_1.repository.Operation.StudentEnrollmentRepository;
import com.dat.backend_v2_1.service.Operation.StudentEnrollmentService;
import com.dat.backend_v2_1.specification.StudentSpecification;
import com.dat.backend_v2_1.util.AccountUtil;
import com.dat.backend_v2_1.util.converter.NameConverter;
import com.dat.backend_v2_1.util.error.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class StudentService {
    private final StudentRepository studentRepository;
    private final BranchService branchService;
    private final StudentMapper studentMapper;
    private final StudentEnrollmentMapper studentEnrollmentMapper;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final StudentEnrollmentService studentEnrollmentService;

    public Student getStudentById(UUID personId) {
        return studentRepository.findById(personId)
                .orElseThrow(() -> new BusinessException("Student not found: " + personId));
    }

    public Student getStudentByStudentCode(String studentCode) {
        return studentRepository.findByStudentCode(studentCode)
                .orElseThrow(() -> new BusinessException("Student not found: " + studentCode));
    }

    public StudentResDTO.StudentDetail getStudentDetail(UUID personId) {
        return studentMapper.toStudentDetail(getStudentById(personId));
    }

    public StudentResDTO.StudentDetail getStudentDetail(String studentCode) {
        Student student = getStudentByStudentCode(studentCode);
        List<StudentEnrollmentResDTO.SimpleResponse> enrollments =
                studentEnrollmentService.findStudentEnrollmentsByStudentCode(studentCode).stream()
                        .map(studentEnrollmentMapper::toSimpleResponse)
                        .toList();
        return studentMapper.toStudentDetailWithEnrollments(student, enrollments);
    }

    @Transactional(rollbackFor = Exception.class)
    public StudentResDTO.StudentDetail updateStudent(StudentReqDTO.StudentUpdate updateDTO) {
        Student student = getStudentById(updateDTO.getPersonId());

        if (updateDTO.getNationalCode() != null &&
                !updateDTO.getNationalCode().equals(student.getNationalCode()) &&
                studentRepository.existsByNationalCode(updateDTO.getNationalCode())) {
            throw new BusinessException("National code already exists");
        }

        if (updateDTO.getBirthDate() != null) student.setBirthDate(updateDTO.getBirthDate());
        if (updateDTO.getBelt() != null) student.setBelt(updateDTO.getBelt());
        if (updateDTO.getNationalCode() != null) student.setNationalCode(updateDTO.getNationalCode());
        if (updateDTO.getFullName() != null) student.setFullName(NameConverter.formatVietnameseName(updateDTO.getFullName()));
        if (updateDTO.getStartDate() != null) student.setStartDate(updateDTO.getStartDate());
        if (updateDTO.getStudentStatus() != null) student.setStudentStatus(updateDTO.getStudentStatus());
        if (updateDTO.getBranchId() != null) {
            Branch branch = branchService.getBranchById(updateDTO.getBranchId());
            student.setBranch(branch);
        }

        Student updatedStudent = studentRepository.save(student);
        return getStudentDetail(updatedStudent.getPersonId());
    }

    @Transactional(rollbackFor = Exception.class)
    public StudentResDTO.StudentDetail createStudent(StudentReqDTO.StudentCreate createDTO) {
        if (createDTO.getNationalCode() != null && studentRepository.existsByNationalCode(createDTO.getNationalCode())) {
            throw new BusinessException("National code already exists");
        }

        Branch branch = branchService.getBranchById(createDTO.getBranchId());
        Student newStudent = new Student();
        newStudent.setFullName(NameConverter.formatVietnameseName(createDTO.getFullName()));
        newStudent.setBirthDate(createDTO.getBirthDate());
        newStudent.setNationalCode(createDTO.getNationalCode());
        newStudent.setStartDate(createDTO.getStartDate() != null ? createDTO.getStartDate() : LocalDate.now());
        newStudent.setStudentStatus(createDTO.getStudentStatus() != null ? createDTO.getStudentStatus() : StudentStatus.ACTIVE);
        newStudent.setBelt(createDTO.getBelt());
        newStudent.setBranch(branch);

        String generatedCode = AccountUtil.getUserCode(createDTO.getFullName(), createDTO.getBirthDate(), null);
        if (studentRepository.existsByStudentCode(generatedCode)) {
            generatedCode = generatedCode + "_" + RandomStringUtils.secure().nextNumeric(2);
        }
        newStudent.setStudentCode(generatedCode);
        newStudent = studentRepository.save(newStudent);

        List<StudentEnrollmentResDTO.SimpleResponse> enrollmentResponses = new ArrayList<>();
        if (createDTO.getEnrollmentRequest() != null
                && createDTO.getEnrollmentRequest().getScheduleIds() != null
                && !createDTO.getEnrollmentRequest().getScheduleIds().isEmpty()) {
            createDTO.getEnrollmentRequest().setStudentCode(newStudent.getStudentCode());
            List<StudentEnrollment> studentEnrollments =
                    studentEnrollmentService.createStudentEnrollmentForNewStudent(newStudent, createDTO.getEnrollmentRequest());
            enrollmentResponses = studentEnrollments.stream()
                    .map(studentEnrollmentMapper::toSimpleResponse)
                    .toList();
        }

        log.info("Created student successfully with code: {}", generatedCode);
        return studentMapper.toStudentDetailWithEnrollments(newStudent, enrollmentResponses);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteStudent(String studentCode) {
        Student student = getStudentByStudentCode(studentCode);
        if (student.getStudentStatus() == StudentStatus.DROPPED) {
            throw new BusinessException("Student is already dropped");
        }
        student.setStudentStatus(StudentStatus.DROPPED);
        studentRepository.save(student);
    }

    @Transactional(rollbackFor = Exception.class)
    public void permanentlyDeleteStudent(String studentCode) {
        Student student = getStudentByStudentCode(studentCode);
        studentRepository.delete(student);
    }

    public StudentResDTO.StudentListResponse getStudentsWithStats(String search, StudentStatus status,
                                                                  Pageable pageable, List<String> scheduleIds,
                                                                  List<Belt> belts) {
        Specification<Student> spec = StudentSpecification.filterBy(search, status, scheduleIds, belts);
        Page<Student> studentsPage = studentRepository.findAll(spec, pageable);

        Specification<Student> countSpec = StudentSpecification.filterWithoutStatus(search, scheduleIds);
        List<StudentRepositoryCustom.StudentStatusCount> filteredCounts =
                studentRepository.countStudentsByStatus(countSpec);

        Map<StudentStatus, Long> statusCountMap = filteredCounts.stream()
                .collect(Collectors.toMap(
                        StudentRepositoryCustom.StudentStatusCount::getStatus,
                        StudentRepositoryCustom.StudentStatusCount::getCount
                ));

        List<UUID> studentIds = studentsPage.getContent().stream().map(Student::getPersonId).toList();
        Map<UUID, List<StudentEnrollment>> enrollmentsByStudentId = Collections.emptyMap();
        if (!studentIds.isEmpty()) {
            List<StudentEnrollment> allActiveEnrollments = studentEnrollmentRepository
                    .findByStudent_PersonIdsInAndStatusWithClassSchedule(studentIds, StudentEnrollmentStatus.ACTIVE);
            enrollmentsByStudentId = allActiveEnrollments.stream()
                    .collect(Collectors.groupingBy(e -> e.getStudent().getPersonId()));
        }

        final Map<UUID, List<StudentEnrollment>> finalEnrollmentsMap = enrollmentsByStudentId;
        Page<StudentResDTO.StudentOverview> studentOverviews = studentsPage.map(student -> {
            StudentResDTO.StudentOverview overview = studentMapper.toStudentOverview(student);
            List<ClassScheduleResDTO.ClassScheduleSummary> scheduleResponses =
                    finalEnrollmentsMap.getOrDefault(student.getPersonId(), Collections.emptyList()).stream()
                            .map(studentEnrollmentMapper::toSimpleResponse)
                            .map(StudentEnrollmentResDTO.SimpleResponse::getClassScheduleSummary)
                            .toList();
            overview.setClassSchedules(scheduleResponses);
            return overview;
        });

        return StudentResDTO.StudentListResponse.builder()
                .activeStudentCount(statusCountMap.getOrDefault(StudentStatus.ACTIVE, 0L))
                .reservedStudentCount(statusCountMap.getOrDefault(StudentStatus.RESERVED, 0L))
                .droppedStudentCount(statusCountMap.getOrDefault(StudentStatus.DROPPED, 0L))
                .students(PageResponse.of(studentOverviews))
                .build();
    }

    public List<Student> getStudentByParentId(UUID parentId) {
        return List.of();
    }
}
