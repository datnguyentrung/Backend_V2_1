package com.dat.ai_receptionist_web.service.Operation;

import com.dat.ai_receptionist_web.config.Supabase.SupabaseProperties;
import com.dat.ai_receptionist_web.enums.ErrorCode;
import com.dat.ai_receptionist_web.util.error.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupabaseStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/webp"
    );

    @Qualifier("supabaseStorageRestClient")
    private final RestClient supabaseStorageRestClient;
    private final SupabaseProperties properties;

    public ValidatedImage validateImage(MultipartFile file) {
        if (file == null) {
            throw new AppException(ErrorCode.INVALID_IMAGE_FILE);
        }
        if (file.isEmpty()) {
            throw new AppException(ErrorCode.EMPTY_IMAGE_FILE);
        }
        if (file.getSize() > properties.getStorage().getMaxFileSize()) {
            throw new AppException(ErrorCode.FILE_TOO_LARGE);
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new AppException(ErrorCode.UNSUPPORTED_IMAGE_TYPE);
        }

        try {
            byte[] bytes = file.getBytes();
            if (!matchesSignature(contentType, bytes)) {
                throw new AppException(ErrorCode.INVALID_IMAGE_FILE);
            }
            return new ValidatedImage(MediaType.parseMediaType(contentType), extensionFor(contentType), bytes);
        } catch (IOException exception) {
            throw new AppException(ErrorCode.INVALID_IMAGE_FILE, exception);
        }
    }

    public String uploadPersonFaceImage(UUID personId, MultipartFile file) {
        return uploadPersonFaceImage(personId, validateImage(file));
    }

    public String uploadPersonFaceImage(UUID personId, ValidatedImage image) {
        String objectPath = "persons/%s/%s.%s".formatted(personId, UUID.randomUUID(), image.extension());
        String bucket = properties.getStorage().getFaceImageBucket();
        try {
            supabaseStorageRestClient.post()
                    .uri("/object/{bucket}/{objectPath}", bucket, objectPath)
                    .header("x-upsert", "false")
                    .contentType(image.contentType())
                    .body(image.bytes())
                    .retrieve()
                    .toBodilessEntity();
            log.info("Uploaded face image: personId={}, objectPath={}", personId, objectPath);
            return objectPath;
        } catch (RestClientResponseException exception) {
            log.error("Supabase face-image upload failed: personId={}, objectPath={}, status={}, exceptionType={}",
                    personId, objectPath, exception.getStatusCode().value(), exception.getClass().getSimpleName(), exception);
            throw new AppException(toStorageErrorCode(exception), exception);
        } catch (ResourceAccessException exception) {
            log.error("Supabase face-image upload unavailable: personId={}, objectPath={}, exceptionType={}",
                    personId, objectPath, exception.getClass().getSimpleName(), exception);
            throw new AppException(ErrorCode.SUPABASE_STORAGE_UNAVAILABLE, exception);
        } catch (RuntimeException exception) {
            log.error("Supabase face-image upload failed: personId={}, objectPath={}, exceptionType={}",
                    personId, objectPath, exception.getClass().getSimpleName(), exception);
            throw new AppException(ErrorCode.SUPABASE_STORAGE_UPLOAD_FAILED, exception);
        }
    }

    public void deleteObject(String objectPath) {
        if (!StringUtils.hasText(objectPath)) {
            return;
        }

        try {
            supabaseStorageRestClient.delete()
                    .uri("/object/{bucket}/{objectPath}", properties.getStorage().getFaceImageBucket(), objectPath)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Deleted face image: objectPath={}", objectPath);
        } catch (RestClientResponseException exception) {
            log.error("Supabase face-image delete failed: objectPath={}, status={}, exceptionType={}",
                    objectPath, exception.getStatusCode().value(), exception.getClass().getSimpleName(), exception);
            throw new AppException(toStorageErrorCode(exception), exception);
        } catch (ResourceAccessException exception) {
            log.error("Supabase face-image delete unavailable: objectPath={}, exceptionType={}",
                    objectPath, exception.getClass().getSimpleName(), exception);
            throw new AppException(ErrorCode.SUPABASE_STORAGE_UNAVAILABLE, exception);
        } catch (RuntimeException exception) {
            log.error("Supabase face-image delete failed: objectPath={}, exceptionType={}",
                    objectPath, exception.getClass().getSimpleName(), exception);
            throw new AppException(ErrorCode.SUPABASE_STORAGE_DELETE_FAILED, exception);
        }
    }

    public String createSignedUrl(String objectPath, Duration duration) {
        if (!StringUtils.hasText(objectPath)) {
            return null;
        }
        try {
            SignedUrlResponse response = supabaseStorageRestClient.post()
                    .uri("/object/sign/{bucket}/{objectPath}", properties.getStorage().getFaceImageBucket(), objectPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("expiresIn", duration.toSeconds()))
                    .retrieve()
                    .body(SignedUrlResponse.class);
            if (response == null || !StringUtils.hasText(response.signedURL())) {
                throw new AppException(ErrorCode.SUPABASE_STORAGE_UPLOAD_FAILED);
            }
            return stripTrailingSlash(properties.getUrl()) + "/storage/v1" + response.signedURL();
        } catch (AppException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            log.error("Supabase signed-url request failed: objectPath={}, status={}, exceptionType={}",
                    objectPath, exception.getStatusCode().value(), exception.getClass().getSimpleName(), exception);
            throw new AppException(toStorageErrorCode(exception), exception);
        } catch (ResourceAccessException exception) {
            log.error("Supabase signed-url request unavailable: objectPath={}, exceptionType={}",
                    objectPath, exception.getClass().getSimpleName(), exception);
            throw new AppException(ErrorCode.SUPABASE_STORAGE_UNAVAILABLE, exception);
        }
    }

    private static ErrorCode toStorageErrorCode(RestClientResponseException exception) {
        return exception.getStatusCode().is5xxServerError()
                ? ErrorCode.SUPABASE_STORAGE_UNAVAILABLE
                : ErrorCode.SUPABASE_STORAGE_UPLOAD_FAILED;
    }

    private static boolean matchesSignature(String contentType, byte[] bytes) {
        return switch (contentType) {
            case MediaType.IMAGE_JPEG_VALUE -> bytes.length >= 3
                    && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF;
            case MediaType.IMAGE_PNG_VALUE -> bytes.length >= 8
                    && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47
                    && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A;
            case "image/webp" -> bytes.length >= 12
                    && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
            default -> false;
        };
    }

    private static String extensionFor(String contentType) {
        return switch (contentType) {
            case MediaType.IMAGE_JPEG_VALUE -> "jpg";
            case MediaType.IMAGE_PNG_VALUE -> "png";
            case "image/webp" -> "webp";
            default -> throw new AppException(ErrorCode.UNSUPPORTED_IMAGE_TYPE);
        };
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    public record ValidatedImage(MediaType contentType, String extension, byte[] bytes) {
    }

    private record SignedUrlResponse(String signedURL) {
    }
}
