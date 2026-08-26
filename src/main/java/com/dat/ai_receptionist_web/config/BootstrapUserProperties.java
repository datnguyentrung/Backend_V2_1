package com.dat.ai_receptionist_web.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.bootstrap")
public class BootstrapUserProperties {
    private List<UserAccount> users = new ArrayList<>();

    @Getter
    @Setter
    public static class UserAccount {
        private String roleCode;
        private String phoneNumber;
        private String password;
        private String personCode;
        private String fullName;
        private String email;
    }
}
