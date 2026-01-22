package com.dat.backend_v2_1.mapper.Core;

import com.dat.backend_v2_1.domain.Core.Branch;
import com.dat.backend_v2_1.domain.Core.Student;
import com.dat.backend_v2_1.domain.Security.Role;
import com.dat.backend_v2_1.dto.Core.StudentResDTO;
import com.dat.backend_v2_1.dto.Security.UserRes;
import com.dat.backend_v2_1.enums.Security.UserStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface StudentMapper {
    @Mapping(target = "userInfo", source = "student")
    @Mapping(target = "userProfile", source = "student")
    UserRes toUserRes(Student student);

    @Mapping(target = "idUser", source = "userId") // Lấy từ User (cha)
    @Mapping(target = "idAccount", source = "studentCode") // Dùng mã SV làm ID Account
    @Mapping(target = "idRole", source = "role", qualifiedByName = "getRoleName") // Xử lý object Role ra String
    UserRes.UserInfo toUserInfo(Student student);

    @Mapping(target = "name", source = "fullName") // Mapping khác tên: fullName -> name
    @Mapping(target = "phone", source = "phoneNumber") // Mapping khác tên
    @Mapping(target = "isActive", source = "status", qualifiedByName = "mapActiveStatus") // Convert Enum -> Boolean
    UserRes.UserProfile toUserProfile(Student student);

    @Named("mapActiveStatus")
    default Boolean mapActiveStatus(UserStatus status) {
        if (status == null) return false;
        return status == UserStatus.ACTIVE;
    }

    // Logic: Lấy tên Role hoặc ID Role từ Object Role
    @Named("getRoleName")
    default String getRoleName(Role role) {
        if (role == null) return null;
        // Giả sử class Role của bạn có method getRoleName() hoặc getRoleId()
        // Nếu Role là Enum thì dùng role.name()
        return role.getCode(); // Hãy thay bằng role.getName() nếu có
    }

    /**
     * Map Student entity sang StudentDetail DTO
     * Bao gồm thông tin từ User (parent), Student (child), và Branch (related)
     *
     * @param student Student entity
     * @return StudentDetail DTO với đầy đủ thông tin
     */
    default StudentResDTO.StudentDetail toStudentDetail(Student student) {
        if (student == null) {
            return null;
        }

        Branch branch = student.getBranch();
        Role role = student.getRole();

        return StudentResDTO.StudentDetail.builder()
                // Thông tin từ User (Parent)
                .userId(student.getUserId())
                .birthDate(student.getBirthDate())
                .phoneNumber(student.getPhoneNumber())
                .belt(student.getBelt())
                .status(student.getStatus())
                .createdAt(student.getCreatedAt())
                .updatedAt(student.getUpdatedAt())
                .lastLoginAt(student.getLastLoginAt())
                .roleName(role != null ? role.getName() : null)
                // Thông tin từ Student (Child)
                .studentCode(student.getStudentCode())
                .nationalCode(student.getNationalCode())
                .fullName(student.getFullName())
                .startDate(student.getStartDate())
                .studentStatus(student.getStudentStatus())
                // Thông tin từ Branch (Related)
                .branchId(branch != null ? branch.getBranchId() : null)
                .branchName(branch != null ? branch.getBranchName() : null)
                .branchAddress(branch != null ? branch.getAddress() : null)
                .build();
    }
}
