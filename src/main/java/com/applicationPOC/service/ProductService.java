package com.applicationPOC.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.applicationPOC.domain.Category;
import com.applicationPOC.domain.Product;
import com.applicationPOC.repository.CategoryRepository;
import com.applicationPOC.repository.ProductRepository;

@Service
public class ProductService {

 private final ProductRepository productRepository;
 private final CategoryRepository categoryRepository;

 public ProductService(ProductRepository productRepository,
                       CategoryRepository categoryRepository) {
     this.productRepository = productRepository;
     this.categoryRepository = categoryRepository;
 }

 @Transactional(readOnly = true)
 public Page<Product> findByCategoryPaged(String categoryName, int page, int size) {
     Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
     return productRepository.findByCategory_Name(categoryName, pageable);
 }

 @Transactional(readOnly = true)
 public Page<Product> searchProducts(String keyword, int page, int size) {
     Pageable pageable = PageRequest.of(page, size, Sort.by("price").descending());
     return productRepository.findByNameContainingIgnoreCase(keyword, pageable);
 }

 @Transactional(readOnly = true)
 public List<Product> findExpensiveInStock(BigDecimal minPrice) {
     return productRepository.findExpensiveInStock(minPrice);
 }

 @Transactional
 public Product createProduct(String name, BigDecimal price, boolean inStock, String categoryName) {
     Category category = categoryRepository.findByName(categoryName)
             .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryName));

     Product p = new Product();
     p.setName(name);
     p.setPrice(price);
     p.setInStock(inStock);
     p.setCategory(category);

     return productRepository.save(p);
 }

 @Transactional
 public void changeStockFlag(Long productId, boolean inStock) {
     int updated = productRepository.updateInStockFlag(productId, inStock);
     if (updated == 0) {
         throw new IllegalArgumentException("Product not found: " + productId);
     }
 }
 @Transactional(readOnly = true)
 public Product findById(Long id) {
	return productRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
 }
}

