// src/main/java/com/example/demo/controller/UserFormController.java
package com.applicationPOC.controller;

import java.net.URI;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;

import com.applicationPOC.model.UserDto;
import com.applicationPOC.service.DemoService;

@Controller
@RequestMapping("/user")
@SessionAttributes("userForm")
public class UserFormController {
	
	private DemoService demoService;
	
	public UserFormController(DemoService demoService) {
		this.demoService = demoService;
	}

    @ModelAttribute("userForm")
    public UserDto userForm() {
        return new UserDto();
//    	return null;
    }

    @GetMapping("/step1")
    public String showStep1(@ModelAttribute("userForm") UserDto userForm) {
        return "user-step1";
    }

    @PostMapping("/step1")
    public String processStep1(@ModelAttribute("userForm") UserDto userForm, SessionStatus status) {
    	if(userForm.getName() == null || userForm.getName().isBlank()) {
    		status.setComplete(); // clear session attribute
    	}
        return "redirect:/user/step2";
    }

    @GetMapping("/step2")
    public String showStep2(@ModelAttribute("userForm") UserDto userForm, Model model) {
        return "user-step2";
    }

    @PostMapping("/complete")
    public String complete(@ModelAttribute("userForm") UserDto userForm,
                           Model model,
                           SessionStatus status) {
        model.addAttribute("completed", true);
        UserDto saveUser = demoService.saveUser(userForm);
        model.addAttribute("uri", URI.create("/api/public/users/" + saveUser.getId()));
        status.setComplete(); // clear session attribute, if not cleared it will persist across sessions even after completion
        return "user-complete";
    }
}
