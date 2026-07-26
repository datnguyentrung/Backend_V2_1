package com.dat.ai_receptionist_web.client;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

@Component
@Slf4j
public class PythonBackendClient {

    @Value("${BACKEND_PYTHON_API:}")
    private String backendPythonApi;

    @Value("${HUGGING_FACE_TOKEN:}")
    private String huggingFaceToken;

    @Value("${PYTHON_BACKEND_CONNECT_TIMEOUT:5s}")
    private Duration connectTimeout;

    @Value("${PYTHON_BACKEND_READ_TIMEOUT:60s}")
    private Duration readTimeout;

    private RestClient restClient;

    @PostConstruct
    void initializeRestClient() {
        validateConfiguration();

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        String baseUrl = stripTrailingSlash(backendPythonApi);
        restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeaders(headers -> headers.setBearerAuth(huggingFaceToken))
                .build();
        log.info("Python backend client configured: baseUrlConfigured=true, huggingFaceAuthenticationConfigured=true, "
                        + "connectTimeoutMs={}, readTimeoutMs={}",
                connectTimeout.toMillis(), readTimeout.toMillis());
    }

    public CheckInResponse checkInByFaceImage(MultipartFile file) {
        String requestId = UUID.randomUUID().toString();
        long startedAtNanos = System.nanoTime();
        String fileName = resolvedFileName(file);
        MediaType fileContentType = resolveContentType(file.getContentType());
        try {
            MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("file", createFilePart(file, fileName, fileContentType));

            log.info("Calling Python backend: requestId={}, method=POST, path=/check-in", requestId);

            CheckInResponse response = restClient.post()
                    .uri("/check-in")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .header("X-Request-ID", requestId)
                    .body(requestBody)
                    .retrieve()
                    .body(CheckInResponse.class);

            if (response == null || (response.matched() && response.personId() == null)) {
                log.warn("Python backend returned an invalid check-in payload: requestId={}, path=/check-in, durationMs={}",
                        requestId, elapsedMillis(startedAtNanos));
                throw new PythonBackendClientException(
                        FailureType.INVALID_RESPONSE,
                        "Python backend did not return a person identifier"
                );
            }
            log.info("Python backend check-in completed: requestId={}, path=/check-in, durationMs={}",
                    requestId, elapsedMillis(startedAtNanos));
            return response;
        } catch (PythonBackendClientException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw responseException("check-in", requestId, "/check-in", startedAtNanos, exception);
        } catch (ResourceAccessException exception) {
            throw resourceAccessException("check-in", requestId, "/check-in", startedAtNanos, exception);
        } catch (RestClientException exception) {
            throw invalidResponseException("check-in", requestId, "/check-in", startedAtNanos, exception);
        }
    }

