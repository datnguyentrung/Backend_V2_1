package com.dat.backend_v2_1.mapper.Core;

import com.dat.backend_v2_1.domain.Core.Student;
import com.dat.backend_v2_1.dto.Core.StudentResDTO;
import com.dat.backend_v2_1.dto.Operation.StudentEnrollmentResDTO;
import com.dat.backend_v2_1.dto.Security.UserRes;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface StudentMapper {

    @Mapping(target = "userInfo", source = "student")
    @Mapping(target = "userProfile", source = "student")
    UserRes toUserRes(Student student);

    @Mapping(target = "idUser", source = "personId")
    @Mapping(target = "userCode", source = "studentCode")
    UserRes.UserInfo toUserInfo(Student student);

    @Mapping(target = "name", source = "fullName")
    @Mapping(target = "isActive", expression = "java(student.getStudentStatus() == com.dat.backend_v2_1.enums.Core.StudentStatus.ACTIVE)")
    @Mapping(target = "phone", ignore = true)
    UserRes.UserProfile toUserProfile(Student student);

    @Mapping(target = "personId", source = "personId")
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "branchId", source = "branch.branchId")
    @Mapping(target = "branchName", source = "branch.branchName")
    @Mapping(target = "branchAddress", source = "branch.address")
    @Mapping(target = "enrollments", ignore = true)
    StudentResDTO.StudentDetail toStudentDetail(Student student);

    @Mapping(target = "personId", source = "student.personId")
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "branchId", source = "student.branch.branchId")
    @Mapping(target = "branchName", source = "student.branch.branchName")
    @Mapping(target = "branchAddress", source = "student.branch.address")
    @Mapping(target = "enrollments", source = "enrollments")
    StudentResDTO.StudentDetail toStudentDetailWithEnrollments(
            Student student,
            List<StudentEnrollmentResDTO.SimpleResponse> enrollments);

    @Mapping(target = "branchName", source = "branch.branchName")
    @Mapping(target = "classSchedules", ignore = true)
    @Mapping(target = "phoneNumber", ignore = true)
    @Mapping(target = "roleName", ignore = true)
    StudentResDTO.StudentOverview toStudentOverview(Student student);

    @Mapping(target = "personId", source = "personId")
    @Mapping(target = "code", source = "studentCode")
    StudentResDTO.StudentSummary toStudentSummary(Student student);
}
