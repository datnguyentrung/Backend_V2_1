package com.dat.backend_v2_1.service.Core;

import com.dat.backend_v2_1.domain.Core.Branch;
import com.dat.backend_v2_1.domain.Core.Student;
import com.dat.backend_v2_1.dto.Core.StudentReqDTO;
import com.dat.backend_v2_1.dto.Core.StudentResDTO;
import com.dat.backend_v2_1.enums.Core.StudentStatus;
import com.dat.backend_v2_1.mapper.Core.StudentMapper;
import com.dat.backend_v2_1.repository.Core.StudentRepository;
import com.dat.backend_v2_1.service.Security.UserService;
import com.dat.backend_v2_1.util.AccountUtil;
import com.dat.backend_v2_1.util.converter.NameConverter;
import com.dat.backend_v2_1.util.error.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class StudentService {
    private final StudentRepository studentRepository;
    private final BranchService branchService;
    private final UserService userService;
    private final StudentMapper studentMapper;

    public Student getStudentById(String idUser) {
        return studentRepository.findById(UUID.fromString(idUser))
                .orElseThrow(() -> new BusinessException("Không tìm thấy học viên với ID: " + idUser));
    }

    public Student getStudentById(UUID idUser) {
        return studentRepository.findById(idUser)
                .orElseThrow(() -> new BusinessException("Không tìm thấy học viên với ID: " + idUser));
    }

    /**
     * Lấy thông tin chi tiết Student bao gồm cả thông tin từ User và Branch
     * @param userId ID của học viên
     * @return StudentDetail DTO chứa đầy đủ thông tin
     */
    public StudentResDTO.StudentDetail getStudentDetail(UUID userId) {
        Student student = getStudentById(userId);
        return studentMapper.toStudentDetail(student);
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



    @Transactional(rollbackFor = Exception.class)
    public String createStudent(StudentReqDTO.StudentCreate createDTO){
        // BƯỚC 1: Validate Business (Check trùng lặp)
        if (studentRepository.existsByPhoneNumber(createDTO.getPhoneNumber())) {
            throw new BusinessException("Số điện thoại này đã được đăng ký!");
        }
        if (createDTO.getNationalCode() != null &&
                studentRepository.existsByNationalCode(createDTO.getNationalCode())) {
            throw new BusinessException("Mã định danh/CCCD này đã tồn tại!");
        }

        // BƯỚC 2: Lấy dữ liệu liên quan (Branch)
        // Hàm getBranchById nên tự throw Exception nếu không tìm thấy
        Branch branch = branchService.getBranchById(createDTO.getBranchId());

        // BƯỚC 3: Mapping DTO -> Entity
        Student newStudent = new Student();

        // --- Thông tin cơ bản ---
        // Lưu ý: Nên lưu tên chuẩn (Nguyễn Văn A) để hiển thị, không lưu slug vào field fullName
        newStudent.setFullName(NameConverter.formatVietnameseName(createDTO.getFullName()));
        newStudent.setPhoneNumber(createDTO.getPhoneNumber());
        newStudent.setBirthDate(createDTO.getBirthDate());
        newStudent.setNationalCode(createDTO.getNationalCode());
        newStudent.setStartDate(createDTO.getStartDate() != null ? createDTO.getStartDate() : LocalDate.now());

        // --- Thông tin chuyên môn ---
        newStudent.setStudentStatus(createDTO.getStudentStatus() != null ? createDTO.getStudentStatus() : StudentStatus.ACTIVE);
        newStudent.setBelt(createDTO.getBelt());
        newStudent.setBranch(branch);

        // BƯỚC 4: Enrich Data (Tự động sinh dữ liệu hệ thống)
        // Tạo mã học viên: HV_datnt_311005
        String generatedCode = AccountUtil.getUserCode(createDTO.getFullName(), createDTO.getBirthDate());

        // Check trùng mã sinh ra (Trường hợp hiếm gặp 2 người trùng tên trùng ngày sinh)
        if (studentRepository.existsByStudentCode(generatedCode)) {
            generatedCode = generatedCode + "_" + RandomStringUtils.secure().nextNumeric(2);
        }
        newStudent.setStudentCode(generatedCode);

        // BƯỚC 5: Thiết lập User Base (Tài khoản đăng nhập)
        // Logic này sẽ encode password, set Role STUDENT
        userService.setupBaseUser(newStudent, "STUDENT");

        // BƯỚC 6: Save
        studentRepository.save(newStudent);

        log.info("Created student successfully with code: {}", generatedCode);
        return generatedCode;
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
}
