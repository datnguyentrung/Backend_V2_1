package com.dat.ai_receptionist_web.mapper.Core;

import com.dat.ai_receptionist_web.domain.Core.Coach;
import com.dat.ai_receptionist_web.domain.Security.UserProfile;
import com.dat.ai_receptionist_web.dto.Core.CoachResDTO;
import com.dat.ai_receptionist_web.dto.Operation.CoachAssignmentResDTO;
import com.dat.ai_receptionist_web.dto.Security.UserRes;
import com.dat.ai_receptionist_web.mapper.Security.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = UserMapper.class
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

    @Mapping(target = "personId", source = "coach.personId")
    @Mapping(target = "faceImagePath", source = "coach.faceImagePath")
    @Mapping(target = "email", source = "coach.email")
    @Mapping(target = "staffCode", source = "coach.staffCode")
    @Mapping(target = "coachStatus", source = "coach.coachStatus")
    @Mapping(target = "currentAssignments", ignore = true)
    @Mapping(target = "userDetails", source = "userProfiles")
    CoachResDTO.CoachDetail toCoachDetail(Coach coach, List<UserProfile> userProfiles);

    @Mapping(target = "personId", source = "coach.personId")
    @Mapping(target = "faceImagePath", source = "coach.faceImagePath")
    @Mapping(target = "email", source = "coach.email")
    @Mapping(target = "staffCode", source = "coach.staffCode")
    @Mapping(target = "coachStatus", source = "coach.coachStatus")
    @Mapping(target = "currentAssignments", source = "coachAssignmentCurrent")
    @Mapping(target = "userDetails", source = "userProfiles")
    CoachResDTO.CoachDetail toCoachDetailWithAssignments(
            Coach coach,
            List<CoachAssignmentResDTO.SimpleResponse> coachAssignmentCurrent,
            List<UserProfile> userProfiles);

    List<CoachResDTO.CoachDetail> toCoachDetailList(List<Coach> coaches);
}
