# SahaTakip Security Policy

SahaTakip is committed to ensuring the security and privacy of field personnel data. This document outlines our security features, supported versions, and the process for reporting vulnerabilities.

## Security Features

SahaTakip incorporates several layers of security to protect sensitive information:

*   **Root Detection:** The application actively checks for root access (SU binaries, Magisk, test-keys, etc.) to prevent operation on compromised devices.
*   **Secure Storage:** Sensitive data and local identifiers are encrypted using the **Android Keystore System** with `AES/GCM/NoPadding` transformation.
*   **Biometric Security:** Integration with Android's Biometric API allows for secure, hardware-backed user authentication.
*   **Real-time Monitoring:** Geofence violations and system-level security events (GPS/Internet status changes) are logged and alerted via a dedicated security notification channel.

## Supported Versions

We provide security updates for the following versions of SahaTakip:

| Version | Supported          |
| ------- | ------------------ |
| 1.x     | :white_check_mark: |
| < 1.0   | :x:                |

## Reporting a Vulnerability

We appreciate the work of security researchers in identifying vulnerabilities. If you discover a security issue, please report it responsibly:

1.  **Do not** disclose the vulnerability publicly until it has been addressed.
2.  Send a detailed report to **security@sahatakip.com** (placeholder - please update with your actual contact).
3.  Include a description of the vulnerability, steps to reproduce, and potential impact.

### Response Timeline

*   **Acknowledgement:** Within 48 hours of receipt.
*   **Initial Assessment:** Within 5 business days.
*   **Status Updates:** Every 2 weeks until the issue is resolved.

## Security Alerts

Critical security alerts are delivered via the "Field Security and Geofence Alerts" notification channel within the app. Users are encouraged to keep this channel enabled.
