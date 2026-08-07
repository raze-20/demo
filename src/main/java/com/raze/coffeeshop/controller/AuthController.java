package com.raze.coffeeshop.controller;

import com.raze.coffeeshop.dto.AuthRequest;
import com.raze.coffeeshop.dto.AuthResponse;
import com.raze.coffeeshop.security.JwtService;
import com.raze.coffeeshop.security.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para autenticación: login con email+password, emisión de JWT.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Value("${app.jwt.expiration-minutes:60}")
    private long expirationMinutes;

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(principal.getId(), principal.getEmail(), principal.getRole().name());

        return new AuthResponse(token, "Bearer", expirationMinutes, principal.getRole());
    }
}
