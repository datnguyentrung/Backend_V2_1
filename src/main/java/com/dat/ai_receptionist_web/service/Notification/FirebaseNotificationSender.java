package com.dat.ai_receptionist_web.service.Notification;
import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import java.util.*;
@Slf4j
@Service
public class FirebaseNotificationSender {
    private final FirebaseMessaging firebaseMessaging;
    public FirebaseNotificationSender(ObjectProvider<FirebaseMessaging> provider) {
        this.firebaseMessaging = provider.getIfAvailable();
    }
    public boolean send(Set<String> tokens, String title, String body, String payload) {
        if (tokens.isEmpty() || firebaseMessaging == null) return false;
        try {
            MulticastMessage.Builder message = MulticastMessage.builder().addAllTokens(tokens)
                    .putData("title", title).putData("body", body);
            if (payload != null) message.putData("payload", payload);
            return firebaseMessaging.sendEachForMulticast(message.build()).getSuccessCount() > 0;
        } catch (FirebaseMessagingException exception) {
            log.error("FCM delivery failed for {} tokens", tokens.size(), exception);
            return false;
        }
    }
}
