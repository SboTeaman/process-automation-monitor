# Securing Kubernetes Secrets with SealedSecrets

By default, Kubernetes Secrets are stored in etcd in Base64 (not encrypted). Use **SealedSecrets** to encrypt secrets at rest.

## Install SealedSecrets

```bash
kubectl apply -f https://github.com/bitnami-labs/sealed-secrets/releases/download/v0.24.0/controller.yaml
```

## Create a SealedSecret

1. Create a plain Kubernetes Secret YAML:
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: process-monitor-secrets
type: Opaque
stringData:
  postgres-password: "your-strong-postgres-password"
  postgres-user: "orchestrator"
  mysql-password: "your-mysql-password"
  mysql-root-password: "your-mysql-root-password"
  mysql-analytics-password: "your-analytics-password"
  mysql-analytics-root-password: "your-analytics-root-password"
  jwt-secret: "your-min-32-char-jwt-secret"
  worker-api-key: "your-worker-api-key"
  analytics-api-key: "your-analytics-api-key"
```

2. Seal it:
```bash
kubeseal -f secret.yaml -w sealed-secret.yaml --scope cluster-wide
```

3. Deploy the sealed secret:
```bash
kubectl apply -f sealed-secret.yaml
```

The SealedSecret can now be safely committed to git. Only the SealedSecrets controller can decrypt it.

## Alternative: Use External Secrets + AWS Secrets Manager

For production, store secrets in AWS Secrets Manager, Azure KeyVault, or HashiCorp Vault:

```yaml
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: aws-secrets
spec:
  provider:
    aws:
      service: SecretsManager
      region: us-east-1
      auth:
        jwt:
          serviceAccountRef:
            name: external-secrets-sa

---
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: process-monitor-secrets
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: aws-secrets
    kind: SecretStore
  target:
    name: process-monitor-secrets
    creationPolicy: Owner
  data:
    - secretKey: postgres-password
      remoteRef:
        key: /processmonitor/db/postgres-password
```

Install External Secrets Operator:
```bash
helm repo add external-secrets https://charts.external-secrets.io
helm install external-secrets external-secrets/external-secrets -n external-secrets-system --create-namespace
```

## Principle of Least Privilege

Create separate database users for each service:

```sql
-- PostgreSQL
CREATE USER orchestrator WITH PASSWORD 'strong-password' LOGIN NOSUPERUSER;
CREATE USER analytics WITH PASSWORD 'strong-password' LOGIN NOSUPERUSER;

-- Grant only needed permissions
GRANT CONNECT ON DATABASE orchestrator_db TO orchestrator;
GRANT USAGE ON SCHEMA public TO orchestrator;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO orchestrator;

-- Analytics: read-only access
GRANT CONNECT ON DATABASE orchestrator_db TO analytics;
GRANT USAGE ON SCHEMA public TO analytics;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO analytics;
```

```sql
-- MySQL
CREATE USER 'orchestrator'@'%' IDENTIFIED BY 'strong-password';
CREATE USER 'analytics'@'%' IDENTIFIED BY 'strong-password';

GRANT ALL PRIVILEGES ON orchestrator_db.* TO 'orchestrator'@'%';
GRANT SELECT ON orchestrator_db.* TO 'analytics'@'%';

FLUSH PRIVILEGES;
```

## Enable etcd Encryption at Rest (optional, cluster-wide)

```yaml
# /etc/kubernetes/encryption.yaml
apiVersion: apiserver.config.k8s.io/v1
kind: EncryptionConfiguration
resources:
  - resources:
      - secrets
    providers:
      - aescbc:
          keys:
            - name: key1
              secret: <32-byte-base64-encoded-key>
      - identity: {}
```

Then update kube-apiserver with:
```
--encryption-provider-config=/etc/kubernetes/encryption.yaml
```

This encrypts ALL secrets in etcd, not just SealedSecrets.
