# Add these to your vault-config.hcl
api_addr     = "http://127.0.0.1:8200"
cluster_addr = "http://127.0.0.1:8201"

# Enable the Web UI
ui = true

# Use the local file system for persistent storage
storage "file" {
  path = "/vault/file"
}

# Define how Vault listens for requests
listener "tcp" {
  address     = "0.0.0.0:8200"
  tls_disable = 1  # In a real cloud env, you would point to your SSL certs here
}

# Prevent memory from being swapped to disk (Security best practice)
disable_mlock = false