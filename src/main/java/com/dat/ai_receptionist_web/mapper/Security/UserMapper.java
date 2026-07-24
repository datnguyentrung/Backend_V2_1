package com.dat.ai_receptionist_web.mapper.Security;

import com.dat.ai_receptionist_web.domain.Security.Role;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.dto.Security.UserRes;
import com.dat.ai_receptionist_web.enums.Security.UserStatus;
import org.mapstruct.*;

import java.util.List;
import java.util.Set;

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
    @Mapping(target = "idRole", source = "roles", qualifiedByName = "getRoleNames")
    UserRes.UserInfo toUserInfo(User user);

    @Mapping(target = "isActive", source = "status", qualifiedByName = "mapActiveStatus")
    @Mapping(target = "phone", source = "phoneNumber")
    UserRes.UserProfile toUserProfile(User user);

    @Named("mapActiveStatus")
    default Boolean mapActiveStatus(UserStatus status) {
        return status == UserStatus.ACTIVE;
    }

    @Named("getRoleNames")
    default String getRoleNames(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        return String.join(",", roles.stream().map(Role::getCode).sorted().toList());
    }

    default List<String> mapRoles(Set<Role> roles) {
        return roles == null ? List.of() : roles.stream().map(Role::getCode).sorted().toList();
    }
}
