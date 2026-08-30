package com.dat.ai_receptionist_web.service.Core;

import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.enums.Core.PersonKind;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CoreErrorCode;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Phân loại person theo prefix person_code: VQT_ = SYSTEM_EMPLOYEE, VQ_ = STUDENT.
 */
@Component
public class PersonCodePolicy {

    public PersonKind classify(String personCode) {
        if (personCode == null || personCode.isBlank()) {
            throw violation();
        }
        String code = personCode.trim().toUpperCase(Locale.ROOT);
        if (code.startsWith("VQT_")) {
            return PersonKind.SYSTEM_EMPLOYEE;
        }
        if (code.startsWith("VQ_")) {
            return PersonKind.STUDENT;
        }
        throw violation();
    }

    public void validateFormat(String personCode) {
        classify(personCode);
    }

    public void requireStudent(Person person) {
        requireKind(person, PersonKind.STUDENT);
    }

    public void requireSystemEmployee(Person person) {
        requireKind(person, PersonKind.SYSTEM_EMPLOYEE);
    }

    private void requireKind(Person person, PersonKind expected) {
        if (person == null || classify(person.getPersonCode()) != expected) {
            throw violation();
        }
    }

    private ApiException violation() {
        return new ApiException(CoreErrorCode.PERSON_CODE_POLICY_VIOLATION);
    }
}
