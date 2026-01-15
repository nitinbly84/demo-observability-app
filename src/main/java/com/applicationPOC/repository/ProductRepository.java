package com.applicationPOC.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.applicationPOC.domain.Category;
import com.applicationPOC.domain.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

 // 1) Derived query: find all by category (ManyToOne side)
 List<Product> findByCategory(Category category);

 // 2) Pagination + filtering: returns a Page
 Page<Product> findByCategory_Name(String categoryName, Pageable pageable);

 // 3) Derived query with "Containing" (LIKE %keyword%)
 Page<Product> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

 // 4) Custom JPQL query
 @Query("select p from Product p where p.inStock = true and p.price > :minPrice")
 List<Product> findExpensiveInStock(BigDecimal minPrice);

 // 5) Custom JPQL with pagination
 @Query("select p from Product p where p.inStock = :inStock")
 Page<Product> findByInStock(boolean inStock, Pageable pageable);

 // 6) Update query example
 @Modifying
 @Query("update Product p set p.inStock = :inStock where p.id = :id")
 int updateInStockFlag(Long id, boolean inStock);
}

