package com.applicationPOC.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.applicationPOC.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

	// Find by username (case-insensitive if your DB supports it)
	@Query("SELECT u FROM User u WHERE u.username = :username")
	Optional<User> findByUsername(@Param("username") String username);
}

