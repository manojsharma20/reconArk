package io.reconark.platform.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Local development only: authenticates as {@code X-Dev-User} (default {@code dev-maker}) so maker-checker can be
 * exercised with two names. Never registered unless dev-mode is on, and dev-mode is refused in prod.
 */
final class DevUserFilter extends OncePerRequestFilter {

    private final List<SimpleGrantedAuthority> authorities;

    DevUserFilter(List<String> roles) {
        this.authorities = roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String user = request.getHeader("X-Dev-User");
        String name = user == null || !user.matches("[a-z0-9-]{1,32}") ? "dev-maker" : user;
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(name, "n/a", authorities));
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
