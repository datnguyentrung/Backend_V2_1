package com.dat.ai_receptionist_web.service.Operation;

import com.dat.ai_receptionist_web.event.ClassSessionCompletedEvent;
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
