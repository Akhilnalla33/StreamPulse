package com.streampulse.ingestion.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Trusts identity headers (X-Tenant-Id, X-User-Id, X-User-Roles) set by api-gateway after it
 * verified the caller's JWT, and builds a Spring Security authentication from them so
 * {@code @PreAuthorize} works normally downstream. This is safe only because the compose
 * network isolates these services from direct external access — the gateway is the sole
 * entry point (see docs/CONTRACT.md).
 */
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    public static final String TENANT_ID_HEADER = "X-Tenant-Id";
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String ROLES_HEADER = "X-User-Roles";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String userId = request.getHeader(USER_ID_HEADER);
        String rolesHeader = request.getHeader(ROLES_HEADER);

        if (userId != null && rolesHeader != null) {
            List<GrantedAuthority> authorities = Arrays.stream(rolesHeader.split(","))
                    .filter(r -> !r.isBlank())
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r.trim()))
                    .collect(Collectors.toList());
            var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        chain.doFilter(request, response);
    }
}
