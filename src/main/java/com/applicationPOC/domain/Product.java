package com.applicationPOC.domain;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
@NamedQueries({
 @NamedQuery(
	 name = "Product.findByCategory",
	 query = "SELECT p FROM Product p WHERE p.category.name = :categoryName"
 ),
 @NamedQuery(
	 name = "Product.searchByName",
	 query = "SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))"
 ),
 @NamedQuery(
	 name = "Product.findExpensiveInStock",
	 query = "SELECT p FROM Product p WHERE p.price >= :minPrice AND p.inStock = true"
 )
})
public class Product {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 @Column(nullable = false, length = 200)
 private String name;

 @Column(nullable = false, precision = 10, scale = 2)
 private BigDecimal price;

 @Column(name = "in_stock", nullable = false)
 private boolean inStock;

 // Many products belong to one category
 @ManyToOne(fetch = FetchType.LAZY, optional = false)
 @JoinColumn(name = "category_id", nullable = false)
 // To prevent circular references during JSON serialization
 @JsonIgnore
 private Category category;

 public Long getId() {
	return id;
 }

 public void setId(Long id) {
	this.id = id;
 }

 public String getName() {
	return name;
 }

 public void setName(String name) {
	this.name = name;
 }

 public BigDecimal getPrice() {
	return price;
 }

 public void setPrice(BigDecimal price) {
	this.price = price;
 }

 public boolean isInStock() {
	return inStock;
 }

 public void setInStock(boolean inStock) {
	this.inStock = inStock;
 }

 public Category getCategory() {
	return category;
 }

 public void setCategory(Category category) {
	this.category = category;
 }
 
}

