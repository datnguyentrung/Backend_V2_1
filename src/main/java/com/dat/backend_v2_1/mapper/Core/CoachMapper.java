package com.dat.backend_v2_1.mapper.Core;

import com.dat.backend_v2_1.domain.Core.Coach;
import com.dat.backend_v2_1.domain.Security.Role;
import com.dat.backend_v2_1.dto.Core.CoachResDTO;
import com.dat.backend_v2_1.dto.Operation.CoachAssignmentResDTO;
import com.dat.backend_v2_1.dto.Security.UserRes;
import com.dat.backend_v2_1.enums.Security.UserStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CoachMapper {
    // 1. Sửa tham số đầu vào từ Student -> Coach
    // source = "coach" phải khớp với tên biến tham số (Coach coach)
    @Mapping(target = "userInfo", source = "coach")
    @Mapping(target = "userProfile", source = "coach")
    UserRes toUserRes(Coach coach);

    // 2. Mapping cho UserInfo
    @Mapping(target = "idUser", source = "userId")
    @Mapping(target = "userCode", source = "coach", qualifiedByName = "getUserCode")
    @Mapping(target = "idRole", source = "role", qualifiedByName = "getRoleName")
    UserRes.UserInfo toUserInfo(Coach coach);

    // 3. Mapping cho UserProfile
    @Mapping(target = "name", source = "fullName")
    @Mapping(target = "phone", source = "phoneNumber")
    @Mapping(target = "isActive", source = "status", qualifiedByName = "mapActiveStatus")
    // Lấy status của User (ACTIVE/LOCKED)
    // Lưu ý: Nếu muốn map thêm thuộc tính riêng của Coach (ví dụ belt, position) vào UserProfile
    // thì class UserProfile phải có các trường đó. Hiện tại UserProfile có 'belt', Coach không có 'belt'.
    // Nếu Coach có logic đai đẳng riêng thì cần xử lý, nếu không thì trường belt trong UserProfile sẽ null.
    UserRes.UserProfile toUserProfile(Coach coach);

    // --- NAMED METHODS ---

    @Named("mapActiveStatus")
    default Boolean mapActiveStatus(UserStatus status) {
        if (status == null) return false;
        return status == UserStatus.ACTIVE;
    }

    @Named("getUserCode")
    default String getUserCode(Coach coach) {
        if (coach == null) return null;
        return coach.getStaffCode(); // Hoặc có thể map sang một giá trị khác nếu cần
    }

    @Named("getRoleName")
    default String getRoleName(Role role) {
        if (role == null) return null;
        return role.getCode(); // Đảm bảo class Role có hàm getCode()
    }

    // 1. Hàm map CHỈ CÓ 1 THAM SỐ (Dùng cho List hoặc khi không có assignment)
    // MapStruct sẽ tự động dùng hàm này cho vòng lặp của toCoachDetailList
    @Mapping(target = "roleName", source = "role", qualifiedByName = "getRoleName")
    @Mapping(target = "currentAssignments", ignore = true)
    // Bỏ qua field này vì không có data truyền vào
    CoachResDTO.CoachDetail toCoachDetail(Coach coach);

    // 2. Hàm map CÓ 2 THAM SỐ (Dùng riêng cho hàm createCoach hoặc getCoachDetail)
    // Đổi tên hàm một chút để MapStruct không bị nhầm lẫn
    @Mapping(target = "roleName", source = "coach.role", qualifiedByName = "getRoleName")
    @Mapping(target = "currentAssignments", source = "coachAssignmentCurrent")
    CoachResDTO.CoachDetail toCoachDetailWithAssignments(Coach coach, List<CoachAssignmentResDTO.SimpleResponse> coachAssignmentCurrent);

    // 3. Hàm map List
    // Lúc này MapStruct sẽ tự động biết gọi hàm số (1) ở trên để map.
    List<CoachResDTO.CoachDetail> toCoachDetailList(List<Coach> coaches);
}
