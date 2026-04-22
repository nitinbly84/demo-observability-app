package com.applicationPOC.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.vault.core.VaultKeyValueOperationsSupport;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
	
	@GetMapping("/secret")
	public String getSecret() {
		return secretValue;
	}
	
	@GetMapping("/secrets")
	public Map<String, String> getAllSecrets() {
		return (Map<String, String>)vaultTemplate.read("secret/data/demo-observability-app").getData().get("data"); // Access the 'data' field for KV-V2
	}
	
	@GetMapping("/secretByKey")
	public Object getSecretByKey(@RequestParam String key) {
		return vaultTemplate.opsForKeyValue("secret", VaultKeyValueOperationsSupport.KeyValueBackend.KV_2)
                .get("demo-observability-app")
                .getRequiredData()
                .getOrDefault(key, key+" doesn't exist"); // Throws an exception if data is missing instead of returning null
	}
	
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

}
