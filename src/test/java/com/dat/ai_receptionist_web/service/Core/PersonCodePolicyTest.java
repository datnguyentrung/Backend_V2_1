package com.dat.ai_receptionist_web.service.Core;

import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.enums.Core.PersonKind;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CoreErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersonCodePolicyTest {
    private final PersonCodePolicy policy = new PersonCodePolicy();

    @Test
    void classifiesStudentAndSystemEmployeeByPrefix() {
        assertThat(policy.classify("VQ_anv_010100")).isEqualTo(PersonKind.STUDENT);
        assertThat(policy.classify("vqt_system_admin")).isEqualTo(PersonKind.SYSTEM_EMPLOYEE);
    }

    @Test
    void rejectsInvalidOrMissingCodes() {
        assertThatThrownBy(() -> policy.classify(null)).isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode())
                        .isEqualTo(CoreErrorCode.PERSON_CODE_POLICY_VIOLATION));
        assertThatThrownBy(() -> policy.classify("STAFF-001")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> policy.classify("")).isInstanceOf(ApiException.class);
    }

    @Test
    void requireStudentAndSystemEmployeeEnforceKind() {
        Person student = Person.builder().personCode("VQ_anv_010100").build();
        Person employee = Person.builder().personCode("VQT_SYSTEM_ADMIN").build();

        policy.requireStudent(student);
        policy.requireSystemEmployee(employee);

        assertThatThrownBy(() -> policy.requireSystemEmployee(student))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> policy.requireStudent(employee))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> policy.requireStudent(Person.builder().personCode(null).build()))
                .isInstanceOf(ApiException.class);
    }
}
