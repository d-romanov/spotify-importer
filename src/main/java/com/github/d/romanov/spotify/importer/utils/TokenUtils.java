package com.github.d.romanov.spotify.importer.utils;

import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.core.user.OAuth2User;
import reactor.core.publisher.Mono;

@UtilityClass
public class TokenUtils {

    public static Mono<OAuth2User> getPrincipal() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getPrincipal)
                .map(OAuth2User.class::cast);
    }
}
