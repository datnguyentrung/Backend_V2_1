package com.dat.ai_receptionist_web.config.Supabase;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "supabase")
public class SupabaseProperties {

    @NotBlank
    private String url;

    @NotBlank
    private String serviceRoleKey;

    @Valid
    private Storage storage = new Storage();

    @Getter
    @Setter
    public static class Storage {
        @NotBlank
        private String faceImageBucket;

        @Positive
        private long maxFileSize;
    }
}
