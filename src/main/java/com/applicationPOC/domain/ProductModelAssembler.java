package com.applicationPOC.domain;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.stereotype.Component;

import com.applicationPOC.controller.ProductController;

@Component
public class ProductModelAssembler implements RepresentationModelAssembler<Product, EntityModel<Product>> {

	@Override
	public EntityModel<Product> toModel(Product product) {
		return EntityModel.of(product,
				WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(ProductController.class)
						.one(product.getId()))
				.withSelfRel(),
				WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(ProductController.class)
						.byCategory(product.getCategory().getName(), 0, 5))
				.withRel("category"));
	}
}
