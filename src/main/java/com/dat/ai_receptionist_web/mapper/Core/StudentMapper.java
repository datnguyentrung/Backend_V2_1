package com.dat.ai_receptionist_web.mapper.Core;

import com.dat.ai_receptionist_web.domain.Core.Student;
import com.dat.ai_receptionist_web.domain.Security.UserProfile;
import com.dat.ai_receptionist_web.dto.Core.StudentResDTO;
import com.dat.ai_receptionist_web.dto.Operation.StudentEnrollmentResDTO;
import com.dat.ai_receptionist_web.dto.Security.UserRes;
import com.dat.ai_receptionist_web.mapper.Security.UserMapper;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = UserMapper.class
)
public interface StudentMapper {

    @Mapping(target = "userInfo", source = "student")
    @Mapping(target = "userProfile", source = "student")
    UserRes toUserRes(Student student);

    @Mapping(target = "idUser", source = "personId")
    @Mapping(target = "userCode", source = "studentCode")
    UserRes.UserInfo toUserInfo(Student student);

    @Mapping(target = "name", source = "fullName")
    @Mapping(target = "isActive", expression = "java(student.getStudentStatus() == com.dat.ai_receptionist_web.enums.Core.StudentStatus.ACTIVE)")
    @Mapping(target = "phone", ignore = true)
    UserRes.UserProfile toUserProfile(Student student);

    @Mapping(target = "personId", source = "student.personId")
    @Mapping(target = "faceImagePath", source = "student.faceImagePath")
    @Mapping(target = "branchId", source = "student.branch.branchId")
    @Mapping(target = "branchName", source = "student.branch.branchName")
    @Mapping(target = "branchAddress", source = "student.branch.address")
    @Mapping(target = "enrollments", ignore = true)
    @Mapping(target = "userDetails", source = "userProfiles")
    StudentResDTO.StudentDetail toStudentDetail(Student student, List<UserProfile> userProfiles);

    @Mapping(target = "personId", source = "student.personId")
    @Mapping(target = "faceImagePath", source = "student.faceImagePath")
    @Mapping(target = "branchId", source = "student.branch.branchId")
    @Mapping(target = "branchName", source = "student.branch.branchName")
    @Mapping(target = "branchAddress", source = "student.branch.address")
    @Mapping(target = "enrollments", source = "enrollments")
    @Mapping(target = "userDetails", source = "userProfiles")
    StudentResDTO.StudentDetail toStudentDetailWithEnrollments(
            Student student,
            List<StudentEnrollmentResDTO.SimpleResponse> enrollments,
            List<UserProfile> userProfiles);

    @Mapping(target = "branchName", source = "branch.branchName")
    @Mapping(target = "personId", source = "personId")
    @Mapping(target = "faceImagePath", source = "faceImagePath")
    @Mapping(target = "classSchedules", ignore = true)
    @Mapping(target = "phoneNumber", ignore = true)
    @Mapping(target = "roleName", ignore = true)
    StudentResDTO.StudentOverview toStudentOverview(Student student);

    @Mapping(target = "personId", source = "personId")
    @Mapping(target = "code", source = "studentCode")
    StudentResDTO.StudentSummary toStudentSummary(Student student);
}
