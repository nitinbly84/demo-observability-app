package com.applicationPOC.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 @Column(name = "username", nullable = false, unique = true, length = 50)
 private String username;

 @Column(name = "password", nullable = false, length = 255)
 private String password;

 @Column(name = "role", nullable = false, length = 20)
 private String role;

 @Column(name = "enabled", nullable = false)
 private boolean enabled = true;

 @Column(name = "created_at", nullable = false)
 private LocalDateTime createdAt;

 @Column(name = "updated_at", nullable = false)
 private LocalDateTime updatedAt;

 // JPA requires no-arg constructor
 protected User() {}

 public User(String username, String password, String role) {
     this.username = username;
     this.password = password;
     this.role = role;
     this.createdAt = LocalDateTime.now();
     this.updatedAt = LocalDateTime.now();
 }

 // getters and setters
 public Long getId() { return id; }
 public void setId(Long id) { this.id = id; }

 public String getUsername() { return username; }
 public void setUsername(String username) { this.username = username; }

 public String getPassword() { return password; }
 public void setPassword(String password) { this.password = password; }

 public String getRole() { return role; }
 public void setRole(String role) { this.role = role; }

 public boolean isEnabled() { return enabled; }
 public void setEnabled(boolean enabled) { this.enabled = enabled; }

 public LocalDateTime getCreatedAt() { return createdAt; }
 public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

 public LocalDateTime getUpdatedAt() { return updatedAt; }
 public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

