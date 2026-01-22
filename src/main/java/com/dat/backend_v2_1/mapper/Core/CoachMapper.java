package com.dat.backend_v2_1.mapper.Core;

import com.dat.backend_v2_1.domain.Core.Coach;
import com.dat.backend_v2_1.domain.Core.Student;
import com.dat.backend_v2_1.domain.Security.Role;
import com.dat.backend_v2_1.dto.Security.UserRes;
import com.dat.backend_v2_1.enums.Security.UserStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface CoachMapper {
    // 1. Sửa tham số đầu vào từ Student -> Coach
    // source = "coach" phải khớp với tên biến tham số (Coach coach)
    @Mapping(target = "userInfo", source = "coach")
    @Mapping(target = "userProfile", source = "coach")
    UserRes toUserRes(Coach coach);

    // 2. Mapping cho UserInfo
    @Mapping(target = "idUser", source = "userId")
    @Mapping(target = "idAccount", source = "staffCode") // Coach dùng staffCode, Student dùng studentCode
    @Mapping(target = "idRole", source = "role", qualifiedByName = "getRoleName")
    UserRes.UserInfo toUserInfo(Coach coach);

    // 3. Mapping cho UserProfile
    @Mapping(target = "name", source = "fullName")
    @Mapping(target = "phone", source = "phoneNumber")
    @Mapping(target = "isActive", source = "status", qualifiedByName = "mapActiveStatus") // Lấy status của User (ACTIVE/LOCKED)
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

    @Named("getRoleName")
    default String getRoleName(Role role) {
        if (role == null) return null;
        return role.getCode(); // Đảm bảo class Role có hàm getCode()
    }
}
