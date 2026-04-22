package com.applicationPOC.controller;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.applicationPOC.aspects.DemoAspectService;
import com.applicationPOC.config.UserProperties;
import com.applicationPOC.event.UserCreatedEvent;
import com.applicationPOC.model.First;
import com.applicationPOC.model.FirstAutowiredBean;
import com.applicationPOC.model.MultiAutowiredBean;
import com.applicationPOC.model.Scope1;
import com.applicationPOC.model.UserDto;
import com.applicationPOC.service.DemoService;
import com.applicationPOC.service.FeatureService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/public")
//Allows browser clients running at http://localhost:3000 (e.g. React app) to call these endpoints via AJAX.
// else, browser will block due to CORS policy
@CrossOrigin(origins = "http://localhost:3000") // @CrossOrigin demo
public class PublicController {

	// Injecting DemoService by declaring it as final and it will be injected via constructor, no need to use @Autowired on constructor
	private final DemoService demoService;
	private final ApplicationEventPublisher publisher;

	// Using ApplicationContext to fetch the bean for different scopes, else controller is Singleton and any bean injected into it directly,
	// will behave as singleton only. Plus 'request' scope from 'singleton' scoped bean causes issue
	// It is field injection but acceptable here for demo purpose else better to use constructor injection
	@Autowired
	private ApplicationContext context;

	@Autowired
	UserProperties userProperties;

	@Autowired
	private DemoAspectService demoAspectService;

	@Autowired
	private FeatureService featureService;

	@Autowired
	private FirstAutowiredBean first;

	@Autowired
	@Qualifier("Multi")
	private MultiAutowiredBean multi;

	@Autowired
	@Qualifier("ConditionalFirst")
	private First conditionalFirst;

	//	@Autowired
	//	private Scope1 scope1;

	// If we use below number then it will remain same after every start of the server.
	@Value("#{T(java.lang.Math).random() * 100}") // SpEL
	private int randomNumber;

	@Value("${demo.name:Default Name}") // Injecting value from application.properties with default value
	private String name;

	public PublicController(DemoService demoService, ApplicationEventPublisher publisher) {
		this.demoService = demoService;
		this.publisher = publisher;
	}

	@GetMapping("/ping")
	public String ping() {
		return "pong";
	}

	// @PathVariable + cache
	@GetMapping("/cached/{id}")
	public String cached(@PathVariable String id) {
		return demoService.expensiveCall(id);
	}

	// @RequestParam example
	@GetMapping("/search")
	public Map<String, Object> search(@RequestParam(defaultValue = "java") String keyword,
			@RequestParam(defaultValue = "1") int page) {
		return Map.of("keyword", keyword, "page", page);
	}

	// @RequestHeader example
	@GetMapping("/user-agent")
	public Map<String, String> userAgent(@RequestHeader("User-Agent") String userAgent) {
		return Map.of("userAgent", userAgent);
	}

	// @CookieValue + setting cookie
	@GetMapping("/welcome")
	// name in cookie is not required as param is same as cookie name, else name is needed
	public String welcomeUser(@CookieValue(name = "username", required = false) String username,
			HttpServletResponse response) {

		if (username == null) {
			Cookie cookie = new Cookie("username", "demo-user");
			cookie.setPath("/");
			response.addCookie(cookie);
			return "Cookie 'username' set to demo-user";
		}
		return "Welcome, " + username;
	}

	@GetMapping("/default-user")
	public UserDto getDefaultUser() {
		UserDto user = new UserDto();
		user.setName("John Doe");
		user.setEmail("default@email.com");
		user.setPassword("defaultPassword");
		user.setRole("USER");
		return user;
	}		

	// @RequestBody + @ResponseStatus
	@PostMapping("/users")
	@ResponseStatus(HttpStatus.CREATED)
	// Using BindingResult to handle validation errors as such errors are not caught by GlobalExceptionHandler
	// validation errors skip the controller method and go directly to Spring�s internal handler
	// https://medium.com/@AlexanderObregon/showing-form-field-error-messages-in-spring-boot-without-custom-templates-517e3b41fbd7
	public ResponseEntity<?> createUser(@Valid @RequestBody UserDto user, BindingResult result) {
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
		publisher.publishEvent(new UserCreatedEvent(this, user));
		return ResponseEntity.created(URI.create("/api/public/users/" + user.getId()))
				.body(user);
	}

