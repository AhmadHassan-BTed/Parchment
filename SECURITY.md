# Security Policy

## Supported Versions

| Version | Supported          |
|---------|--------------------|
| 1.0.x   | ✅ Active support  |
| < 1.0   | ❌ Not supported   |

## Reporting a Vulnerability

**Please do NOT report security vulnerabilities through public GitHub issues.**

If you discover a security vulnerability, please report it responsibly:

1. **Email:** Send details to **parchment-security@example.com**
2. **Include:**
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (if any)

### What to expect

- **Acknowledgment** within 48 hours
- **Assessment** within 1 week
- **Fix timeline** communicated after assessment
- **Credit** in the security advisory (unless you prefer anonymity)

## Security Practices

### What we protect
- No user data is transmitted off-device
- All reading preferences are stored locally via SharedPreferences
- No network requests are made by the app
- No analytics or tracking is included

### Repository security
- No secrets, API keys, or credentials are committed
- `.env` files are gitignored
- Keystore files are never tracked in version control
- CI/CD secrets are managed via GitHub Encrypted Secrets
- Dependencies are monitored via Dependabot

### Build security
- Release builds use ProGuard/R8 for code shrinking and obfuscation
- APK signing keys are stored securely outside the repository
- Release artifacts are distributed through GitHub Releases

## Dependencies

We monitor dependencies for known vulnerabilities using GitHub's Dependabot alerts.
