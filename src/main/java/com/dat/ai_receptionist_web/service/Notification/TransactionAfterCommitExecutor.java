package com.dat.ai_receptionist_web.service.Notification;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Seam dùng chung để chạy tác vụ sau khi transaction commit thành công.
 */
@Component
public class TransactionAfterCommitExecutor {

    public void afterCommit(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
