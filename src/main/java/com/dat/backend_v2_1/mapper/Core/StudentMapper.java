package com.dat.backend_v2_1.mapper.Core;

import com.dat.backend_v2_1.domain.Core.Student;
import com.dat.backend_v2_1.dto.Core.StudentResDTO;
import com.dat.backend_v2_1.dto.Operation.StudentEnrollmentResDTO;
import com.dat.backend_v2_1.dto.Security.UserRes;
import com.dat.backend_v2_1.enums.Security.UserStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface StudentMapper {

    @Mapping(target = "userInfo", source = "student")
    @Mapping(target = "userProfile", source = "student")
    UserRes toUserRes(Student student);

    // ĐÃ TỐI ƯU: Map thẳng field thay vì dùng hàm @Named
    @Mapping(target = "idUser", source = "userId")
    @Mapping(target = "userCode", source = "studentCode")
    @Mapping(target = "idRole", source = "role.code")
    UserRes.UserInfo toUserInfo(Student student);

    @Mapping(target = "name", source = "fullName")
    @Mapping(target = "phone", source = "phoneNumber")
    @Mapping(target = "isActive", source = "status", qualifiedByName = "mapActiveStatus")
    UserRes.UserProfile toUserProfile(Student student);

    /**
     * Map Student -> StudentDetail
     * ĐÃ TỐI ƯU: Dùng Nested Mapping (dấu chấm) thay vì manual builder.
     * MapStruct tự động check null cho branch và role.
     * Lưu ý: roleCode được kế thừa từ UserRes.UserDetail, Lombok Builder tự động handle.
     */
    @Mapping(target = "role", source = "role.code")
    @Mapping(target = "branchId", source = "branch.branchId")
    @Mapping(target = "branchName", source = "branch.branchName")
    @Mapping(target = "branchAddress", source = "branch.address")
    @Mapping(target = "enrollments", ignore = true)
    // Ignore để không bị cảnh báo lúc compile
    StudentResDTO.StudentDetail toStudentDetail(Student student);

    /**
     * Map Student + Enrollments -> StudentDetail
     * ĐÃ TỐI ƯU: Truyền nhiều parameter vào Mapper.
     * Lưu ý: roleCode được kế thừa từ UserRes.UserDetail, Lombok Builder tự động handle.
     */
    @Mapping(target = "role", source = "student.role.code")
    @Mapping(target = "branchId", source = "student.branch.branchId")
    @Mapping(target = "branchName", source = "student.branch.branchName")
    @Mapping(target = "branchAddress", source = "student.branch.address")
    @Mapping(target = "enrollments", source = "enrollments")
    // Map list truyền vào
    StudentResDTO.StudentDetail toStudentDetailWithEnrollments(
            Student student,
            List<StudentEnrollmentResDTO.SimpleResponse> enrollments);

    /**
     * Map Student -> StudentOverview
     */
    @Mapping(target = "branchName", source = "branch.branchName")
    @Mapping(target = "classSchedules", ignore = true)
    // Ignore vì service layer sẽ tự set
    StudentResDTO.StudentOverview toStudentOverview(Student student);

    // ================= HELPER METHODS =================

    @Named("mapActiveStatus")
    default Boolean mapActiveStatus(UserStatus status) {
        return status == UserStatus.ACTIVE;
    }

    // ĐÃ XÓA: getRoleName() và getUserCode() vì MapStruct có thể truy cập thẳng qua dấu "." (ví dụ: role.code, studentCode)
}