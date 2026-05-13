package org.shieldwork.chatmmbackend.security;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock private JwtService jwtService;
    @Mock private UserDetailsService userDetailsService;
    @Mock private HandlerExceptionResolver exceptionResolver;

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_ShouldProceedWithoutAuth_WhenHeaderIsMissing() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication(), "Authentication should be null when there is no Authorization header");
        verifyNoInteractions(jwtService, userDetailsService, exceptionResolver);
    }

    @Test
    void doFilterInternal_ShouldProceedWithoutAuth_WhenHeaderDoesNotStartWithBearer() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Basic invalid-token-format");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication(), "Authentication should be null when Authorization header does not start with 'Bearer '");
        verifyNoInteractions(jwtService, userDetailsService, exceptionResolver);
    }

    @Test
    void doFilterInternal_ShouldAuthenticateUser_WhenTokenIsValid() throws ServletException, IOException {
        String token = "valid-jwt-token";
        String userEmail = "test@example.com";
        UserDetails mockUserDetails = mock(UserDetails.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractUsername(token)).thenReturn(userEmail);
        when(userDetailsService.loadUserByUsername(userEmail)).thenReturn(mockUserDetails);
        when(jwtService.isTokenValid(token, mockUserDetails)).thenReturn(true);
        when(mockUserDetails.getAuthorities()).thenReturn(null);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth, "Authentication should be set in SecurityContext");
        assertEquals(mockUserDetails, auth.getPrincipal(), "Authenticated principal should match the loaded user");
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldNotAuthenticate_WhenTokenIsInvalid() throws ServletException, IOException {
        String token = "invalid-jwt-token";
        String userEmail = "test@example.com";
        UserDetails mockUserDetails = mock(UserDetails.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractUsername(token)).thenReturn(userEmail);
        when(userDetailsService.loadUserByUsername(userEmail)).thenReturn(mockUserDetails);

        when(jwtService.isTokenValid(token, mockUserDetails)).thenReturn(false);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(), "Authentication should NOT be set when token is invalid");
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldPassExceptionToResolver_WhenTokenIsExpired() throws ServletException, IOException {
        String token = "expired-jwt-token";
        ExpiredJwtException expiredException = mock(ExpiredJwtException.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractUsername(token)).thenThrow(expiredException);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(exceptionResolver, times(1)).resolveException(request, response, null, expiredException);
        assertNull(SecurityContextHolder.getContext().getAuthentication(), "Authentication should be null when token is expired");
    }

    @Test
    void doFilterInternal_ShouldPassExceptionToResolver_WhenGenericExceptionOccurs() throws ServletException, IOException {
        String token = "malformed-jwt-token";
        RuntimeException genericException = new RuntimeException("Something went wrong");

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractUsername(token)).thenThrow(genericException);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(exceptionResolver, times(1)).resolveException(request, response, null, genericException);
        assertNull(SecurityContextHolder.getContext().getAuthentication(), "Authentication should be null when an error occurs");
    }
}