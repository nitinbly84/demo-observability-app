path \"secret/data/demo-observability-app\" { capabilities = [\"read\"] }
# Optional: Permission to see metadata if using versioned secrets
path "secret/metadata/demo-observability-app" {
  capabilities = ["read", "list"]
}