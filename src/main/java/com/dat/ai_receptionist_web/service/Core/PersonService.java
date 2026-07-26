package com.dat.ai_receptionist_web.service.Core;

import com.dat.ai_receptionist_web.client.PythonBackendClient;
import com.dat.ai_receptionist_web.client.PythonBackendClient.PythonBackendClientException;
import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.dto.Core.PersonDTO;
import com.dat.ai_receptionist_web.dto.Operation.CheckInStudentProjection;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.enums.ErrorCode;
import com.dat.ai_receptionist_web.enums.Core.CoachStatus;
import com.dat.ai_receptionist_web.enums.Core.StudentStatus;
import com.dat.ai_receptionist_web.mapper.Core.PersonMapper;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.service.Operation.CoachTimesheetService;
import com.dat.ai_receptionist_web.service.Operation.StudentAttendanceService;
import com.dat.ai_receptionist_web.util.error.AppException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository personRepository;
    private final PersonMapper personMapper;
    private final PythonBackendClient pythonBackendClient;
    private final StudentAttendanceService studentAttendanceService;
    private final CoachTimesheetService coachTimesheetService;

    @Value("${FACE_MATCH_THRESHOLD:0.70}")
    private float faceMatchThreshold = 0.70f;

    @PostConstruct
    void validateFaceMatchThreshold() {
        if (!Float.isFinite(faceMatchThreshold) || faceMatchThreshold < 0.0f || faceMatchThreshold > 1.0f) {
            throw new IllegalStateException("FACE_MATCH_THRESHOLD must be between 0 and 1");
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<PersonDTO.SearchItem> searchStudentsAndCoaches(String search, Pageable pageable) {
        String normalizedSearch = search == null || search.trim().isEmpty()
                ? null
                : "%" + search.trim().toLowerCase() + "%";
        Page<PersonRepository.PersonSearchProjection> people =
                personRepository.searchStudentsAndCoaches(normalizedSearch, pageable);
        return PageResponse.of(people, personMapper::toSearchItem);
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.FACE_IMAGE_INVALID);
        }

        String contentType = file.getContentType();

        if (contentType == null || !contentType.startsWith("image/")) {
            throw new AppException(ErrorCode.FACE_IMAGE_INVALID);
        }

        long maxSize = 10 * 1024 * 1024L;

        if (file.getSize() > maxSize) {
            throw new AppException(ErrorCode.FACE_IMAGE_INVALID);
        }
    }

    public PersonDTO.FaceCheckInResponse checkInByFaceImage(MultipartFile file) {
        return checkInByFaceImage(file, null);
    }

    public PersonDTO.FaceCheckInResponse checkInByFaceImage(MultipartFile file, UUID classSessionId) {
        PythonBackendClient.FaceEmbeddingResponse response = generateFaceEmbedding(file);
        NearestPersonMatch nearestPerson = findNearestPersonByEmbedding(
                response.embedding(),
                faceMatchThreshold
        );
        if (nearestPerson.person() == null) {
            throw new AppException(ErrorCode.FACE_NOT_MATCHED);
        }
        PersonRepository.FaceCheckInSubjectProjection subject = personRepository
                .findFaceCheckInSubjectByPersonId(nearestPerson.person().getPersonId())
                .orElseThrow(() -> new AppException(ErrorCode.FACE_CHECK_IN_PERSON_TYPE_INVALID));

        boolean isStudent = subject.getStudentCode() != null;
        boolean isCoach = subject.getStaffCode() != null;
        if (isStudent == isCoach) {
            throw new AppException(ErrorCode.FACE_CHECK_IN_PERSON_TYPE_INVALID);
        }

        if (isStudent) {
            return studentAttendanceService.createAttendanceRecordForResolvedStudent(
                    new ResolvedStudentCheckIn(subject),
                    classSessionId
            );
        }
        return coachTimesheetService.checkInResolvedCoach(
                subject.getPersonId(),
                toCoachStatus(subject.getCoachStatus()),
                classSessionId
        );
    }

    /**
     * Finds the closest stored face embedding using pgvector cosine distance.
     * A missing candidate and a candidate below the requested threshold both return
     * a {@code null} person; the latter still returns its confidence for diagnostics.
     */
    public NearestPersonMatch findNearestPersonByEmbedding(List<Float> embeddingVector, float threshold) {
        if (embeddingVector == null || embeddingVector.isEmpty()) {
            return new NearestPersonMatch(null, 0.0f);
        }

        List<PersonRepository.NearestFaceMatchProjection> matches = personRepository
                .findNearestFaceMatch(toPgVectorLiteral(embeddingVector));
        if (matches.isEmpty()) {
            return new NearestPersonMatch(null, 0.0f);
        }

        PersonRepository.NearestFaceMatchProjection match = matches.getFirst();
        float distance = match.getDistance() == null ? 1.0f : match.getDistance().floatValue();
        float confidence = Math.clamp(1.0f - distance, 0.0f, 1.0f);
        if (confidence < threshold) {
            return new NearestPersonMatch(null, confidence);
        }

        return personRepository.findById(match.getPersonId())
                .map(person -> new NearestPersonMatch(person, confidence))
                .orElseGet(() -> new NearestPersonMatch(null, confidence));
    }

    private static String toPgVectorLiteral(List<Float> embeddingVector) {
        StringBuilder vector = new StringBuilder("[");
        for (int index = 0; index < embeddingVector.size(); index++) {
            Float value = embeddingVector.get(index);
            if (value == null || !Float.isFinite(value)) {
                throw new IllegalArgumentException("Face embedding must contain only finite values");
            }
            if (index > 0) {
                vector.append(',');
            }
            vector.append(value);
        }
        return vector.append(']').toString();
    }

    private static java.util.Optional<ErrorCode> resolveFaceEmbeddingErrorCode(String backendErrorCode) {
        if (!StringUtils.hasText(backendErrorCode)) {
            return java.util.Optional.empty();
        }
        try {
            ErrorCode errorCode = ErrorCode.valueOf(backendErrorCode.trim().toUpperCase(java.util.Locale.ROOT));
            return switch (errorCode) {
                case INVALID_IMAGE_FILE, EMPTY_IMAGE_FILE, FILE_TOO_LARGE, UNSUPPORTED_IMAGE_TYPE,
                     IMAGE_DECODE_FAILED, FACE_NOT_DETECTED, MULTIPLE_FACES_DETECTED,
                     FACE_EMBEDDING_FAILED, INVALID_EMBEDDING, MODEL_NOT_INITIALIZED, INTERNAL_ERROR ->
                        java.util.Optional.of(errorCode);
                default -> java.util.Optional.empty();
            };
        } catch (IllegalArgumentException exception) {
            return java.util.Optional.empty();
        }
    }

    private static StudentStatus toStudentStatus(String status) {
        try {
            return StudentStatus.valueOf(status);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new AppException(ErrorCode.FACE_CHECK_IN_PERSON_TYPE_INVALID, exception);
        }
    }

    private static CoachStatus toCoachStatus(String status) {
        try {
            return CoachStatus.valueOf(status);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new AppException(ErrorCode.FACE_CHECK_IN_PERSON_TYPE_INVALID, exception);
        }
    }

    public PersonDTO.FaceEmbeddingUpdateResponse updateFaceEmbedding(MultipartFile file, UUID personId) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new AppException(ErrorCode.PERSON_NOT_FOUND));
        PythonBackendClient.FaceEmbeddingResponse response = generateFaceEmbedding(file);

        float[] embedding = new float[response.embedding().size()];
        for (int index = 0; index < embedding.length; index++) {
            embedding[index] = response.embedding().get(index);
        }
        person.setFaceEmbedding(embedding);
        Person savedPerson = personRepository.saveAndFlush(person);
        return PersonDTO.FaceEmbeddingUpdateResponse.builder()
                .personId(savedPerson.getPersonId())
                .dimension(response.dimension())
                .model(response.model())
                .updatedAt(savedPerson.getUpdatedAt())
                .build();
    }

    private PythonBackendClient.FaceEmbeddingResponse generateFaceEmbedding(MultipartFile file) {
        validateImage(file);
        try {
            return pythonBackendClient.generateFaceEmbedding(file);
        } catch (PythonBackendClientException exception) {
            ErrorCode errorCode = resolveFaceEmbeddingErrorCode(exception.getBackendErrorCode())
                    .orElseGet(() -> exception.getFailureType().isUnavailable()
                            ? ErrorCode.PYTHON_BACKEND_UNAVAILABLE
                            : ErrorCode.PYTHON_BACKEND_ERROR);
            throw new AppException(errorCode, exception);
        }
    }

    private record ResolvedStudentCheckIn(PersonRepository.FaceCheckInSubjectProjection subject)
            implements CheckInStudentProjection {

        @Override
        public java.util.UUID getPersonId() {
            return subject.getPersonId();
        }

        @Override
        public String getStudentCode() {
            return subject.getStudentCode();
        }

        @Override
        public StudentStatus getStudentStatus() {
            return toStudentStatus(subject.getStudentStatus());
        }

        @Override
        public String getFullName() {
            return subject.getFullName();
        }
    }

    public record NearestPersonMatch(Person person, float confidence) {
    }
}
