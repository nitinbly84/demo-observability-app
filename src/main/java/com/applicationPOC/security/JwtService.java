package com.applicationPOC.security;

import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	// For demo only; move to config/env var in real apps
	private static final String SECRET = "change-this-secret-to-32-plus-chars-change-this-secret";

	private SecretKey getSigningKey() {
		return Keys.hmacShaKeyFor(SECRET.getBytes());
	}

	public String generateToken(UserDetails userDetails) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(userDetails.getUsername())
				.claim("roles", userDetails.getAuthorities())
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusSeconds(3600))) // 1 hour
				.signWith(getSigningKey())
				.compact();
	}

	public String extractUsername(String token) {
		return Jwts.parser()
				.verifyWith(getSigningKey())
				.build()
				.parseSignedClaims(token)
				.getPayload()
				.getSubject();
	}

	public boolean isTokenValid(String token, UserDetails userDetails) {
		String username = extractUsername(token);
		return username.equals(userDetails.getUsername())
				&& !isExpired(token);
	}

	private boolean isExpired(String token) {
		Date exp = Jwts.parser()
				.verifyWith(getSigningKey())
				.build()
				.parseSignedClaims(token)
				.getPayload()
				.getExpiration();
		return exp.before(new Date());
	}
}

