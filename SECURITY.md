# Security Policy

## Supported Versions

Only the latest development version on the active development branch (`main`) is supported for security updates.

| Version | Supported |
| --- | --- |
| Development (`main`) | :white_check_mark: |
| Legacy releases | :x: |

---

## Reporting a Vulnerability

If you discover a security vulnerability or exploit within Settlement Mod, please do not disclose it publicly via GitHub Issues.

Instead, please report the vulnerability privately:
1. Open a **Private Security Advisory** on GitHub under the repository's Security tab, or
2. Contact the maintainers directly via private repository channels.

Please provide:
- A clear description of the vulnerability.
- Steps to reproduce or a minimal proof-of-concept.
- An assessment of potential impact (e.g., remote code execution, server crash/DoS, state corruption).

You should receive an acknowledgment within 48 hours.

---

## Contributor Security Invariants

All contributors must adhere to strict repository security practices:
- **No Secrets or Credentials:** Never commit API keys, bot tokens, access tokens, passwords, or `.env` files.
- **No Machine-Specific Paths or IPs:** Do not commit hardcoded `/home/` paths, absolute developer filesystem paths, or machine-specific private/tailscale IP addresses.
- **Dependency Hygiene:** Never add unvetted third-party dependencies. External mod integrations must be approved by the maintainer and abstracted behind clean port interfaces.