    public void checkHealth() {
        String requestId = UUID.randomUUID().toString();
        long startedAtNanos = System.nanoTime();
        try {
            log.info("Calling Python backend: requestId={}, method=GET, path=/health", requestId);
            restClient.get()
                    .uri("/health")
                    .header("X-Request-ID", requestId)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Python backend health check completed: requestId={}, path=/health, durationMs={}",
                    requestId, elapsedMillis(startedAtNanos));
        } catch (RestClientResponseException exception) {
            throw responseException("health check", requestId, "/health", startedAtNanos, exception);
        } catch (ResourceAccessException exception) {
            throw resourceAccessException("health check", requestId, "/health", startedAtNanos, exception);
        } catch (RestClientException exception) {
            throw invalidResponseException("health check", requestId, "/health", startedAtNanos, exception);
        }
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(backendPythonApi)) {
            throw new IllegalStateException("BACKEND_PYTHON_API environment variable is required");
        }
        if (!StringUtils.hasText(huggingFaceToken)) {
            throw new IllegalStateException("HUGGING_FACE_TOKEN environment variable is required");
        }
    }

    private static PythonBackendClientException responseException(
            String operation,
            String requestId,
            String path,
            long startedAtNanos,
            RestClientResponseException exception
    ) {
        FailureType failureType = classifyHttpFailure(exception.getStatusCode());
        log.warn("Python backend {} failed: requestId={}, path={}, durationMs={}, status={}, failureType={}",
                operation, requestId, path, elapsedMillis(startedAtNanos), exception.getStatusCode(), failureType);
        return new PythonBackendClientException(failureType, messageFor(failureType), exception);
    }

    private static PythonBackendClientException resourceAccessException(
            String operation,
            String requestId,
            String path,
            long startedAtNanos,
            ResourceAccessException exception
    ) {
        FailureType failureType = classifyResourceAccessFailure(exception);
        Throwable cause = exception.getMostSpecificCause();
        String causeType = cause == null ? "Unknown" : cause.getClass().getSimpleName();
        log.warn("Python backend {} failed: requestId={}, path={}, durationMs={}, failureType={}, causeType={}",
                operation, requestId, path, elapsedMillis(startedAtNanos), failureType, causeType);
        return new PythonBackendClientException(failureType, messageFor(failureType), exception);
    }

    private static PythonBackendClientException invalidResponseException(
            String operation,
            String requestId,
            String path,
            long startedAtNanos,
            RestClientException exception
    ) {
        log.warn("Python backend {} failed: requestId={}, path={}, durationMs={}, failureType={}",
                operation, requestId, path, elapsedMillis(startedAtNanos), FailureType.INVALID_RESPONSE);
        return new PythonBackendClientException(
                FailureType.INVALID_RESPONSE,
                messageFor(FailureType.INVALID_RESPONSE),
                exception
        );
    }

    static FailureType classifyResourceAccessFailure(ResourceAccessException exception) {
        Throwable cause = exception.getMostSpecificCause();
        if (cause instanceof SocketTimeoutException timeoutException) {
            String message = timeoutException.getMessage();
            return message != null && message.toLowerCase(Locale.ROOT).contains("connect")
                    ? FailureType.CONNECT_TIMEOUT
                    : FailureType.READ_TIMEOUT;
        }
        if (cause instanceof ConnectException) {
            return FailureType.CONNECTION_FAILED;
        }
        return FailureType.CONNECTION_FAILED;
    }

    private static FailureType classifyHttpFailure(HttpStatusCode statusCode) {
        if (statusCode.value() == HttpStatus.UNAUTHORIZED.value()) {
            return FailureType.AUTHENTICATION_FAILED;
        }
        if (statusCode.value() == HttpStatus.FORBIDDEN.value()) {
            return FailureType.ACCESS_DENIED;
        }
        if (statusCode.value() == HttpStatus.NOT_FOUND.value()) {
            return FailureType.ENDPOINT_NOT_FOUND;
        }
        if (statusCode.value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
            return FailureType.RATE_LIMITED;
        }
        return statusCode.is5xxServerError() ? FailureType.UPSTREAM_ERROR : FailureType.REJECTED;
    }

    private static String messageFor(FailureType failureType) {
        return switch (failureType) {
            case AUTHENTICATION_FAILED -> "Python backend authentication failed";
            case ACCESS_DENIED -> "Python backend access denied";
            case ENDPOINT_NOT_FOUND -> "Python backend endpoint was not found";
            case RATE_LIMITED -> "Python backend rate limit exceeded";
            case UPSTREAM_ERROR, CONNECTION_FAILED -> "Python backend is unavailable";
            case CONNECT_TIMEOUT, READ_TIMEOUT -> "Python backend request timed out";
            case INVALID_RESPONSE -> "Invalid response from Python backend";
            case REJECTED -> "Python backend rejected the request";
        };
    }

    private static HttpEntity<Resource> createFilePart(
            MultipartFile file,
            String fileName,
            MediaType fileContentType
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(fileContentType);
        headers.setContentDispositionFormData(
                "file",
                fileName
        );
        return new HttpEntity<>(file.getResource(), headers);
    }

    private static String resolvedFileName(MultipartFile file) {
        String fileName = StringUtils.getFilename(file.getOriginalFilename());
        return StringUtils.hasText(fileName) ? fileName : "face-image";
    }

    private static MediaType resolveContentType(String contentType) {
        try {
            return StringUtils.hasText(contentType)
                    ? MediaType.parseMediaType(contentType)
                    : MediaType.APPLICATION_OCTET_STREAM;
        } catch (InvalidMediaTypeException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private static String stripTrailingSlash(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private static long elapsedMillis(long startedAtNanos) {
        return Duration.ofNanos(System.nanoTime() - startedAtNanos).toMillis();
    }

    public record CheckInResponse(
            boolean matched,

            @JsonAlias({"person_id", "personId"})
            UUID personId,

            Double confidence,

            String error
    ) {
    }

    public enum FailureType {
        AUTHENTICATION_FAILED,
        ACCESS_DENIED,
        ENDPOINT_NOT_FOUND,
        RATE_LIMITED,
        UPSTREAM_ERROR,
        CONNECT_TIMEOUT,
        READ_TIMEOUT,
        CONNECTION_FAILED,
        REJECTED,
        INVALID_RESPONSE;

        public boolean isUnavailable() {
            return switch (this) {
                case UPSTREAM_ERROR, CONNECT_TIMEOUT, READ_TIMEOUT, CONNECTION_FAILED -> true;
                default -> false;
            };
        }
    }

    public static class PythonBackendClientException extends RuntimeException {
        private final FailureType failureType;

        public PythonBackendClientException(FailureType failureType, String message) {
            super(message);
            this.failureType = failureType;
        }

        public PythonBackendClientException(FailureType failureType, String message, Throwable cause) {
            super(message, cause);
            this.failureType = failureType;
        }

        public FailureType getFailureType() {
            return failureType;
        }
    }
}
