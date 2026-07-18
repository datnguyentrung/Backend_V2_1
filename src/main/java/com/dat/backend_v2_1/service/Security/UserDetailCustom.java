package com.dat.backend_v2_1.service.Security;

import com.dat.backend_v2_1.domain.Security.User;
import com.dat.backend_v2_1.enums.Security.UserStatus;
import com.dat.backend_v2_1.util.PhoneNumberUtil;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("userDetailsService")
public class UserDetailCustom implements UserDetailsService {
    private final UserService userService;

    public UserDetailCustom(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(@NonNull String phoneNumber) throws UsernameNotFoundException {
        String normalizedPhone;
        try {
            normalizedPhone = PhoneNumberUtil.normalize(phoneNumber);
        } catch (IllegalArgumentException e) {
            throw new UsernameNotFoundException("Invalid phone number");
        }

        User user = userService.getUserWithRolesByPhoneNumber(normalizedPhone);
        boolean active = user.getStatus() == UserStatus.ACTIVE;
        boolean locked = user.getStatus() == UserStatus.LOCKED || user.getStatus() == UserStatus.BANNED;
        boolean disabled = user.getStatus() == UserStatus.DISABLED || user.getStatus() == UserStatus.DEACTIVATED
                || user.getStatus() == UserStatus.PENDING;

        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getCode()))
                .toList();

        return org.springframework.security.core.userdetails.User
                .withUsername(normalizedPhone)
                .password(user.getPasswordHash())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(locked)
                .credentialsExpired(false)
                .disabled(!active || disabled)
                .build();
    }
}
