package com.dat.ai_receptionist_web.mapper.Core;

import com.dat.ai_receptionist_web.domain.Core.Coach;
import com.dat.ai_receptionist_web.domain.Security.Role;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.dto.Core.CoachResDTO;
import com.dat.ai_receptionist_web.dto.Operation.CoachAssignmentResDTO;
import com.dat.ai_receptionist_web.dto.Security.UserRes;
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
    @Mapping(target = "isActive", expression = "java(coach.getCoachStatus() == com.dat.ai_receptionist_web.enums.Core.CoachStatus.ACTIVE)")
    UserRes.UserProfile toUserProfile(Coach coach);

    @Mapping(target = "personId", source = "personId")
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "faceImagePath", source = "faceImagePath")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "staffCode", source = "staffCode")
    @Mapping(target = "coachStatus", source = "coachStatus")
    @Mapping(target = "currentAssignments", ignore = true)
    CoachResDTO.CoachDetail toCoachDetail(Coach coach);

    @Mapping(target = "personId", source = "coach.personId")
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "faceImagePath", source = "coach.faceImagePath")
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
