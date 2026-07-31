package com.dat.ai_receptionist_web.mapper.Security;

import com.dat.ai_receptionist_web.domain.Security.Role;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.domain.Security.UserProfile;
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

    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "phoneNumber", source = "user.phoneNumber")
    @Mapping(target = "status", source = "user.status")
    @Mapping(target = "createdAt", source = "user.createdAt")
    @Mapping(target = "updatedAt", source = "user.updatedAt")
    @Mapping(target = "lastLoginAt", source = "user.lastLoginAt")
    @Mapping(target = "roles", source = "user.roles", qualifiedByName = "mapRoles")
    @Mapping(target = "fullName", source = "person.fullName")
    @Mapping(target = "birthDate", source = "person.birthDate")
    @Mapping(target = "belt", source = "person.belt")
    @Mapping(target = "gender", source = "person.gender")
    @Mapping(target = "relationshipType", source = "relationshipType")
    @Mapping(target = "active", source = "active")
    UserRes.UserDetail toUserDetail(UserProfile userProfile);

    List<UserRes.UserDetail> toUserDetailList(List<UserProfile> userProfiles);

    @Named("mapRoles")
    default List<String> mapRoles(Set<Role> roles) {
        return roles == null ? List.of() : roles.stream().map(Role::getCode).sorted().toList();
    }
}
