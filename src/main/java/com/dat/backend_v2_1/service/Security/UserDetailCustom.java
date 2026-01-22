package com.dat.backend_v2_1.service.Security;

import com.dat.backend_v2_1.domain.Security.User;
import com.dat.backend_v2_1.enums.Security.UserStatus;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component("userDetailsService")
public class UserDetailCustom implements UserDetailsService {

    @Autowired
    private UserService userService;

    /**
     * Load user details by phone number for Spring Security authentication
     *
     * @param phoneNumber the phone number identifying the user
     * @return UserDetails object containing user authentication information
     * @throws UsernameNotFoundException if user is not found or account is not active
     */
    @Override
    public UserDetails loadUserByUsername(@NonNull String phoneNumber) throws UsernameNotFoundException {
        User user = userService.getUserByPhoneNumber(phoneNumber);

        if (user == null) {
            throw new UsernameNotFoundException("User not found with phone number: " + phoneNumber);
        }

        // Check if user account is active and not banned/deactivated
        boolean isAccountNonLocked = user.getStatus() != UserStatus.BANNED;
        boolean isEnabled = user.getStatus() == UserStatus.ACTIVE;

        // Build authorities from user role (Role.code already contains "ROLE_" prefix)
        SimpleGrantedAuthority authority = user.getRole() != null
                ? new SimpleGrantedAuthority(user.getRole().getCode())
                : new SimpleGrantedAuthority("ROLE_USER");

        // Use Spring Security's User class with builder pattern to avoid confusion with entity User
        return org.springframework.security.core.userdetails.User
                .withUsername(phoneNumber)
                .password(user.getPasswordHash())
                .authorities(Collections.singletonList(authority))
                .accountExpired(false)
                .accountLocked(!isAccountNonLocked)
                .credentialsExpired(false)
                .disabled(!isEnabled)
                .build();
    }
}