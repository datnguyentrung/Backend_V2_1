package com.dat.ai_receptionist_web.repository.Core;

import com.dat.ai_receptionist_web.domain.Core.Person;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface PersonRepository extends JpaRepository<Person, UUID> {
    boolean existsByNationalCode(String nationalCode);
    boolean existsByPersonCode(String personCode);
    Optional<Person> findByPersonCodeIgnoreCase(String personCode);
    Page<Person> findByFullNameContainingIgnoreCaseOrPersonCodeContainingIgnoreCase(
            String fullName, String personCode, Pageable pageable);
}
