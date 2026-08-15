package com.dat.ai_receptionist_web.service.Core;

import com.dat.ai_receptionist_web.client.PythonBackendClient;
import com.dat.ai_receptionist_web.client.PythonBackendClient.PythonBackendClientException;
import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.dto.Core.PersonDTO;
import com.dat.ai_receptionist_web.dto.Core.PersonDTO.PersonCreationData;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.enums.ErrorCode;
import com.dat.ai_receptionist_web.mapper.Core.PersonMapper;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.service.Operation.SupabaseStorageService;
import com.dat.ai_receptionist_web.util.converter.NameConverter;
import com.dat.ai_receptionist_web.util.error.AppException;
import com.dat.ai_receptionist_web.util.error.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.List;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository personRepository;
    private final PersonMapper personMapper;
    private final PythonBackendClient pythonBackendClient;
    private final SupabaseStorageService supabaseStorageService;
    private final PersonAvatarUrlCacheService avatarUrlCacheService;

    @Value("${FACE_MATCH_THRESHOLD:0.70}")
    private float faceMatchThreshold = 0.70f;

    @PostConstruct
    void validateFaceMatchThreshold() {
        if (!Float.isFinite(faceMatchThreshold) || faceMatchThreshold < 0.0f || faceMatchThreshold > 1.0f) {
            throw new IllegalStateException("FACE_MATCH_THRESHOLD must be between 0 and 1");
        }
    }

    /**
     * Applies and persists the common Person state for a concrete subtype.
     *
     * <p>With {@code InheritanceType.JOINED}, persisting a {@code Student} or
     * {@code Coach} through this repository writes both {@code core.person} and
     * its subtype row. The subtype-specific fields must therefore be populated
     * before this method is called.</p>
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public <T extends Person> T createPerson(T person, PersonCreationData data) {
        if (data.nationalCode() != null && personRepository.existsByNationalCode(data.nationalCode())) {
            throw new BusinessException("National code already exists");
        }

        person.setFullName(NameConverter.formatVietnameseName(data.fullName()));
        person.setBirthDate(data.birthDate());
        person.setBelt(data.belt());
        person.setNationalCode(data.nationalCode());
        person.setEmail(data.email());
        return personRepository.save(person);
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

        // The vector query has already produced a stable person id. Loading Person here
        // materializes the face_embedding float array only to read that same id again.
        return new NearestPersonMatch(match.getPersonId(), confidence);
    }

    public NearestPersonMatch findNearestPersonByEmbedding(List<Float> embeddingVector) {
        return findNearestPersonByEmbedding(embeddingVector, faceMatchThreshold);
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

    @Transactional(rollbackFor = Exception.class)
    public PersonDTO.FaceEmbeddingUpdateResponse updateFaceEmbedding(MultipartFile file, UUID personId) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new AppException(ErrorCode.PERSON_NOT_FOUND));
        PersonFaceData faceData = processAndAttachFaceImage(person, file);
        Person savedPerson = personRepository.saveAndFlush(person);
        String avatarUrl = getPublicFaceImageUrl(savedPerson.getFaceImagePath());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                avatarUrlCacheService.put(savedPerson.getPersonId(), avatarUrl);
            }
        });
        return PersonDTO.FaceEmbeddingUpdateResponse.builder()
                .personId(savedPerson.getPersonId())
                .dimension(faceData.dimension())
                .model(faceData.model())
                .faceImagePath(savedPerson.getFaceImagePath())
                .avatarUrl(avatarUrl)
                .updatedAt(savedPerson.getUpdatedAt())
                .build();
    }

    /**
     * Generates an embedding and uploads a versioned private image, then attaches both to a managed Person.
     * The caller owns the database transaction; uploaded storage is compensated if that transaction rolls back.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public PersonFaceData processAndAttachFaceImage(Person person, MultipartFile file) {
        if (person.getPersonId() == null) {
            throw new IllegalStateException("Person must have an id before processing a face image");
        }

        SupabaseStorageService.ValidatedImage image = supabaseStorageService.validateImage(file);
        PythonBackendClient.FaceEmbeddingResponse response = generateFaceEmbedding(file);
        float[] embedding = toEmbeddingArray(response);
        String oldPath = person.getFaceImagePath();
        String uploadedPath = null;

        try {
            uploadedPath = supabaseStorageService.uploadPersonFaceImage(person.getPersonId(), image);
            person.setFaceEmbedding(embedding);
            person.setFaceImagePath(uploadedPath);
            registerStorageCompensation(person.getPersonId(), uploadedPath, oldPath);
            return new PersonFaceData(embedding, uploadedPath, response.dimension(), response.model());
        } catch (RuntimeException exception) {
            if (uploadedPath != null) {
                cleanupObjectAfterFailure(person.getPersonId(), uploadedPath, "immediate rollback");
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public PersonDTO.FaceImageUrlResponse getFaceImageUrl(UUID personId) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new AppException(ErrorCode.PERSON_NOT_FOUND));
        return new PersonDTO.FaceImageUrlResponse(getPublicFaceImageUrl(person.getFaceImagePath()));
    }

    public String getPublicFaceImageUrl(String faceImagePath) {
        return supabaseStorageService.getPublicUrl(faceImagePath);
    }

    public PythonBackendClient.FaceEmbeddingResponse generateFaceEmbedding(MultipartFile file) {
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

    @Transactional
    public void deleteFaceEmbedding(UUID personId) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new AppException(ErrorCode.PERSON_NOT_FOUND));

        String oldPath = person.getFaceImagePath();
        person.setFaceEmbedding(null);
        person.setFaceImagePath(null);
        personRepository.saveAndFlush(person);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                avatarUrlCacheService.remove(personId);
            }
        });
        if (oldPath != null) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cleanupObjectAfterFailure(personId, oldPath, "delete after commit");
                }
            });
        }
    }

    private static float[] toEmbeddingArray(PythonBackendClient.FaceEmbeddingResponse response) {
        if (response.dimension() == null || response.dimension() != 512
                || response.embedding() == null || response.embedding().size() != 512) {
            throw new AppException(ErrorCode.INVALID_EMBEDDING);
        }
        float[] embedding = new float[512];
        for (int index = 0; index < embedding.length; index++) {
            Float value = response.embedding().get(index);
            if (value == null || !Float.isFinite(value)) {
                throw new AppException(ErrorCode.INVALID_EMBEDDING);
            }
            embedding[index] = value;
        }
        return embedding;
    }

    private void registerStorageCompensation(UUID personId, String uploadedPath, String oldPath) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (oldPath != null && !oldPath.equals(uploadedPath)) {
                    cleanupObjectAfterFailure(personId, oldPath, "replace after commit");
                }
            }

            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    cleanupObjectAfterFailure(personId, uploadedPath, "transaction rollback");
                }
            }
        });
    }

    private void cleanupObjectAfterFailure(UUID personId, String objectPath, String stage) {
        try {
            supabaseStorageService.deleteObject(objectPath);
        } catch (RuntimeException cleanupException) {
            // Cleanup must never replace the original database or Python/storage failure.
            org.slf4j.LoggerFactory.getLogger(PersonService.class).error(
                    "Face-image cleanup failed: personId={}, objectPath={}, stage={}, exceptionType={}",
                    personId, objectPath, stage, cleanupException.getClass().getSimpleName(), cleanupException);
        }
    }

    /**

     * Tìm Person bằng ảnh khuôn mặt hoặc mã định danh.
     * <p>
     * Ưu tiên nhận diện bằng ảnh; nếu không tìm thấy thì fallback sang {@code personCode}.
     * Mã {@code VQT_} là HLV, {@code VQ_} là học viên.
     *
     * @param file ảnh khuôn mặt, có thể {@code null}
     * @param personCode mã học viên (studentCode) hoặc HLV (StaffCode)
     * @return thông tin Person, hoặc {@code null} nếu không tìm thấy
     */
    @Transactional(readOnly = true)
    public PersonDTO.PersonResponse identifyPerson(MultipartFile file, String personCode) {
        if (file != null) {
            PythonBackendClient.FaceEmbeddingResponse embeddingResponse = generateFaceEmbedding(file);
            NearestPersonMatch nearestPerson = findNearestPersonByEmbedding(embeddingResponse.embedding());
            if (nearestPerson.personId() == null) {
                throw new AppException(ErrorCode.FACE_NOT_MATCHED);
            }
            return toPersonResponse(nearestPerson.personId());
        }

        if (!StringUtils.hasText(personCode)) {
            throw new AppException(ErrorCode.INVALID_IDENTIFICATION_REQUEST);
        }

        List<UUID> personIds = personRepository.findPersonIdsByPersonCode(personCode.trim());
        if (personIds.isEmpty()) {
            throw new AppException(ErrorCode.PERSON_NOT_FOUND);
        }
        if (personIds.size() > 1) {
            throw new AppException(ErrorCode.FACE_CHECK_IN_PERSON_TYPE_INVALID);
        }
        return toPersonResponse(personIds.getFirst());
    }

    private PersonDTO.PersonResponse toPersonResponse(UUID personId) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new AppException(ErrorCode.PERSON_NOT_FOUND));
        return personMapper.toPersonResponse(person);
    }


    public record NearestPersonMatch(UUID personId, float confidence) {
    }

    public record PersonFaceData(float[] embedding, String imagePath, int dimension, String model) {
    }
}
