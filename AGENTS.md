# diq-platform-libs

Shared Maven parent POM (`diq-parent`), Checkstyle bundle (`diq-build-config`), and
platform starters (`diq-common-starter`) for DisputeIQ Java microservices.

- Follow `.agents/skills/disputeiq-engineering/SKILL.md`.
- Build and install locally with `.\mvnw.cmd clean install`.
- Never use `ThreadLocal`; use `TenantContext` (`ScopedValue`) and `TenantAware.wrap`.
