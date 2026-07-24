package com.dat.ai_receptionist_web.dto.Operation;

public record FirebaseMulticastResult(
        int attemptedCount,
        int successCount,
        int failureCount
) {
    public static FirebaseMulticastResult skipped() {
        return new FirebaseMulticastResult(0, 0, 0);
    }

    public boolean attempted() {
        return attemptedCount > 0;
    }

    public boolean hasSuccess() {
        return successCount > 0;
    }
}
