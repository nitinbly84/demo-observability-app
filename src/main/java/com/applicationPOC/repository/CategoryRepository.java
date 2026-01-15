package com.applicationPOC.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.applicationPOC.domain.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Derived query: find by unique name
    Optional<Category> findByName(String name);
}
