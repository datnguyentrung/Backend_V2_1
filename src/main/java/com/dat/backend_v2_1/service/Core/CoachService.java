package com.dat.backend_v2_1.service.Core;

import com.dat.backend_v2_1.domain.Core.Coach;
import com.dat.backend_v2_1.domain.Operation.CoachAssignment;
import com.dat.backend_v2_1.domain.Security.User;
import com.dat.backend_v2_1.domain.Security.UserProfile;
import com.dat.backend_v2_1.dto.Core.CoachReqDTO;
import com.dat.backend_v2_1.dto.Core.CoachResDTO;
import com.dat.backend_v2_1.dto.Operation.CoachAssignmentResDTO;
import com.dat.backend_v2_1.enums.Core.CoachStatus;
import com.dat.backend_v2_1.enums.Operation.CoachAssignmentStatus;
import com.dat.backend_v2_1.enums.Security.RelationshipType;
import com.dat.backend_v2_1.mapper.Core.CoachMapper;
import com.dat.backend_v2_1.mapper.Operation.CoachAssignmentMapper;
import com.dat.backend_v2_1.repository.Core.CoachRepository;
import com.dat.backend_v2_1.repository.Security.UserProfileRepository;
import com.dat.backend_v2_1.service.Operation.CoachAssignmentService;
import com.dat.backend_v2_1.util.AccountUtil;
import com.dat.backend_v2_1.util.converter.NameConverter;
import com.dat.backend_v2_1.util.error.BusinessException;
import com.dat.backend_v2_1.util.error.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public Coach validateCoachAndGetActive(String coachId) {
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
        return coachMapper.toCoachDetailWithAssignments(coach, assignments);
    }

    @Transactional(readOnly = true)
    public CoachResDTO.CoachDetail getCoachDetail(String staffCode) {
        Coach coach = getCoachByStaffCode(staffCode);
        List<CoachAssignmentResDTO.SimpleResponse> assignments =
                coachAssignmentService.findCoachAssignmentsByCoachId(coach.getPersonId(), CoachAssignmentStatus.ACTIVE);
        return coachMapper.toCoachDetailWithAssignments(coach, assignments);
    }

    @Caching(put = {})
    @Transactional(rollbackFor = Exception.class)
    public CoachResDTO.CoachDetail createCoach(CoachReqDTO.CoachCreate createDTO) {
        Coach newCoach = new Coach();
        newCoach.setFullName(NameConverter.formatVietnameseName(createDTO.getFullName()));
        newCoach.setBirthDate(createDTO.getBirthDate());
        newCoach.setBelt(createDTO.getBelt());
        newCoach.setEmail(createDTO.getEmail());
        newCoach.setCoachStatus(createDTO.getCoachStatus() != null ? createDTO.getCoachStatus() : CoachStatus.ACTIVE);

        String generatedCode = AccountUtil.getUserCode(createDTO.getFullName(), createDTO.getBirthDate(), "VQT");
        while (coachRepository.existsByStaffCode(generatedCode)) {
            generatedCode = generatedCode + "_" + RandomStringUtils.secure().nextNumeric(2);
        }
        newCoach.setStaffCode(generatedCode);
        newCoach = coachRepository.save(newCoach);

        List<CoachAssignmentResDTO.SimpleResponse> assignmentResponses = new ArrayList<>();
        if (createDTO.getAssignmentRequest() != null
                && createDTO.getAssignmentRequest().getScheduleIds() != null
                && !createDTO.getAssignmentRequest().getScheduleIds().isEmpty()) {
            createDTO.getAssignmentRequest().setCoachId(String.valueOf(newCoach.getPersonId()));
            List<CoachAssignment> assignments = coachAssignmentService.createCoachAssignment(createDTO.getAssignmentRequest());
            assignmentResponses = assignments.stream().map(coachAssignmentMapper::toSimpleResponse).toList();
        }

        log.info("Created coach successfully with code: {}", generatedCode);
        return coachMapper.toCoachDetailWithAssignments(newCoach, assignmentResponses);
    }

    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {})
    public CoachResDTO.CoachDetail updateCoach(CoachReqDTO.CoachUpdate updateDTO) {
        Coach coach = getCoachById(updateDTO.getPersonId());
        if (updateDTO.getBirthDate() != null) coach.setBirthDate(updateDTO.getBirthDate());
        if (updateDTO.getBelt() != null) coach.setBelt(updateDTO.getBelt());
        if (updateDTO.getFullName() != null) coach.setFullName(NameConverter.formatVietnameseName(updateDTO.getFullName()));
        if (updateDTO.getCoachStatus() != null) coach.setCoachStatus(updateDTO.getCoachStatus());
        if (updateDTO.getNationalCode() != null) coach.setNationalCode(updateDTO.getNationalCode());

        Coach updatedCoach = coachRepository.save(coach);
        List<CoachAssignmentResDTO.SimpleResponse> assignments =
                coachAssignmentService.findCoachAssignmentsByCoachId(updatedCoach.getPersonId(), CoachAssignmentStatus.ACTIVE);
        return coachMapper.toCoachDetailWithAssignments(updatedCoach, assignments);
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

        Map<UUID, User> usersByCoachId = userProfileRepository
                .findAllByPerson_PersonIdInAndRelationshipTypeAndActiveTrue(coachIds, RelationshipType.OWNER)
                .stream()
                .collect(Collectors.toMap(
                        profile -> profile.getPerson().getPersonId(),
                        UserProfile::getUser,
                        (current, ignored) -> current
                ));

        return coaches.stream().map(coach -> {
            List<CoachAssignmentResDTO.SimpleResponse> assignmentResponses =
                    assignmentsByCoachId.getOrDefault(coach.getPersonId(), new ArrayList<>()).stream()
                            .map(coachAssignmentMapper::toSimpleResponse)
                            .toList();
            return coachMapper.toCoachDetailWithAssignments(coach, assignmentResponses, usersByCoachId.get(coach.getPersonId()));
        }).toList();
    }
}