	@GetMapping("/users/{id}")
	public ResponseEntity<?> getUser(@PathVariable Long id) {
		Optional<UserDto> userById = demoService.getUserById(id);
		if(userById.isPresent())
			return ResponseEntity.ok(userById.get());
		else
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User with id " + id + " not found");
	}

	// @ModelAttribute (for query/form binding into object)
	// Below throwing exception to demo GlobalExceptionHandler
	@PostMapping("/users/form")
	public UserDto createUserFromForm(@Valid @ModelAttribute UserDto user) {
		int nextInt = new Random().nextInt(1, 10);
		if(nextInt <= 4)
			throw new IllegalArgumentException("Throwing Random Exception from controller...");
		return demoService.saveUser(user);
	}

	// Async endpoint
	@GetMapping("/async/{input}")
	public CompletableFuture<String> async(@PathVariable String input) {
		return demoService.asyncOperation(input).thenApply(result -> "Processed: " + result);
	}

	// Custom Async endpoint
	@GetMapping("/custom-async/{input}")
	public CompletableFuture<String> customAsync(@PathVariable String input) {
		return demoService.asyncCustomOperation(input).thenApply(result -> "Processed: " + result);
	}

	// @RequestAttribute demo value injected from a filter - UserNameFilter.java
	@GetMapping("/greet")
	public String greet(@RequestAttribute(name = "username", required = false) String username) {
		return "Hello, " + (username != null ? username : "anonymous") + "!";
	}

	@GetMapping("/message")
	public String getMessage(@Value("${demo.message:Are Beans equal}") String message) {
		// Creating another bean of type Scope1 in RandomComponent.java, so have to provide the bean name here to resolve the conflict
		// else can use class name without requiring the type cast
		// Below will return false as scope1 bean is prototype scoped and a new instance is created on every request,
		// so both instances are different
		boolean areEqual = ((Scope1)context.getBean("scope1")) == ((Scope1)context.getBean("scope1"));
		return message + " " + areEqual + " " + ((Scope1)context.getBean("scope1")).getNumber();
	}

	// Its better to use specific method mapping annotations like @GetMapping, @PostMapping etc. instead of @RequestMapping
	@GetMapping("/scope")
	// Default is GET mapping and if method is not specified then it allows all HTTP methods and value is not needed
	//	@RequestMapping("/scope")
	// Use below way to restrict to specific HTTP method or IDE will suggest to use specific method annotation
	//	@RequestMapping(value = "/scope", method=RequestMethod.POST)
	public String getScope() {
		// Using Singleton scoped bean & scope is context specific 
		return ((Scope1) context.getBean("scope1Bean")).getInstanceId();
		//		return scope1.getInstanceId();
	}

	// Aspect demo to modify return value
	@GetMapping("/aspect-value/{name}")
	public String getAspectValue(@PathVariable String name) {
		return demoAspectService.serviceMethod(name);
	}

	// Use it to generate bcrypt hash of a password to store in DB for testing login
	@GetMapping("/hash/{password}")
	public ResponseEntity<?> hashPassword(@PathVariable String password) {
		PasswordEncoder encoder = new BCryptPasswordEncoder();
		String hash = encoder.encode(password);
		return ResponseEntity.ok(hash);
	}

	@GetMapping("first")
	public String getFirst() {
		return first.whoAmI();
	}

	@GetMapping("multi")
	public String getMulti() {
		return multi.whoAmI();
	}

	@GetMapping("name")
	public String getName() {
		return name;
	}

	@GetMapping("conditional-first")
	public String checkConditionalFirst() {
		return conditionalFirst.whoAmI();
	}

	@GetMapping("/user-properties")
	public UserProperties getUserProperties() {
		return userProperties;
	}

	@GetMapping("/feature/{feature}")
	public String checkFeature(@PathVariable String feature) {
		return switch(feature) {
		case "feature1" -> featureService.isFeature1Available();
		case "feature2" -> featureService.isFeature2Available();
		case "feature3" -> featureService.isFeature3Available();
		default -> "Invalid feature name";
		};
	}

}
