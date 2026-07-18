package com.dat.backend_v2_1.mapper.Core;

import com.dat.backend_v2_1.domain.Core.Coach;
import com.dat.backend_v2_1.domain.Security.Role;
import com.dat.backend_v2_1.domain.Security.User;
import com.dat.backend_v2_1.dto.Core.CoachResDTO;
import com.dat.backend_v2_1.dto.Operation.CoachAssignmentResDTO;
import com.dat.backend_v2_1.dto.Security.UserRes;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Set;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface CoachMapper {
    @Mapping(target = "userInfo", source = "coach")
    @Mapping(target = "userProfile", source = "coach")
    UserRes toUserRes(Coach coach);

    @Mapping(target = "idUser", source = "personId")
    @Mapping(target = "userCode", source = "staffCode")
    UserRes.UserInfo toUserInfo(Coach coach);

    @Mapping(target = "name", source = "fullName")
    @Mapping(target = "phone", ignore = true)
    @Mapping(target = "isActive", expression = "java(coach.getCoachStatus() == com.dat.backend_v2_1.enums.Core.CoachStatus.ACTIVE)")
    UserRes.UserProfile toUserProfile(Coach coach);

    @Mapping(target = "userId", source = "personId")
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "email", source = "email")
    @Mapping(target = "staffCode", source = "staffCode")
    @Mapping(target = "coachStatus", source = "coachStatus")
    @Mapping(target = "currentAssignments", ignore = true)
    CoachResDTO.CoachDetail toCoachDetail(Coach coach);

    @Mapping(target = "userId", source = "coach.personId")
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "email", source = "coach.email")
    @Mapping(target = "staffCode", source = "coach.staffCode")
    @Mapping(target = "coachStatus", source = "coach.coachStatus")
    @Mapping(target = "currentAssignments", source = "coachAssignmentCurrent")
    CoachResDTO.CoachDetail toCoachDetailWithAssignments(Coach coach, List<CoachAssignmentResDTO.SimpleResponse> coachAssignmentCurrent);

    default CoachResDTO.CoachDetail toCoachDetailWithAssignments(
            Coach coach,
            List<CoachAssignmentResDTO.SimpleResponse> coachAssignmentCurrent,
            User user) {
        CoachResDTO.CoachDetail coachDetail = toCoachDetailWithAssignments(coach, coachAssignmentCurrent);
        if (user == null) {
            return coachDetail;
        }

        coachDetail.setPhoneNumber(user.getPhoneNumber());
        coachDetail.setStatus(user.getStatus());
        coachDetail.setLastLoginAt(user.getLastLoginAt());
        coachDetail.setRoles(mapRoles(user.getRoles()));
        return coachDetail;
    }

    default List<String> mapRoles(Set<Role> roles) {
        return roles == null ? List.of() : roles.stream().map(Role::getCode).sorted().toList();
    }

    List<CoachResDTO.CoachDetail> toCoachDetailList(List<Coach> coaches);
}
