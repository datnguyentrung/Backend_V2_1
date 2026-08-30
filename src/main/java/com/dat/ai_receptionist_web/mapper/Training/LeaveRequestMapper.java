package com.dat.ai_receptionist_web.mapper.Training;

import com.dat.ai_receptionist_web.domain.Training.LeaveRequest;
import com.dat.ai_receptionist_web.dto.Training.LeaveRequestDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LeaveRequestMapper {
    @Mapping(target = "personId", source = "person.personId")
    @Mapping(target = "leaveClassSessionId", source = "leaveClassSession.classSessionId")
    @Mapping(target = "makeupClassSessionId", source = "makeupClassSession.classSessionId")
    @Mapping(target = "createdByUserId", source = "createdByUser.userId")
    @Mapping(target = "reviewedByUserId", source = "reviewedByUser.userId")
    LeaveRequestDTO.Response toResponse(LeaveRequest entity);
}
