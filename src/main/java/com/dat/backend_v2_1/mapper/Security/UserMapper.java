package com.dat.backend_v2_1.mapper.Security;

import com.dat.backend_v2_1.domain.Security.Role;
import com.dat.backend_v2_1.domain.Security.User;
import com.dat.backend_v2_1.dto.Security.UserRes;
import com.dat.backend_v2_1.enums.Security.UserStatus;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserMapper {
    @Mapping(target = "userInfo", source = "user")
    @Mapping(target = "userProfile", source = "user")
    UserRes toUserRes(User user);

    @Mapping(target = "idUser", source = "userId")
    @Mapping(target = "userCode", source = "user", qualifiedByName = "getUserCode")
    @Mapping(target = "idRole", source = "role", qualifiedByName = "getRoleName")
        // Xử lý object Role ra String
    UserRes.UserInfo toUserInfo(User user);

    @Mapping(target = "isActive", source = "status", qualifiedByName = "mapActiveStatus")
    @Mapping(target = "name", source = "fullName") // Mapping khác tên: fullName -> name
    @Mapping(target = "phone", source = "phoneNumber")
    UserRes.UserProfile toUserProfile(User user);

    @Named("mapActiveStatus")
    default Boolean mapActiveStatus(UserStatus status) {
        if (status == null) return false;
        return status == UserStatus.ACTIVE;
    }

    // Logic: Lấy code Role từ Object Role
    @Named("getRoleName")
    default String getRoleName(Role role) {
        if (role == null) return null;
        // Role.code là khóa chính (VD: ROLE_STUDENT, ROLE_COACH)
        return role.getCode();
    }

    // Logic: Lấy userCode từ User
    // User base class không có staffCode/studentCode, nên trả về null
    // CoachMapper và StudentMapper sẽ override để lấy staffCode/studentCode
    @Named("getUserCode")
    default String getUserCode(User user) {
        return null; // User base class không có code
    }
}
