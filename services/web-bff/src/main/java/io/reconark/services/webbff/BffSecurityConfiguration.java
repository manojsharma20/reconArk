package io.reconark.services.webbff;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * OAuth 2.0 for browser-based apps, token-handler pattern (ADR-0031): the browser gets an HttpOnly session cookie and a
 * CSRF cookie; access and refresh tokens never reach JavaScript.
 */
@Configuration(proxyBeanMethods = false)
class BffSecurityConfiguration {

    static final List<String> DEV_ROLES = List.of("ONBOARDING_MAKER", "ONBOARDING_CHECKER", "RECON_ANALYST", "RECON_SUPERVISOR",
            "REPORT_VIEWER", "REPORT_EXPORTER", "OPERATOR", "AUDITOR");

    @Bean
    SecurityFilterChain bffSecurity(HttpSecurity http, BffProperties props) throws Exception {
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName(null); // SPA reads the token from the XSRF-TOKEN cookie
        http.csrf(c -> c.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()).csrfTokenRequestHandler(csrfHandler))
                .headers(h -> h
                        .contentSecurityPolicy(c -> c.policyDirectives(
                                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; "
                                        + "connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'"))
                        .referrerPolicy(r -> r.policy(
                                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)));
        if (props.devMode()) {
            http.authorizeHttpRequests(a -> a.anyRequest().permitAll())
                    .addFilterBefore(devUser(), AnonymousAuthenticationFilter.class);
            return http.build();
        }
        http.authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.GET, "/actuator/health/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(Customizer.withDefaults())
                .logout(l -> l.logoutUrl("/bff/logout").logoutSuccessUrl("/"));
        return http.build();
    }

    private static OncePerRequestFilter devUser() {
        List<SimpleGrantedAuthority> authorities = DEV_ROLES.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList();
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                    jakarta.servlet.http.HttpServletRequest request,
                    jakarta.servlet.http.HttpServletResponse response,
                    jakarta.servlet.FilterChain chain)
                    throws java.io.IOException, jakarta.servlet.ServletException {
                SecurityContextHolder.getContext().setAuthentication(
                        UsernamePasswordAuthenticationToken.authenticated("dev-maker", "n/a", authorities));
                try {
                    chain.doFilter(request, response);
                } finally {
                    SecurityContextHolder.clearContext();
                }
            }
        };
    }
}
