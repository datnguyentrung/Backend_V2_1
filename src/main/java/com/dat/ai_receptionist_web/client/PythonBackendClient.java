package com.dat.ai_receptionist_web.client;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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

import java.time.Duration;
import java.util.UUID;

@Component
@Slf4j
public class PythonBackendClient {

    @Value("${BACKEND_PYTHON_API:http://localhost:8000}")
    private String backendPythonApi;

    @Value("${PYTHON_BACKEND_CONNECT_TIMEOUT:2s}")
    private Duration connectTimeout;

    @Value("${PYTHON_BACKEND_READ_TIMEOUT:5s}")
    private Duration readTimeout;

    private RestClient restClient;
    private String checkInUrl;

    @PostConstruct
    void initializeRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        String baseUrl = stripTrailingSlash(backendPythonApi);
        checkInUrl = baseUrl + "/check-in";
        restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        log.info("Python face check-in client configured: url={}, connectTimeoutMs={}, readTimeoutMs={}",
                checkInUrl, connectTimeout.toMillis(), readTimeout.toMillis());
    }

    public CheckInResponse checkInByFaceImage(MultipartFile file) {
        String requestId = UUID.randomUUID().toString();
        long startedAtNanos = System.nanoTime();
        String fileName = resolvedFileName(file);
        MediaType fileContentType = resolveContentType(file.getContentType());
        try {
            MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("file", createFilePart(file, fileName, fileContentType));

            log.info("Calling Python face check-in: requestId={}, method=POST, url={}, requestContentType={}, "
                            + "partName=file, fileName={}, fileSizeBytes={}, fileContentType={}",
                    requestId, checkInUrl, MediaType.MULTIPART_FORM_DATA_VALUE,
                    fileName, file.getSize(), fileContentType);

            CheckInResponse response = restClient.post()
                    .uri("/check-in")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .header("X-Request-ID", requestId)
                    .body(requestBody)
                    .retrieve()
                    .body(CheckInResponse.class);

            if (response == null || (response.matched() && response.personId() == null)) {
                log.warn("Python face check-in returned an invalid payload: requestId={}, url={}, durationMs={}",
                        requestId, checkInUrl, elapsedMillis(startedAtNanos));
                throw new PythonBackendClientException(
                        FailureType.INVALID_RESPONSE,
                        "Python backend did not return a person identifier"
                );
            }
            log.info("Python face check-in completed: requestId={}, url={}, durationMs={}, matched={}, "
                            + "personIdPresent={}, confidence={}, errorPresent={}",
                    requestId, checkInUrl, elapsedMillis(startedAtNanos), response.matched(),
                    response.personId() != null, response.confidence(), StringUtils.hasText(response.error()));
            return response;
        } catch (PythonBackendClientException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            log.warn("Python face check-in rejected request: requestId={}, url={}, durationMs={}, status={}, responseBody={}",
                    requestId, checkInUrl, elapsedMillis(startedAtNanos), exception.getStatusCode(),
                    abbreviate(exception.getResponseBodyAsString()));
            throw new PythonBackendClientException(
                    FailureType.REJECTED,
                    "Python backend rejected the check-in request",
                    exception
            );
        } catch (ResourceAccessException exception) {
            Throwable cause = exception.getMostSpecificCause();
            log.warn("Python face check-in is unreachable: requestId={}, url={}, durationMs={}, causeType={}, cause={}",
                    requestId, checkInUrl, elapsedMillis(startedAtNanos),
                    cause.getClass().getSimpleName(), cause.getMessage());
            throw new PythonBackendClientException(
                    FailureType.UNAVAILABLE,
                    "Python backend could not be reached",
                    exception
            );
        } catch (RestClientException exception) {
            log.warn("Python face check-in client failure: requestId={}, url={}, durationMs={}, error={}",
                    requestId, checkInUrl, elapsedMillis(startedAtNanos), exception.getMessage());
            throw new PythonBackendClientException(
                    FailureType.INVALID_RESPONSE,
                    "Python backend returned an invalid response",
                    exception
            );
        }
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

    private static String abbreviate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 500 ? value : value.substring(0, 500) + "...";
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
        REJECTED,
        UNAVAILABLE,
        INVALID_RESPONSE
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
