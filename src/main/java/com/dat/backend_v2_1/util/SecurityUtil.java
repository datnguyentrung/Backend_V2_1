package com.dat.backend_v2_1.util;


import com.dat.backend_v2_1.dto.Security.LoginRes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class SecurityUtil {
    private final JwtEncoder jwtEncoder;

    public SecurityUtil(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    public static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS512;

    @Value("${jwt.base64-secret}")
    private String jwtKey;

    @Value("${jwt.access-token-validity-in-seconds}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpiration;

    public String createAccessToken(UUID idUser, LoginRes.UserLogin userLogin) {
        Instant now = Instant.now();
        Instant validity = now.plus(this.accessTokenExpiration, ChronoUnit.SECONDS);

        JwtClaimsSet claimsSet = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(validity)
                .subject(idUser.toString())
                .claim("user", userLogin)
                .claim("role", userLogin.getRole())
                .build();

        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claimsSet))
                .getTokenValue();
    }

//    public String createRefreshToken(UUID idUser, LoginRes userLogin) {
//        Instant now = Instant.now();
//        Instant validity = now.plus(this.refreshTokenExpiration, ChronoUnit.SECONDS);
//
//        JwtClaimsSet claimsSet = JwtClaimsSet.builder()
//                .issuedAt(now)
//                .expiresAt(validity)
//                .subject(idUser.toString())
//                .claim("user", userLogin.getUser())
//                .claim("role", userLogin.getUser().getRole())
//                .claim("id_device", userLogin.getIdDevice())
//                .build();
//
//        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
//        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claimsSet))
//                .getTokenValue();
//    }

    public static Optional<String> getCurrentUserLogin() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return Optional.ofNullable(extractPrincipal(securityContext.getAuthentication()));
    }

    private static String extractPrincipal(Authentication authentication) {
        if (authentication == null) {
            return null;
        } else if (authentication.getPrincipal() instanceof UserDetails springSecurityUser) {
            return springSecurityUser.getUsername();
        } else if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        } else if (authentication.getPrincipal() instanceof String s) {
            return s;
        }
        return null;
    }

    public static Optional<String> getCurrentUserJWT() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return Optional.ofNullable(securityContext.getAuthentication())
                .filter(authentication -> authentication.getCredentials() instanceof String)
                .map(authentication -> (String) authentication.getCredentials());
    }

    private SecretKey getSecretKey() {
        byte[] keyBytes = Base64.from(jwtKey).decode();
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, JWT_ALGORITHM.getName());
    }

    public Jwt checkValidRefreshToken(String refreshToken) {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(
                getSecretKey()).macAlgorithm(JWT_ALGORITHM).build();
        try {
            return jwtDecoder.decode(refreshToken);
//            System.out.println(">>> Refresh Token ok!");
        } catch (Exception e) {
            System.out.println(">>> Refresh Token error: " + e.getMessage());
            throw e;
        }
    }

    public static Optional<String> getCurrentUserRole() {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return Optional.empty();
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst();
    }

    public static Optional<String> getCurrentUserStatus() {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            // Lấy user object từ claims
            Object userClaim = jwt.getClaim("user");
            if (userClaim instanceof Map<?, ?> userMap) {
                Object status = userMap.get("status");
                return status != null ? Optional.of(status.toString()) : Optional.empty();
            }
        }

        return Optional.empty();
    }

    public static LoginRes.UserLogin getCurrentUser(Jwt jwt) {
        Map<String, Object> userMap = jwt.getClaim("user");

        // Dùng ObjectMapper để convert Map sang Object
        ObjectMapper mapper = new ObjectMapper();

        return mapper.convertValue(userMap, LoginRes.UserLogin.class);
    }
}