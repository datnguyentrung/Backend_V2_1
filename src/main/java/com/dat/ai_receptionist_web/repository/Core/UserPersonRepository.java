package com.dat.ai_receptionist_web.repository.Core;

import com.dat.ai_receptionist_web.domain.Core.UserPerson;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserPersonRepository extends JpaRepository<UserPerson, UUID> {
    @EntityGraph(attributePaths = "person")
    List<UserPerson> findAllByUser_UserIdAndActiveTrue(UUID userId);

    @EntityGraph(attributePaths = "person")
    Optional<UserPerson> findByUserPersonIdAndUser_UserIdAndActiveTrue(UUID userPersonId, UUID userId);

    @Query("select distinct up.user.userId from UserPerson up where up.person.personId = :personId and up.active = true")
    List<UUID> findActiveUserIdsByPersonId(@Param("personId") UUID personId);
}
