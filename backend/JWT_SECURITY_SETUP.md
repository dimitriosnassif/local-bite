# 🔐 JWT Security Configuration Guide

## ⚠️ CRITICAL: JWT Secret Setup

The LocalBite authentication system requires a secure JWT secret to be configured via environment variables. **Never use hardcoded secrets in production!**

## Quick Setup

### 1. Generate a Secure Secret
```bash
# Generate a 512-bit Base64-encoded secret
openssl rand -base64 64
```

### 2. Set Environment Variable

#### For Development (Local)
```bash
# Linux/macOS
export JWT_SECRET="your-generated-base64-secret-here"

# Windows
set JWT_SECRET=your-generated-base64-secret-here
```

#### For Production Deployment
Set the environment variable in your deployment platform:

**Docker:**
```bash
docker run -e JWT_SECRET="your-secret" localbite-backend
```

**Docker Compose:**
```yaml
environment:
  - JWT_SECRET=your-secret
```

**Kubernetes:**
```yaml
env:
  - name: JWT_SECRET
    valueFrom:
      secretKeyRef:
        name: jwt-secret
        key: secret
```

## Full Environment Configuration

```bash
# JWT Security (REQUIRED)
JWT_SECRET=your-base64-encoded-secret-here
JWT_EXPIRATION=3600000          # 1 hour (milliseconds)
JWT_REFRESH_EXPIRATION=604800000 # 7 days (milliseconds)
JWT_ISSUER=LocalBite
JWT_AUDIENCE=LocalBite-Users
```

## Security Validation

The application will:
- ✅ Validate that JWT_SECRET is set and not empty
- ✅ Verify the secret is properly Base64-encoded
- ✅ Ensure the secret is at least 256 bits (32 bytes) long
- ❌ Refuse to start if validation fails

## What Changed

### Before (INSECURE):
```java
private String secret = "LocalBiteSecretKeyThatShouldBeAtLeast256BitsLongForSecurityPurposes";
```

### After (SECURE):
```java
private String secret; // Loaded from JWT_SECRET environment variable
```

## Error Messages

If you see these errors, check your JWT configuration:

```
🚨 CRITICAL SECURITY ERROR: JWT secret is not configured!
Set JWT_SECRET environment variable with a secure Base64-encoded secret
Generate one with: openssl rand -base64 64
```

```
🚨 SECURITY ERROR: JWT secret is not valid Base64
Generate a proper secret with: openssl rand -base64 64
```

```
🚨 SECURITY WARNING: JWT secret is too short (< 256 bits)
Generate a longer secret with: openssl rand -base64 64
```

## Best Practices

1. **Never commit secrets to version control**
2. **Use different secrets for different environments**
3. **Rotate secrets regularly**
4. **Store secrets securely (e.g., HashiCorp Vault, AWS Secrets Manager)**
5. **Monitor for secret exposure in logs**

## Testing the Fix

Start the application and look for:
```
✅ JWT configuration validated successfully
```

If you see this message, your JWT security is properly configured! 