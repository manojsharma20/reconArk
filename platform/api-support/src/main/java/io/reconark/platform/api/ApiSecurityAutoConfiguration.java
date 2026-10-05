package io.reconark.platform.api;

import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Every business API is a stateless OAuth2 resource server. Roles come from the {@code roles} claim
 * ({@code ONBOARDING_MAKER}, {@code RECON_ANALYST}, ...; architecture §10.1). In {@code dev-mode} (local only, refused
 * when the composition environment is prod) every request is authenticated as a local developer with all roles.
 */
@AutoConfiguration
@EnableMethodSecurity
public class ApiSecurityAutoConfiguration {

    static final List<String> ALL_ROLES = List.of(
            "ONBOARDING_MAKER", "ONBOARDING_CHECKER", "RECON_ANALYST", "RECON_SUPERVISOR", "REPORT_VIEWER",
            "REPORT_EXPORTER", "OPERATOR", "AUDITOR");

    @Bean
    @ConditionalOnMissingBean
    ProblemDetailsAdvice reconArkProblemDetails() {
        return new ProblemDetailsAdvice();
    }

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    SecurityFilterChain reconArkApiSecurity(
            HttpSecurity http,
            @Value("${reconark.security.dev-mode:false}") boolean devMode,
            @Value("${reconark.composition.environment:local}") String environment)
            throws Exception {
        http.csrf(csrf -> csrf.disable()) // stateless bearer-token API; the BFF owns CSRF for the browser
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h.contentSecurityPolicy(c -> c.policyDirectives("default-src 'none'; frame-ancestors 'none'")));
        if (devMode) {
            if ("prod".equalsIgnoreCase(environment)) {
                throw new IllegalStateException("RK-SEC-0001 reconark.security.dev-mode must not be enabled in prod");
            }
            http.authorizeHttpRequests(a -> a.anyRequest().permitAll())
                    .addFilterBefore(new DevUserFilter(ALL_ROLES), org.springframework.security.web.authentication.AnonymousAuthenticationFilter.class);
            return http.build();
        }
        http.authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.GET, "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").hasRole("OPERATOR")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(rolesConverter())));
        return http.build();
    }

    static Converter<Jwt, AbstractAuthenticationToken> rolesConverter() {
        return jwt -> {
            Collection<String> roles = jwt.getClaimAsStringList("roles");
            List<GrantedAuthority> authorities = roles == null
                    ? List.of()
                    : roles.stream().<GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList();
            return new JwtAuthenticationToken(jwt, authorities, jwt.getClaimAsString("preferred_username"));
        };
    }
}
