#!/bin/sh
# setup-vault.sh (Ensure LF line endings!)

echo "Cleaning up old Vault keys..."
rm -f ./vault/config/keys.txt

rm -rf ./vault/data/*

# Use the service name defined in docker-compose.yml
export VAULT_ADDR='http://vault:8200'

echo "Waiting for Vault to be responsive..."
until vault status -format=json 2>/dev/null | grep -q '"initialized"'; do
  echo "Vault is starting..."
  sleep 2
done

# 1. Initialize if keys.txt is missing
if [ ! -f /vault/config/keys.txt ]; then
  echo "Initializing Vault..."
  vault operator init -key-shares=1 -key-threshold=1 > /vault/config/keys.txt
fi

# 2. Extract and Unseal
UNSEAL_KEY=$(grep 'Unseal Key 1:' /vault/config/keys.txt | cut -d' ' -f4)
ROOT_TOKEN=$(grep 'Initial Root Token:' /vault/config/keys.txt | cut -d' ' -f4)

echo "Unsealing Vault..."
vault operator unseal $UNSEAL_KEY

echo "Logging in..."
vault login $ROOT_TOKEN

# 3. Provision AppRole
echo "Configuring AppRole and Policies..."
vault auth enable approle || true

echo '
path "secret/data/demo-observability-app" { capabilities = ["create", "update", "read"] }
path "secret/metadata/demo-observability-app" { capabilities = ["list", "read"] }
path "sys/mounts" { capabilities = ["read"] }
path "auth/token/lookup-self" { capabilities = ["read"] }
path "sys/internal/ui/mounts/*" { capabilities = ["read"] }
' | vault policy write demo-observability-app-policy -

vault write auth/approle/role/springboot-role \
    token_policies="demo-observability-app-policy" \
    token_ttl=1h token_max_ttl=4h \
    role_id="demo-app-role-id"

vault write -f auth/approle/role/springboot-role/custom-secret-id \
    secret_id="demo-app-secret-id"
 
# 4. Enable KV-V2 and Seed Data
echo "Enabling KV-V2 engine..."
vault secrets enable -path=secret kv-v2 || true

echo "Storing secrets..."
vault kv put secret/demo-observability-app secret.key="secret value"

echo "DONE. Vault is unsealed and provisioned."
