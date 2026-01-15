package com.applicationPOC.controller;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.applicationPOC.model.UserDto;
import com.applicationPOC.service.DemoService;

import jakarta.validation.Valid;

// Controller to demonstrate @InitBinder for trimming input strings
//@RestController
@Controller
@ResponseBody // To return response body directly instead of view name
@RequestMapping("/api/binder")
public class BinderController {

	private DemoService demoService;

	public BinderController(DemoService demoService) {
		this.demoService = demoService;
	}

	// Trim all incoming String form fields; convert empty strings to null
	// It is called before any @RequestMapping method
	@InitBinder
	public void initBinder(WebDataBinder binder) {
		System.out.println("InitBinder called - registering StringTrimmerEditor");
		binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
	}

	// @ModelAttribute maps form data to the UserDto object, either use UI to provide form data
	// or use Postman with 'form-data' body type
	// BindingResult to capture validation errors
	@PostMapping("/register")
	public ResponseEntity<?> register(@ModelAttribute @Valid UserDto user, BindingResult result) {
		if (result.hasErrors()) {
			Map<String, List<String>> errorMap = new HashMap<>();
			for (FieldError error : result.getFieldErrors()) {
				errorMap.compute(error.getField(), (k, v) -> {
					if (v == null) {
						v = List.of(error.getDefaultMessage());
					} else {
						v = new java.util.ArrayList<>(v);
						v.add(error.getDefaultMessage());
					}
					return v;
				});
			}
			return ResponseEntity.badRequest().body(errorMap);
		}
		user = demoService.saveUser(user);
		return ResponseEntity.created(URI.create("/api/public/users/" + user.getId()))
				.body(user);
	}
}

