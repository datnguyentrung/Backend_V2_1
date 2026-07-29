package com.dat.ai_receptionist_web.service.Core;

import com.dat.ai_receptionist_web.client.PythonBackendClient;
import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.mapper.Core.PersonMapper;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.service.Operation.SupabaseStorageService;
import com.dat.ai_receptionist_web.util.error.AppException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PersonServiceFaceImageTest {

    private final PersonRepository personRepository = mock(PersonRepository.class);
    private final PythonBackendClient pythonBackendClient = mock(PythonBackendClient.class);
    private final SupabaseStorageService storageService = mock(SupabaseStorageService.class);
    private final PersonService personService = new PersonService(
            personRepository,
            mock(PersonMapper.class),
            pythonBackendClient,
            storageService
    );

    @AfterEach
    void clearTransactionSynchronization() {
        TransactionSynchronizationManager.clear();
    }

    @Test
    void attachesEmbeddingAndVersionedPathThenDeletesOldPathAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        Person person = personWithExistingImage();
        MockMultipartFile file = imageFile();
        when(storageService.validateImage(file)).thenReturn(validatedImage());
        when(pythonBackendClient.generateFaceEmbedding(file)).thenReturn(embeddingResponse());
        when(storageService.uploadPersonFaceImage(eq(person.getPersonId()), any(SupabaseStorageService.ValidatedImage.class)))
                .thenReturn("persons/new/new.webp");

        PersonService.PersonFaceData result = personService.processAndAttachFaceImage(person, file);

        assertEquals("persons/new/new.webp", result.imagePath());
        assertEquals("persons/new/new.webp", person.getFaceImagePath());
        assertArrayEquals(new float[512], person.getFaceEmbedding());
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(storageService).deleteObject("persons/old/old.webp");
    }

    @Test
    void doesNotUploadWhenPythonRejectsImage() {
        TransactionSynchronizationManager.initSynchronization();
        Person person = personWithExistingImage();
        MockMultipartFile file = imageFile();
        when(storageService.validateImage(file)).thenReturn(validatedImage());
        when(pythonBackendClient.generateFaceEmbedding(file)).thenThrow(
                new PythonBackendClient.PythonBackendClientException(
                        PythonBackendClient.FailureType.REJECTED, "rejected", "FACE_NOT_DETECTED", null)
        );

        assertThrows(AppException.class, () -> personService.processAndAttachFaceImage(person, file));

        verify(storageService, never()).uploadPersonFaceImage(
                eq(person.getPersonId()), any(SupabaseStorageService.ValidatedImage.class)
        );
        assertEquals("persons/old/old.webp", person.getFaceImagePath());
    }

    @Test
    void deletesNewObjectWhenTransactionRollsBack() {
        TransactionSynchronizationManager.initSynchronization();
        Person person = personWithExistingImage();
        MockMultipartFile file = imageFile();
        when(storageService.validateImage(file)).thenReturn(validatedImage());
        when(pythonBackendClient.generateFaceEmbedding(file)).thenReturn(embeddingResponse());
        when(storageService.uploadPersonFaceImage(eq(person.getPersonId()), any(SupabaseStorageService.ValidatedImage.class)))
                .thenReturn("persons/new/new.webp");

        personService.processAndAttachFaceImage(person, file);
        TransactionSynchronizationManager.getSynchronizations().forEach(
                synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK)
        );

        verify(storageService).deleteObject("persons/new/new.webp");
    }

    private static Person personWithExistingImage() {
        Person person = new Person();
        person.setPersonId(UUID.randomUUID());
        person.setFaceImagePath("persons/old/old.webp");
        person.setFaceEmbedding(new float[512]);
        return person;
    }

    private static MockMultipartFile imageFile() {
        return new MockMultipartFile("file", "face.webp", "image/webp", new byte[]{'R', 'I', 'F', 'F'});
    }

    private static SupabaseStorageService.ValidatedImage validatedImage() {
        return new SupabaseStorageService.ValidatedImage(
                MediaType.parseMediaType("image/webp"), "webp", new byte[]{'R', 'I', 'F', 'F'}
        );
    }

    private static PythonBackendClient.FaceEmbeddingResponse embeddingResponse() {
        return new PythonBackendClient.FaceEmbeddingResponse(true, java.util.Collections.nCopies(512, 0.0f), 512, "model", null, null);
    }
}
