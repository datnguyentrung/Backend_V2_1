package com.dat.ai_receptionist_web.mapper.Finance;

import com.dat.ai_receptionist_web.domain.Finance.CoursePurchase;
import com.dat.ai_receptionist_web.dto.Finance.CoursePurchaseDTO;
import org.springframework.stereotype.Component;

@Component
public class CoursePurchaseMapper {
    public CoursePurchaseDTO.Response toResponse(CoursePurchase entity) {
        if (entity == null) return null;
        return new CoursePurchaseDTO.Response(entity.getCoursePurchaseId(), entity.getStudentPerson() == null ? null : entity.getStudentPerson().getPersonId(), entity.getCoursePrice() == null ? null : entity.getCoursePrice().getCoursePriceId(), entity.getDebitTransaction() == null ? null : entity.getDebitTransaction().getWalletTransactionId());
    }

    public void updateEntity(CoursePurchaseDTO.UpdateRequest request, CoursePurchase entity) {
    }
}
