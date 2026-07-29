package com.dat.ai_receptionist_web.service.Operation;

import com.dat.ai_receptionist_web.config.Supabase.SupabaseProperties;
import com.dat.ai_receptionist_web.enums.ErrorCode;
import com.dat.ai_receptionist_web.util.error.AppException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SupabaseStorageServiceTest {

    private final SupabaseStorageService storageService = new SupabaseStorageService(
            RestClient.builder().baseUrl("https://example.supabase.co/storage/v1").build(),
            properties()
    );

    @Test
    void validateImageAcceptsPngWithMatchingSignature() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "profile.png",
                "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}
        );

        SupabaseStorageService.ValidatedImage image = storageService.validateImage(file);

        assertEquals("png", image.extension());
        assertEquals("image/png", image.contentType().toString());
    }

    @Test
    void validateImageRejectsContentTypeThatDoesNotMatchBytes() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "profile.png", "image/png", new byte[]{1, 2, 3}
        );

        AppException exception = assertThrows(AppException.class, () -> storageService.validateImage(file));

        assertEquals(ErrorCode.INVALID_IMAGE_FILE, exception.getErrorCode());
    }

    @Test
    void validateImageRejectsFileOverConfiguredLimit() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "profile.jpg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0}
        );
        SupabaseProperties properties = properties();
        properties.getStorage().setMaxFileSize(3);
        SupabaseStorageService limitedService = new SupabaseStorageService(
                RestClient.builder().baseUrl("https://example.supabase.co/storage/v1").build(), properties
        );

        AppException exception = assertThrows(AppException.class, () -> limitedService.validateImage(file));

        assertEquals(ErrorCode.FILE_TOO_LARGE, exception.getErrorCode());
    }

    private static SupabaseProperties properties() {
        SupabaseProperties properties = new SupabaseProperties();
        properties.setUrl("https://example.supabase.co");
        properties.setServiceRoleKey("test-key");
        properties.getStorage().setFaceImageBucket("face-images");
        properties.getStorage().setMaxFileSize(5 * 1024 * 1024L);
        return properties;
    }
}
