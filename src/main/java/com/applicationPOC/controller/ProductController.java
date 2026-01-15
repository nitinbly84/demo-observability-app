package com.applicationPOC.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.applicationPOC.domain.Product;
import com.applicationPOC.domain.ProductModelAssembler;
import com.applicationPOC.service.ProductService;

@RestController
@RequestMapping("/api/products")
public class ProductController {

	private final ProductService service;
    private final ProductModelAssembler assembler;
    private final PagedResourcesAssembler<Product> pagedAssembler;

    public ProductController(ProductService service,
                             ProductModelAssembler assembler,
                             PagedResourcesAssembler<Product> pagedAssembler) {
        this.service = service;
        this.assembler = assembler;
        this.pagedAssembler = pagedAssembler;
    }
    
    @GetMapping("/{id}")
    public EntityModel<Product> one(@PathVariable Long id) {
        Product product = service.findById(id);
        return assembler.toModel(product);
    }

    @GetMapping("/by-category")
    public PagedModel<EntityModel<Product>> byCategory(
            @RequestParam String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {

        Page<Product> products = service.findByCategoryPaged(category, page, size);
        // Convert Page<Product> -> PagedModel<EntityModel<Product>> using assembler
        return pagedAssembler.toModel(products, assembler);
    }

 @GetMapping("/search")
 public Page<Product> search(@RequestParam String q,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "5") int size) {
     return service.searchProducts(q, page, size);
 }

 @GetMapping("/expensive")
 public List<Product> expensive(@RequestParam(defaultValue = "100") BigDecimal minPrice) {
     return service.findExpensiveInStock(minPrice);
 }

 @PostMapping
 public Product create(@RequestParam String name,
                       @RequestParam BigDecimal price,
                       @RequestParam(defaultValue = "true") boolean inStock,
                       @RequestParam String category) {
     return service.createProduct(name, price, inStock, category);
 }

 @PatchMapping("/{id}/stock")
 public ResponseEntity<Void> changeStock(@PathVariable Long id,
                                         @RequestParam boolean inStock) {
     service.changeStockFlag(id, inStock);
     return ResponseEntity.noContent().build();
 }
}

