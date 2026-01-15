package com.applicationPOC.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
public class Category {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 @Column(nullable = false, unique = true, length = 100)
 private String name;

 // One category has many products; mappedBy points to "category" field in Product.
 @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
 private List<Product> products = new ArrayList<>();

 // helpers for bidirectional relationship
 public void addProduct(Product product) {
     products.add(product);
     product.setCategory(this);
 }

 public void removeProduct(Product product) {
     products.remove(product);
     product.setCategory(null);
 }

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

 public List<Product> getProducts() {
	return products;
 }

 public void setProducts(List<Product> products) {
	this.products = products;
 }
 
}
