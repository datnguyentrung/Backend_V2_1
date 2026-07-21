package com.dat.backend_v2_1.service.Operation;

import com.dat.backend_v2_1.event.ClassSessionCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ClassSessionCompletedEventListener {

    private final ClassSessionNotificationService classSessionNotificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClassSessionCompleted(ClassSessionCompletedEvent event) {
        classSessionNotificationService.enqueueCompletedSessionReport(event.sessionId());
    }
}
