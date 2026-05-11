package com.applicationPOC.controller;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.vault.core.VaultKeyValueOperationsSupport;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;

@Tag(name = "Vault Controller", description = "Endpoints for managing secrets in Vault")
@RestController
@RequestMapping("/vault")
public class VaultController {
	
	@Value("${secret.key}")
	private String secretValue;
//
//	@Value("${random.secret.key}")
//	private String randomValue;
	
	private final VaultTemplate vaultTemplate;
	
	public VaultController(VaultTemplate vaultTemplate) {
		this.vaultTemplate = vaultTemplate;
	}
	
	@Operation(summary = "Get default secret value", description = "Returns the value of the configured secret key")
	@GetMapping("/secret")
	public String getSecret() {
		return secretValue;
	}
	
	@Operation(summary = "Get all secrets", description = "Returns all key-value pairs stored under the demo-observability-app path in Vault")
	@GetMapping("/secrets")
	public ResponseEntity<Object> getAllSecrets() {
		@Nullable
		VaultResponse vaultResponse = vaultTemplate.read("secret/data/demo-observability-app");
		if(vaultResponse == null || vaultResponse.getData() == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(vaultResponse.getData().get("data")); // Access the 'data' field for KV-V2
	}
	
	@Operation(summary = "Get secret by key", description = "Returns the value of a specific key stored under the demo-observability-app path in Vault")
	@GetMapping("/secretByKey")
	public Object getSecretByKey(@RequestParam String key) {
		return vaultTemplate.opsForKeyValue("secret", VaultKeyValueOperationsSupport.KeyValueBackend.KV_2)
                .get("demo-observability-app")
                .getRequiredData()
                .getOrDefault(key, key+" doesn't exist"); // Throws an exception if data is missing instead of returning null
	}
	
	@Operation(summary = "Write secret", description = "Writes a key-value pair to the demo-observability-app path in Vault. If the key already exists, it updates the value and creates a new version.")
	@SecurityRequirement(name = "Bearer Authentication")
	@PostMapping("/manage/secrets")
    public String writeSecret(@RequestParam String key, @RequestParam String value) {
		// 1. Use the version-aware API to avoid manual /data/ pathing
	    var ops = vaultTemplate.opsForKeyValue("secret", VaultKeyValueOperationsSupport.KeyValueBackend.KV_2);
	    String path = "demo-observability-app";

	    // 2. READ: Get the current version of the secret
	    VaultResponse response = ops.get(path);
	    
	    // 3. MODIFY: Initialize a map with existing data or a new one if empty
	    Map<String, Object> data = (response != null && response.getData() != null) 
	                               ? new HashMap<>(response.getRequiredData()) 
	                               : new HashMap<>();

	    // Add your new key-value pair
	    data.put(key, value);

	    // 4. WRITE: Put the entire map back. Vault creates a new version containing ALL keys.
	    ops.put(path, data);

        return "Secret stored successfully!";
    }
	
	// This method is called after the controller is constructed and dependencies are injected
	// It checks if the critical configuration key is set, and if not, it throws a custom exception to trigger the failure analyzer
	@PostConstruct
	public void validateInjectedKeys() {
		if ("NOT_SET".equals(secretValue)) {
            // This is where the custom exception is thrown
            throw new RuntimeException("secret.key1");
		}
	}

}
