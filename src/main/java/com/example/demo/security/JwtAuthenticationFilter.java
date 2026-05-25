package com.example.demo.security;

import com.example.demo.entity.UserSession;
import com.example.demo.repository.UserSessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final UserSessionRepository userSessionRepository;

    public JwtAuthenticationFilter(
        JwtUtil jwtUtil,
        CustomUserDetailsService userDetailsService,
        UserSessionRepository userSessionRepository
    ) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.userSessionRepository = userSessionRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(token);
            } catch (RuntimeException ignored) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (jwtUtil.validateToken(token, userDetails)) {
                Long sessionId = jwtUtil.extractSessionId(token);
                if (sessionId != null) {
                    Optional<UserSession> userSessionOpt = userSessionRepository.findById(sessionId);
                    if (userSessionOpt.isPresent()) {
                        UserSession userSession = userSessionOpt.get();
                        if (!userSession.isRevoked()) {
                            String tokenDeviceId = jwtUtil.extractDeviceId(token);
                            String sessionDeviceId = userSession.getDeviceId();
                            // Enforce device binding when a session has a deviceId; allow legacy sessions without it.
                            boolean deviceIdValid = true;
                            if (sessionDeviceId != null) {
                                deviceIdValid = sessionDeviceId.equals(tokenDeviceId);
                            }
                            if (deviceIdValid) {
                                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                                );
                                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                                SecurityContextHolder.getContext().setAuthentication(authentication);
                            }
                        }
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
