package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.domain.Security.UserProfile;
import com.dat.ai_receptionist_web.dto.Security.UserProfileDTO;
import com.dat.ai_receptionist_web.enums.Security.RelationshipType;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.repository.Security.UserProfileRepository;
import com.dat.ai_receptionist_web.util.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserService userService;
    private final PersonRepository personRepository;
    private final UserProfileRepository userProfileRepository;

    @Transactional(rollbackFor = Exception.class)
    public UserProfileDTO.Response createUserProfile(UserProfileDTO.CreateRequest request) {
        RelationshipType relationshipType = request.getRelationshipType() == null
                ? RelationshipType.OWNER
                : request.getRelationshipType();
        User user = userService.getUserById(request.getUserId());
        Person person = personRepository.findById(request.getPersonId())
                .orElseThrow(() -> new BusinessException("Person not found: " + request.getPersonId()));

        userProfileRepository
                .findByUser_UserIdAndPerson_PersonIdAndRelationshipTypeAndActiveTrue(
                        request.getUserId(),
                        request.getPersonId(),
                        relationshipType
                )
                .ifPresent(profile -> {
                    throw new BusinessException("User profile already exists");
                });

        UserProfile userProfile = UserProfile.builder()
                .user(user)
                .person(person)
                .relationshipType(relationshipType)
                .active(true)
                .build();

        return toResponse(userProfileRepository.save(userProfile));
    }

    private UserProfileDTO.Response toResponse(UserProfile userProfile) {
        return UserProfileDTO.Response.builder()
                .userProfileId(userProfile.getUserProfileId())
                .userId(userProfile.getUser().getUserId())
                .personId(userProfile.getPerson().getPersonId())
                .relationshipType(userProfile.getRelationshipType())
                .active(userProfile.getActive())
                .build();
    }

    public List<UserProfile> getAllByPersonIdAndActiveTrue(UUID personId) {
        return userProfileRepository.findAllByPerson_PersonIdAndActive(personId, true);
    }

    public List<UserProfile> getAllByPersonId(UUID personId) {
        return userProfileRepository.findAllByPerson_PersonId(personId);
    }
}
