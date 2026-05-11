# Grant full CRUD permissions on the secret data path
path "secret/data/demo-observability-app" {
  capabilities = ["create", "update", "read", "list"]
}

# Grant metadata access for versioning and listing
path "secret/metadata/demo-observability-app" {
  capabilities = ["read", "delete", "list"]
}

# Required for Spring Cloud Vault bootstrap and health checks
path "sys/mounts" {
  capabilities = ["read"]
}

path "auth/token/lookup-self" {
  capabilities = ["read"]
}