#!/bin/sh
# setup-vault.sh

export VAULT_ADDR='http://vault:8200'
KEYS_FILE="/vault/config/keys.txt"
CLUSTER_ID_FILE="/vault/config/cluster_id.txt"

# 1. Wait for Vault API
until vault status -format=json > /dev/null 2>&1 || [ $? -eq 2 ]; do
  echo "Waiting for Vault API..."
  sleep 2
done

# 2. Capture State (Using robust cut-based parsing)
STATUS_JSON=$(vault status -format=json 2>/dev/null)
CURRENT_INIT=$(echo "$STATUS_JSON" | grep '"initialized"' | cut -d: -f2 | tr -d ' ,')
CURRENT_SEALED=$(echo "$STATUS_JSON" | grep '"sealed"' | cut -d: -f2 | tr -d ' ,')
CURRENT_CLUSTER_ID=$(echo "$STATUS_JSON" | grep '"cluster_id"' | cut -d'"' -f4)

# 3. Handle Container Change (The Handshake)
if [ "$CURRENT_INIT" = "true" ] && [ -n "$CURRENT_CLUSTER_ID" ]; then
    SAVED_ID=$(cat "$CLUSTER_ID_FILE" 2>/dev/null || echo "")
    if [ -n "$SAVED_ID" ] && [ "$SAVED_ID" != "$CURRENT_CLUSTER_ID" ]; then
        echo "DETECTION: New Vault container detected. Wiping stale workspace keys..."
        rm -f "$KEYS_FILE" "$CLUSTER_ID_FILE"
        CURRENT_INIT="false"
    elif [ -z "$SAVED_ID" ]; then
        echo "$CURRENT_CLUSTER_ID" > "$CLUSTER_ID_FILE"
    fi
fi

# 4. Fresh Initialization
if [ "$CURRENT_INIT" != "true" ]; then
    echo "Performing Fresh Vault Initialization..."
    rm -f "$KEYS_FILE" "$CLUSTER_ID_FILE"
    vault operator init -key-shares=1 -key-threshold=1 > "$KEYS_FILE"
    
    # Capture and persist the new ID
    NEW_ID=$(vault status -format=json | grep '"cluster_id"' | cut -d'"' -f4)
    [ -n "$NEW_ID" ] && echo "$NEW_ID" > "$CLUSTER_ID_FILE"
fi

# 5. Unseal (Required for Restart and New)
UNSEAL_KEY=$(grep 'Unseal Key 1:' "$KEYS_FILE" | cut -d' ' -f4)
ROOT_TOKEN=$(grep 'Initial Root Token:' "$KEYS_FILE" | cut -d' ' -f4)

if [ "$CURRENT_SEALED" = "true" ] || [ "$CURRENT_INIT" != "true" ]; then
    echo "Unsealing Vault..."
    vault operator unseal "$UNSEAL_KEY"
    vault login "$ROOT_TOKEN"
fi

# 6. MANDATORY PROVISIONING (Fixes 403 Forbidden)
# We ensure the Policy and AppRole are registered every time the script runs.
echo "Registering AppRoles and Policies..."

# Enable AppRole silently
vault auth list | grep -q 'approle/' || vault auth enable approle

# Policy: Must include sys/mounts and token/lookup-self for Spring Cloud Vault
#echo '
#path "secret/data/demo-observability-app" { capabilities = ["create", "update", "read", "list"] }
#path "secret/metadata/demo-observability-app" { capabilities = ["read", "delete", "list"] }
#path "sys/mounts" { capabilities = ["read"] }
#path "auth/token/lookup-self" { capabilities = ["read"] }
#' | vault policy write demo-observability-app-policy -

# Replace the 'echo' block with this:
echo "Registering Policy from HCL file..."
vault policy write demo-observability-app-policy /vault/demo-observability-app.hcl

# Map the static Role ID and Secret ID
vault write auth/approle/role/springboot-role \
    token_policies="demo-observability-app-policy" \
    token_ttl=1h role_id="demo-app-role-id"

vault write -f auth/approle/role/springboot-role/custom-secret-id \
    secret_id="demo-app-secret-id"

# Enable KV-V2 silently
vault secrets list | grep -q 'secret/' || vault secrets enable -path=secret kv-v2

# Seed initial secret if it doesn't exist
if ! vault kv get secret/demo-observability-app > /dev/null 2>&1; then
    vault kv put secret/demo-observability-app secret.key="default-registration-value"
fi

echo "Vault Synchronized Successfully."