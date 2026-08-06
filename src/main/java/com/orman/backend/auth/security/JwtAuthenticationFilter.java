package com.orman.backend.auth.security;

import com.orman.backend.auth.exception.InvalidJwtException;
import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.auth.service.JwtService;
import com.orman.backend.auth.service.SessionService;
import com.orman.backend.auth.service.UserAuthorityService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final SessionService sessionService;
    private final UserAuthorityService userAuthorityService;
    private final HandlerExceptionResolver exceptionResolver;

    public JwtAuthenticationFilter(JwtService jwtService, SessionService sessionService,
            UserAuthorityService userAuthorityService,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        this.jwtService = jwtService;
        this.sessionService = sessionService;
        this.userAuthorityService = userAuthorityService;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (!authorization.startsWith(BEARER_PREFIX)
                    || authorization.length() == BEARER_PREFIX.length()
                    || !authorization.substring(BEARER_PREFIX.length()).equals(
                            authorization.substring(BEARER_PREFIX.length()).trim())) {
                throw new InvalidJwtException();
            }
            String token = authorization.substring(BEARER_PREFIX.length());
            JwtService.JwtClaims claims = jwtService.validateAndExtract(token);
            AuthenticatedUser principal = sessionService.authenticate(claims.subject(), claims.sid());
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null,
                            userAuthorityService.loadAuthorities(principal.login())));
            filterChain.doFilter(request, response);
        } catch (RuntimeException exception) {
            SecurityContextHolder.clearContext();
            exceptionResolver.resolveException(request, response, null, exception);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return ("POST".equals(request.getMethod())
                && ("/api/v1/auth/login".equals(path) || "/api/v1/auth/refresh".equals(path)))
                || "OPTIONS".equals(request.getMethod());
    }
}
