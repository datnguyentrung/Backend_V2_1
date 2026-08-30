package com.dat.ai_receptionist_web.repository.Training;

import com.dat.ai_receptionist_web.domain.Training.LeaveRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select lr from LeaveRequest lr where lr.leaveRequestId = :id")
    Optional<LeaveRequest> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
        select lr
        from LeaveRequest lr
        where lr.leaveClassSession.classSessionId in :sessionIds
           or lr.makeupClassSession.classSessionId in :sessionIds
    """)
    List<LeaveRequest> findByReferencedSessionIds(@Param("sessionIds") Collection<UUID> sessionIds);
}
