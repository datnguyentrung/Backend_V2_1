package com.dat.ai_receptionist_web.service.Core;

import com.dat.ai_receptionist_web.domain.Core.Coach;
import com.dat.ai_receptionist_web.domain.Operation.CoachAssignment;
import com.dat.ai_receptionist_web.domain.Security.UserProfile;
import com.dat.ai_receptionist_web.dto.Core.CoachReqDTO;
import com.dat.ai_receptionist_web.dto.Core.CoachResDTO;
import com.dat.ai_receptionist_web.dto.Core.PersonDTO.PersonCreationData;
import com.dat.ai_receptionist_web.dto.Operation.CoachAssignmentResDTO;
import com.dat.ai_receptionist_web.enums.Core.CoachStatus;
import com.dat.ai_receptionist_web.enums.Operation.CoachAssignmentStatus;
import com.dat.ai_receptionist_web.mapper.Core.CoachMapper;
import com.dat.ai_receptionist_web.mapper.Operation.CoachAssignmentMapper;
import com.dat.ai_receptionist_web.repository.Core.CoachRepository;
import com.dat.ai_receptionist_web.repository.Security.UserProfileRepository;
import com.dat.ai_receptionist_web.service.Operation.CoachAssignmentService;
import com.dat.ai_receptionist_web.util.AccountUtil;
import com.dat.ai_receptionist_web.util.converter.NameConverter;
import com.dat.ai_receptionist_web.util.error.BusinessException;
import com.dat.ai_receptionist_web.util.error.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CoachService {
    private final CoachRepository coachRepository;
    private final CoachMapper coachMapper;
    private final CoachAssignmentService coachAssignmentService;
    private final CoachAssignmentMapper coachAssignmentMapper;
    private final UserProfileRepository userProfileRepository;
    private final PersonService personService;

    public Coach validateCoachAndGetActive(UUID coachId) {
        Coach coach = getCoachById(coachId);
        if (coach.getCoachStatus() != CoachStatus.ACTIVE) {
            throw new AccessDeniedException("Coach is not active");
        }
        return coach;
    }

    public Coach getCoachById(UUID coachId) {
        return coachRepository.findById(coachId)
                .orElseThrow(() -> new BusinessException("Coach not found: " + coachId));
    }

    public Coach getCoachById(String coachId) {
        return getCoachById(UUID.fromString(coachId));
    }

    public Coach getCoachByStaffCode(String staffCode) {
        return coachRepository.findByStaffCode(staffCode)
                .orElseThrow(() -> new UserNotFoundException("Coach not found: " + staffCode));
    }

    public CoachResDTO.CoachDetail getCoachDetail(UUID personId) {
        Coach coach = getCoachById(personId);
        List<CoachAssignmentResDTO.SimpleResponse> assignments =
                coachAssignmentService.findCoachAssignmentsByCoachId(personId, CoachAssignmentStatus.ACTIVE);
        return withAvatarUrl(coachMapper.toCoachDetailWithAssignments(coach, assignments, getUserProfiles(coach)), coach);
    }

    @Transactional(readOnly = true)
    public CoachResDTO.CoachDetail getCoachDetail(String staffCode) {
        Coach coach = getCoachByStaffCode(staffCode);
        List<CoachAssignmentResDTO.SimpleResponse> assignments =
                coachAssignmentService.findCoachAssignmentsByCoachId(coach.getPersonId(), CoachAssignmentStatus.ACTIVE);
        return withAvatarUrl(coachMapper.toCoachDetailWithAssignments(coach, assignments, getUserProfiles(coach)), coach);
    }

    @Caching(put = {})
    @Transactional(rollbackFor = Exception.class)
    public CoachResDTO.CoachDetail createCoach(CoachReqDTO.CoachCreate createDTO) {
        return createCoach(createDTO, null);
    }

    @Caching(put = {})
    @Transactional(rollbackFor = Exception.class)
    public CoachResDTO.CoachDetail createCoach(CoachReqDTO.CoachCreate createDTO, MultipartFile file) {
        Coach newCoach = new Coach();
        newCoach.setCoachStatus(createDTO.getCoachStatus() != null ? createDTO.getCoachStatus() : CoachStatus.ACTIVE);

        String generatedCode = AccountUtil.getUserCode(createDTO.getFullName(), createDTO.getBirthDate(), "VQT");
        while (coachRepository.existsByStaffCode(generatedCode)) {
            generatedCode = generatedCode + "_" + RandomStringUtils.secure().nextNumeric(2);
        }
        newCoach.setStaffCode(generatedCode);
        newCoach = personService.createPerson(newCoach, new PersonCreationData(
                createDTO.getFullName(),
                createDTO.getBirthDate(),
                createDTO.getBelt(),
                null,
                createDTO.getEmail()
        ));
        if (file != null && !file.isEmpty()) {
            personService.processAndAttachFaceImage(newCoach, file);
        }

        List<CoachAssignmentResDTO.SimpleResponse> assignmentResponses = new ArrayList<>();
        if (createDTO.getAssignmentRequest() != null
                && createDTO.getAssignmentRequest().getScheduleIds() != null
                && !createDTO.getAssignmentRequest().getScheduleIds().isEmpty()) {
            createDTO.getAssignmentRequest().setCoachId(String.valueOf(newCoach.getPersonId()));
            List<CoachAssignment> assignments = coachAssignmentService.createCoachAssignment(createDTO.getAssignmentRequest());
            assignmentResponses = assignments.stream().map(coachAssignmentMapper::toSimpleResponse).toList();
        }

        log.info("Created coach successfully with code: {}", generatedCode);
        return withAvatarUrl(coachMapper.toCoachDetailWithAssignments(newCoach, assignmentResponses, List.of()), newCoach);
    }

    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {})
    public CoachResDTO.CoachDetail updateCoach(CoachReqDTO.CoachUpdate updateDTO) {
        return updateCoach(updateDTO, null);
    }

    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {})
    public CoachResDTO.CoachDetail updateCoach(CoachReqDTO.CoachUpdate updateDTO, MultipartFile file) {
        Coach coach = getCoachById(updateDTO.getPersonId());
        if (updateDTO.getBirthDate() != null) coach.setBirthDate(updateDTO.getBirthDate());
        if (updateDTO.getBelt() != null) coach.setBelt(updateDTO.getBelt());
        if (updateDTO.getFullName() != null) coach.setFullName(NameConverter.formatVietnameseName(updateDTO.getFullName()));
        if (updateDTO.getCoachStatus() != null) coach.setCoachStatus(updateDTO.getCoachStatus());
        if (updateDTO.getNationalCode() != null) coach.setNationalCode(updateDTO.getNationalCode());
        if (file != null && !file.isEmpty()) {
            personService.processAndAttachFaceImage(coach, file);
        }

        Coach updatedCoach = coachRepository.save(coach);
        List<CoachAssignmentResDTO.SimpleResponse> assignments =
                coachAssignmentService.findCoachAssignmentsByCoachId(updatedCoach.getPersonId(), CoachAssignmentStatus.ACTIVE);
        return withAvatarUrl(coachMapper.toCoachDetailWithAssignments(updatedCoach, assignments, getUserProfiles(updatedCoach)), updatedCoach);
    }

    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {})
    public void deleteCoach(UUID personId) {
        Coach coach = getCoachById(personId);
        if (coach.getCoachStatus() == CoachStatus.INACTIVE) {
            throw new BusinessException("Coach is already inactive");
        }
        coach.setCoachStatus(CoachStatus.INACTIVE);
        coachRepository.save(coach);
    }

    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {})
    public void permanentlyDeleteCoach(String staffCode) {
        coachRepository.delete(getCoachByStaffCode(staffCode));
    }

    public List<CoachResDTO.CoachDetail> getAllCoaches() {
        List<Coach> coaches = coachRepository.findAll();
        if (coaches.isEmpty()) return new ArrayList<>();

        List<UUID> coachIds = coaches.stream().map(Coach::getPersonId).toList();
        List<CoachAssignment> allActiveAssignments =
                coachAssignmentService.getAllCoachAssignmentsByListCoachIds(coachIds, CoachAssignmentStatus.ACTIVE);

        Map<UUID, List<CoachAssignment>> assignmentsByCoachId = allActiveAssignments.stream()
                .collect(Collectors.groupingBy(ca -> ca.getCoach().getPersonId()));

        Map<UUID, List<UserProfile>> userProfilesByCoachId = userProfileRepository
                .findAllByPerson_PersonIdIn(coachIds)
                .stream()
                .collect(Collectors.groupingBy(profile -> profile.getPerson().getPersonId()));

        return coaches.stream().map(coach -> {
            List<CoachAssignmentResDTO.SimpleResponse> assignmentResponses =
                    assignmentsByCoachId.getOrDefault(coach.getPersonId(), new ArrayList<>()).stream()
                            .map(coachAssignmentMapper::toSimpleResponse)
                            .toList();
            return withAvatarUrl(
                    coachMapper.toCoachDetailWithAssignments(
                            coach,
                            assignmentResponses,
                            userProfilesByCoachId.getOrDefault(coach.getPersonId(), List.of())
                    ),
                    coach
            );
        }).toList();
    }

    private CoachResDTO.CoachDetail withAvatarUrl(CoachResDTO.CoachDetail response, Coach coach) {
        response.setAvatarUrl(personService.getPublicFaceImageUrl(coach.getFaceImagePath()));
        return response;
    }

    private List<UserProfile> getUserProfiles(Coach coach) {
        return userProfileRepository.findAllByPerson_PersonId(coach.getPersonId());
    }
}
