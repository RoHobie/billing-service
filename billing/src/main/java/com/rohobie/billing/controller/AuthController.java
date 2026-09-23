package com.rohobie.billing.controller;

import com.rohobie.billing.domain.Vendor;
import com.rohobie.billing.dto.response.ApiResponse;
import com.rohobie.billing.repository.VendorRepository;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AuthController
 *
 * Exposes authentication verification and self-service vendor registration.
 * Assigns new vendors the default review role (FINANCE) and provisions their profile.
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final InMemoryUserDetailsManager userDetailsManager;
    private final PasswordEncoder passwordEncoder;
    private final VendorRepository vendorRepository;

    public record RegisterRequest(
            @NotBlank String username,
            @NotBlank String password,
            String vendorName,
            String contactEmail
    ) {}

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@RequestBody RegisterRequest request) {
        if (request.username() == null || request.username().isBlank() ||
                request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("Username and password are required");
        }

        if (userDetailsManager.userExists(request.username())) {
            throw new IllegalArgumentException("Username already registered");
        }

        // Assign default role (FINANCE)
        UserDetails vendorUser = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .roles("FINANCE")
                .build();
        userDetailsManager.createUser(vendorUser);

        // Register vendor organization
        String company = request.vendorName() != null && !request.vendorName().isBlank()
                ? request.vendorName() : request.username() + " Transport";
        String email = request.contactEmail() != null && !request.contactEmail().isBlank()
                ? request.contactEmail() : request.username() + "@partner.fleet";
        vendorRepository.save(new Vendor(null, company, email));

        log.info("Registered new vendor user: {} with organization: {} and default role FINANCE", request.username(), company);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(Map.of(
                "username", request.username(),
                "role", "FINANCE",
                "vendorName", company,
                "message", "Account registered successfully"
        )));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_FINANCE")
                .replace("ROLE_", "");

        return ResponseEntity.ok(ApiResponse.of(Map.of(
                "username", authentication.getName(),
                "role", role
        )));
    }
}
