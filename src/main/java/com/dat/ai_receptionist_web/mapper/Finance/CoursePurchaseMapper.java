package com.dat.ai_receptionist_web.mapper.Finance;

import com.dat.ai_receptionist_web.domain.Finance.CoursePurchase;
import com.dat.ai_receptionist_web.dto.Finance.CoursePurchaseDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CoursePurchaseMapper {
    @Mapping(target = "studentPersonId", source = "studentPerson.personId")
    @Mapping(target = "coursePriceId", source = "coursePrice.coursePriceId")
    @Mapping(target = "debitTransactionId", source = "debitTransaction.walletTransactionId")
    CoursePurchaseDTO.Response toResponse(CoursePurchase entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "studentPerson", ignore = true)
    @Mapping(target = "coursePrice", ignore = true)
    @Mapping(target = "debitTransaction", ignore = true)
    void updateEntity(CoursePurchaseDTO.UpdateRequest request, @MappingTarget CoursePurchase entity);
}
