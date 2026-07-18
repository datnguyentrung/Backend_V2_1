package com.dat.backend_v2_1.service.Security;

import com.dat.backend_v2_1.domain.Core.Person;
import com.dat.backend_v2_1.domain.Security.User;
import com.dat.backend_v2_1.domain.Security.UserProfile;
import com.dat.backend_v2_1.dto.Security.UserProfileDTO;
import com.dat.backend_v2_1.enums.Security.RelationshipType;
import com.dat.backend_v2_1.repository.Core.PersonRepository;
import com.dat.backend_v2_1.repository.Security.UserProfileRepository;
import com.dat.backend_v2_1.util.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
