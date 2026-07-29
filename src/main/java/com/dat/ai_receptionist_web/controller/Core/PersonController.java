package com.dat.ai_receptionist_web.controller.Core;

import com.dat.ai_receptionist_web.dto.Core.PersonDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.service.Core.PersonService;
import com.dat.ai_receptionist_web.service.Operation.FaceCheckInService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/persons")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;
    private final FaceCheckInService faceCheckInService;

    @PreAuthorize("@securityRule.isManagerSenior(authentication)")
    @GetMapping
    public ResponseEntity<PageResponse<PersonDTO.SearchItem>> searchPersons(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fullName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.DESC.name())
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(personService.searchStudentsAndCoaches(search, pageable));
    }

    @PostMapping(
            value = "/identify",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("@securityRule.isCoach(authentication) or @securityRule.isSystem(authentication)")
    public ResponseEntity<PersonDTO.PersonResponse> identifyPerson(
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "personCode", required = false) String personCode
    ) {
        return ResponseEntity.ok(
                personService.identifyPerson(file, personCode)
        );
    }

    @PostMapping(
            value = "/face-check-in",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("@securityRule.isCoach(authentication) or @securityRule.isSystem(authentication)")
    public ResponseEntity<PersonDTO.FaceCheckInResult> checkInByFaceImage(
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(faceCheckInService.checkInByFaceImage(file));
    }

    @GetMapping("/face-image")
    public ResponseEntity<?> getByFaceImage(@RequestPart("file") MultipartFile file) {
        // Implementation for getting person by face image
        return ResponseEntity.ok().build();
    }

    @PatchMapping(
            value = "/{personId}/face-embedding",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<PersonDTO.FaceEmbeddingUpdateResponse> updateFaceEmbedding(
            @PathVariable UUID personId,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(
                personService.updateFaceEmbedding(file, personId)
        );
    }

    @GetMapping("/{personId}/face-image-url")
    @PreAuthorize("@securityRule.isManagerSenior(authentication)")
    public ResponseEntity<PersonDTO.FaceImageUrlResponse> getFaceImageUrl(
            @PathVariable UUID personId
    ) {
        return ResponseEntity.ok(personService.getFaceImageUrl(personId));
    }

    @DeleteMapping("/{personId}/face-embedding")
    public ResponseEntity<Void> deleteFaceEmbedding(
            @PathVariable UUID personId
    ) {
        personService.deleteFaceEmbedding(personId);
        return ResponseEntity.noContent().build(); // HTTP 204
    }
}
