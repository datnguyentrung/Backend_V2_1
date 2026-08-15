package com.dat.ai_receptionist_web.util.error;

public class LeaderboardUnavailableException extends RuntimeException {
    public LeaderboardUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public LeaderboardUnavailableException(String message) {
        super(message);
    }
}
