package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.util.PhoneNumberUtil;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

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

        User user = userService.getUserByPhoneNumber(normalizedPhone);
        return new AuthenticatedUserPrincipal(user);
    }
}
