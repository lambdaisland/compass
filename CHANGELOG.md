# Unreleased

## Added

## Fixed

- Read the cookie session key from config (`:http/session-cookie-key`) instead of source. The old key is in git history, so set a new one on the server.
- Start the JVM with `-XX:-UseBASE64Intrinsics` as an interim mitigation for CVE-2026-114938 (Java4Shell), until the JDK is upgraded.

## Changed
