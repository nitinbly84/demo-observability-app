package com.applicationPOC.controller;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/secure")
public class SecureController {

    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        return Map.of(
                "user", auth.getName(),
                "authorities", auth.getAuthorities()
        );
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public String secureAdminEndpoint() {
		return "This is a secure Admin endpoint!";
	}
    
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/user")
    public String secureUserEndpoint() {
    			return "This is a secure User endpoint!";
    }
    
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/common")
    public String commonsecureEndpoint() {
    			return "This is a secure endpoint for all authenticated users!";
    }
    
    @GetMapping("/logout-force")
    public ResponseEntity<String> logoutAndForceReauth(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"demo\"")
                .body("Logged out");
    }
    
    @GetMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
    	SecurityContextHolder.getContext().setAuthentication(null);
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		return ResponseEntity.ok("Logged out successfully");
    }
}
