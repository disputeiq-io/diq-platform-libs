# diq-platform-libs

Shared Gradle conventions and platform starters (`diq-common-starter`) for DisputeIQ Java microservices.

- Follow `.agents/skills/disputeiq-engineering/SKILL.md`.
- Build, verify, and publish locally with `.\gradlew.bat check publishToMavenLocal`.
- Never use `ThreadLocal`; use `TenantContext` (`ScopedValue`) and `TenantAware.wrap`.
