package com.dat.backend_v2_1.service.Core;

import com.dat.backend_v2_1.domain.Core.Branch;
import com.dat.backend_v2_1.domain.Core.Student;
import com.dat.backend_v2_1.domain.Operation.StudentEnrollment;
import com.dat.backend_v2_1.dto.Core.ClassScheduleResDTO;
import com.dat.backend_v2_1.dto.Core.StudentReqDTO;
import com.dat.backend_v2_1.dto.Core.StudentResDTO;
import com.dat.backend_v2_1.dto.Operation.StudentEnrollmentResDTO;
import com.dat.backend_v2_1.dto.PageResponse;
import com.dat.backend_v2_1.enums.Core.StudentStatus;
import com.dat.backend_v2_1.enums.Operation.StudentEnrollmentStatus;
import com.dat.backend_v2_1.mapper.Core.StudentMapper;
import com.dat.backend_v2_1.mapper.Operation.StudentEnrollmentMapper;
import com.dat.backend_v2_1.repository.Core.StudentRepository;
import com.dat.backend_v2_1.repository.Core.StudentRepositoryCustom;
import com.dat.backend_v2_1.repository.Operation.StudentEnrollmentRepository;
import com.dat.backend_v2_1.service.Operation.StudentEnrollmentService;
import com.dat.backend_v2_1.service.Security.UserService;
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
    private final UserService userService;
    private final StudentMapper studentMapper;
    private final StudentEnrollmentMapper studentEnrollmentMapper;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final StudentEnrollmentService studentEnrollmentService;

    public Student getStudentById(String idUser) {
        return studentRepository.findById(UUID.fromString(idUser))
                .orElseThrow(() -> new BusinessException("Không tìm thấy học viên với ID: " + idUser));
    }

    public Student getStudentById(UUID idUser) {
        return studentRepository.findById(idUser)
                .orElseThrow(() -> new BusinessException("Không tìm thấy học viên với ID: " + idUser));
    }

    public Student getStudentByStudentCode(String studentCode) {
        return studentRepository.findByStudentCode(studentCode)
                .orElseThrow(() -> new BusinessException("Không tìm thấy học viên với mã: " + studentCode));
    }

    /**
     * Lấy thông tin chi tiết Student bao gồm cả thông tin từ User và Branch
     *
     * @param userId ID của học viên
     * @return StudentDetail DTO chứa đầy đủ thông tin
     */
    public StudentResDTO.StudentDetail getStudentDetail(UUID userId) {
        Student student = getStudentById(userId);
        return studentMapper.toStudentDetail(student);
    }

    /**
     * Lấy thông tin chi tiết Student theo studentCode (mã học viên)
     *
     * @param studentCode Mã học viên
     * @return StudentDetail DTO chứa đầy đủ thông tin
     */
    public StudentResDTO.StudentDetail getStudentDetail(String studentCode) {
        Student student = getStudentByStudentCode(studentCode);
        List<StudentEnrollmentResDTO.SimpleResponse> enrollments =
                studentEnrollmentService.findStudentEnrollmentsByStudentCode(studentCode).stream()
                        .map(studentEnrollmentMapper::toSimpleResponse)
                        .toList();
        return studentMapper.toStudentDetailWithEnrollments(student, enrollments);
    }

    /**
     * Cập nhật thông tin Student một cách chuyên nghiệp
     * - Chỉ cập nhật các field không null
     * - Validate business logic
     * - Log thay đổi
     *
     * @param updateDTO DTO chứa thông tin cần cập nhật
     * @return StudentDetail sau khi cập nhật
     */
    @Transactional(rollbackFor = Exception.class)
    public StudentResDTO.StudentDetail updateStudent(StudentReqDTO.StudentUpdate updateDTO) {
        // BƯỚC 1: Lấy entity hiện tại
        Student student = getStudentById(updateDTO.getUserId());

        // BƯỚC 2: Validate Business Logic
        // 2.1. Kiểm tra số điện thoại trùng (nếu có thay đổi)
        if (updateDTO.getPhoneNumber() != null &&
                !updateDTO.getPhoneNumber().equals(student.getPhoneNumber())) {
            if (studentRepository.existsByPhoneNumber(updateDTO.getPhoneNumber())) {
                throw new BusinessException("Số điện thoại này đã được đăng ký bởi học viên khác!");
            }
        }

        // 2.2. Kiểm tra CCCD/CMND trùng (nếu có thay đổi)
        if (updateDTO.getNationalCode() != null &&
                !updateDTO.getNationalCode().equals(student.getNationalCode())) {
            if (studentRepository.existsByNationalCode(updateDTO.getNationalCode())) {
                throw new BusinessException("Mã định danh/CCCD này đã tồn tại!");
            }
        }

        // BƯỚC 3: Cập nhật các field từ User (Parent)
        if (updateDTO.getPhoneNumber() != null) {
            student.setPhoneNumber(updateDTO.getPhoneNumber());
            log.info("Updated phone number for student {}: {} -> {}",
                    student.getStudentCode(), student.getPhoneNumber(), updateDTO.getPhoneNumber());
        }

        if (updateDTO.getBirthDate() != null) {
            student.setBirthDate(updateDTO.getBirthDate());
            log.info("Updated birth date for student {}: {}",
                    student.getStudentCode(), updateDTO.getBirthDate());
        }

        if (updateDTO.getBelt() != null) {
            log.info("Updated belt for student {}: {} -> {}",
                    student.getStudentCode(), student.getBelt(), updateDTO.getBelt());
            student.setBelt(updateDTO.getBelt());
        }

        // BƯỚC 4: Cập nhật các field từ Student (Child)
        if (updateDTO.getNationalCode() != null) {
            student.setNationalCode(updateDTO.getNationalCode());
            log.info("Updated national code for student {}: {}",
                    student.getStudentCode(), updateDTO.getNationalCode());
        }

        if (updateDTO.getFullName() != null) {
            String formattedName = NameConverter.formatVietnameseName(updateDTO.getFullName());
            student.setFullName(formattedName);
            log.info("Updated full name for student {}: {} -> {}",
                    student.getStudentCode(), student.getFullName(), formattedName);
        }

        if (updateDTO.getStartDate() != null) {
            student.setStartDate(updateDTO.getStartDate());
            log.info("Updated start date for student {}: {}",
                    student.getStudentCode(), updateDTO.getStartDate());
        }

        if (updateDTO.getStudentStatus() != null) {
            log.info("Updated student status for student {}: {} -> {}",
                    student.getStudentCode(), student.getStudentStatus(), updateDTO.getStudentStatus());
            student.setStudentStatus(updateDTO.getStudentStatus());
        }

        // BƯỚC 5: Cập nhật Branch nếu có
        if (updateDTO.getBranchId() != null) {
            Branch newBranch = branchService.getBranchById(updateDTO.getBranchId());
            log.info("Updated branch for student {}: {} -> {}",
                    student.getStudentCode(),
                    student.getBranch() != null ? student.getBranch().getBranchName() : "null",
                    newBranch.getBranchName());
            student.setBranch(newBranch);
        }

        // BƯỚC 6: Lưu thay đổi
        Student updatedStudent = studentRepository.save(student);

        log.info("Successfully updated student with code: {}", updatedStudent.getStudentCode());

        // BƯỚC 7: Trả về StudentDetail
        return getStudentDetail(updatedStudent.getUserId());
    }

    /**
     * Tạo học viên mới
     * - Validate dữ liệu đầu vào
     * - Kiểm tra trùng lặp
     * - Tự động sinh mã học viên
     * - Thiết lập tài khoản đăng nhập
     * - Xử lý đăng ký lớp học (nếu có)
     *
     * @param createDTO DTO chứa thông tin tạo mới
     * @return StudentDetail DTO đầy đủ thông tin học viên vừa tạo kèm enrollment
     */
    @Transactional(rollbackFor = Exception.class)
    public StudentResDTO.StudentDetail createStudent(StudentReqDTO.StudentCreate createDTO) {
        // BƯỚC 1: Validate Business (Check trùng lặp)
        if (studentRepository.existsByPhoneNumber(createDTO.getPhoneNumber())) {
            throw new BusinessException("Số điện thoại này đã được đăng ký!");
        }
        if (createDTO.getNationalCode() != null &&
                studentRepository.existsByNationalCode(createDTO.getNationalCode())) {
            throw new BusinessException("Mã định danh/CCCD này đã tồn tại!");
        }

        // BƯỚC 2: Lấy dữ liệu liên quan (Branch)
        Branch branch = branchService.getBranchById(createDTO.getBranchId());

        // BƯỚC 3: Mapping DTO -> Entity
        Student newStudent = new Student();
        newStudent.setFullName(NameConverter.formatVietnameseName(createDTO.getFullName()));
        newStudent.setPhoneNumber(createDTO.getPhoneNumber());
        newStudent.setBirthDate(createDTO.getBirthDate());
        newStudent.setNationalCode(createDTO.getNationalCode());
        newStudent.setStartDate(createDTO.getStartDate() != null ? createDTO.getStartDate() : LocalDate.now());
        newStudent.setStudentStatus(createDTO.getStudentStatus() != null ? createDTO.getStudentStatus() : StudentStatus.ACTIVE);
        newStudent.setBelt(createDTO.getBelt());
        newStudent.setBranch(branch);

        // BƯỚC 4: Enrich Data (Tự động sinh mã học viên)
        String generatedCode = AccountUtil.getUserCode(createDTO.getFullName(), createDTO.getBirthDate(), null);
        if (studentRepository.existsByStudentCode(generatedCode)) {
            generatedCode = generatedCode + "_" + RandomStringUtils.secure().nextNumeric(2);
        }
        newStudent.setStudentCode(generatedCode);

        // BƯỚC 5: Thiết lập User Base (Tài khoản đăng nhập)
        userService.setupBaseUser(newStudent, "STUDENT");

        // BƯỚC 6: Save
        newStudent = studentRepository.save(newStudent);

        // BƯỚC 7: Xử lý enrollment (nếu có)
        List<StudentEnrollmentResDTO.SimpleResponse> enrollmentResponses = new ArrayList<>();
        if (createDTO.getEnrollmentRequest() != null
                && createDTO.getEnrollmentRequest().getScheduleIds() != null
                && !createDTO.getEnrollmentRequest().getScheduleIds().isEmpty()) {

            // Gán ID vừa tạo vào request enrollment
            createDTO.getEnrollmentRequest().setStudentId(String.valueOf(newStudent.getUserId()));

            // Gọi Service enrollment
            List<StudentEnrollment> studentEnrollments = studentEnrollmentService.createStudentEnrollment(createDTO.getEnrollmentRequest());

            // Map sang DTO
            enrollmentResponses = studentEnrollments.stream()
                    .map(studentEnrollmentMapper::toSimpleResponse)
                    .toList();
        }

        log.info("Created student successfully with code: {}", generatedCode);

        // BƯỚC 8: Trả về StudentDetail kèm enrollment
        return studentMapper.toStudentDetailWithEnrollments(newStudent, enrollmentResponses);
    }

    /**
     * Xóa học viên (Soft Delete)
     * - Không xóa vật lý khỏi database
     * - Chỉ cập nhật status thành DEACTIVATED
     * - Có thể khôi phục lại sau này
     *
     * @param userId ID của học viên cần xóa
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteStudent(UUID userId) {
        // BƯỚC 1: Lấy entity hiện tại
        Student student = getStudentById(userId);

        // BƯỚC 2: Kiểm tra trạng thái hiện tại
        if (student.getStatus() == com.dat.backend_v2_1.enums.Security.UserStatus.DEACTIVATED) {
            log.warn("Student {} is already deactivated", student.getStudentCode());
            throw new BusinessException("Học viên này đã bị vô hiệu hóa trước đó!");
        }

        // BƯỚC 3: Soft Delete - Cập nhật status thành DEACTIVATED
        student.setStatus(com.dat.backend_v2_1.enums.Security.UserStatus.DEACTIVATED);
        student.setStudentStatus(StudentStatus.DROPPED); // Cập nhật trạng thái học tập thành NGHỈ HỌC

        // BƯỚC 4: Lưu thay đổi
        studentRepository.save(student);

        log.info("Successfully deactivated student with code: {} (userId: {})",
                student.getStudentCode(), userId);
    }

    /**
     * Xóa vật lý học viên khỏi database (Hard Delete)
     * ⚠️ CẢNH BÁO: Hành động này không thể hoàn tác!
     * Chỉ nên dùng cho mục đích quản trị hoặc tuân thủ GDPR
     *
     * @param userId ID của học viên cần xóa vĩnh viễn
     */
    @Transactional(rollbackFor = Exception.class)
    public void permanentlyDeleteStudent(UUID userId) {
        // BƯỚC 1: Kiểm tra tồn tại
        Student student = getStudentById(userId);

        log.warn("⚠️ PERMANENTLY DELETING student: {} (userId: {})",
                student.getStudentCode(), userId);

        // BƯỚC 2: Hard Delete
        studentRepository.delete(student);

        log.info("Successfully permanently deleted student with code: {}", student.getStudentCode());
    }

    /**
     * Lấy danh sách học viên kèm theo số liệu thống kê
     * - Cho phép filter theo tên (search) và trạng thái học viên (status)
     * - Trả về số lượng học viên theo từng trạng thái (ACTIVE, RESERVED, DROPPED)
     * - Trả về danh sách học viên dưới dạng Page để hỗ trợ phân trang ở frontend
     * - Mỗi học viên trong danh sách sẽ kèm theo thông tin các lớp học đang tham gia (classSchedules)
     * Lưu ý: Việc lấy thông tin lớp học sẽ được tối ưu bằng cách dùng 1 query duy nhất để lấy tất cả enrollment của các học viên trong page, sau đó map vào từng học viên
     *
     * @param search   Từ khóa tìm kiếm (theo tên), có thể null hoặc rỗng để không filter theo tên
     * @param status   Trạng thái học viên để filter (ACTIVE, RESERVED, DROPPED), có thể null để không filter theo trạng thái
     * @param pageable Thông tin phân trang (page number, page size, sort)
     * @return StudentListResponse chứa danh sách học viên và số liệu thống kê
     */
    public StudentResDTO.StudentListResponse getStudentsWithStats(String search, StudentStatus status, Pageable pageable, List<String> scheduleIds) {
        // 1. Build Specification
        Specification<Student> spec = StudentSpecification.filterBy(search, status, scheduleIds);

        // 2. Lấy danh sách học viên (Page) với Specification
        Page<Student> studentsPage = studentRepository.findAll(spec, pageable);

        // 3. Lấy số lượng thống kê THEO FILTER (Specification không có status)
        Specification<Student> countSpec = StudentSpecification.filterWithoutStatus(search, scheduleIds);
        List<StudentRepositoryCustom.StudentStatusCount> filteredCounts = studentRepository.countStudentsByStatus(countSpec);

        // 4. Map kết quả thống kê ra Map
        Map<StudentStatus, Long> statusCountMap = filteredCounts.stream()
                .collect(Collectors.toMap(
                        StudentRepositoryCustom.StudentStatusCount::getStatus,
                        StudentRepositoryCustom.StudentStatusCount::getCount
                ));

        // 5. Batch Fetch Enrollments (Chống N + 1)
        List<UUID> studentIds = studentsPage.getContent().stream()
                .map(Student::getUserId)
                .toList();

        Map<UUID, List<StudentEnrollment>> enrollmentsByStudentId = Collections.emptyMap();
        if (!studentIds.isEmpty()) {
            List<StudentEnrollment> allActiveEnrollments = studentEnrollmentRepository
                    .findByStudent_UserIdsInAndStatusWithClassSchedule(studentIds, StudentEnrollmentStatus.ACTIVE);

            enrollmentsByStudentId = allActiveEnrollments.stream()
                    .collect(Collectors.groupingBy(e -> e.getStudent().getUserId()));
        }

        // 6. Map sang DTO
        final Map<UUID, List<StudentEnrollment>> finalEnrollmentsMap = enrollmentsByStudentId;
        Page<StudentResDTO.StudentOverview> studentOverviews = studentsPage.map(student -> {
            StudentResDTO.StudentOverview overview = studentMapper.toStudentOverview(student);

            List<StudentEnrollment> studentEnrollments = finalEnrollmentsMap.getOrDefault(student.getUserId(), Collections.emptyList());

            List<ClassScheduleResDTO.ClassScheduleSummary> scheduleResponses = studentEnrollments.stream()
                    .map(studentEnrollmentMapper::toSimpleResponse)
                    .map(StudentEnrollmentResDTO.SimpleResponse::getClassScheduleSummary)
                    .toList();

            overview.setClassSchedules(scheduleResponses);
            return overview;
        });

        // 7. Build Response
        return StudentResDTO.StudentListResponse.builder()
                .activeStudentCount(statusCountMap.getOrDefault(StudentStatus.ACTIVE, 0L))
                .reservedStudentCount(statusCountMap.getOrDefault(StudentStatus.RESERVED, 0L))
                .droppedStudentCount(statusCountMap.getOrDefault(StudentStatus.DROPPED, 0L))
                .students(PageResponse.of(studentOverviews))
                .build();
    }

    public List<Student> getStudentByParentId(UUID parentId) {
        return studentRepository.findByParent_UserId(parentId);
    }
}
