# diq-platform-libs

Shared Maven build standards and Spring Boot platform starters for DisputeIQ.

## Modules

- `diq-build-config`: Packages `diq/checkstyle.xml`.
- `diq-parent`: Shared parent POM (Java 21, Spring Boot 3.4 BOM, Spotless, Checkstyle, Surefire, JaCoCo).
- `diq-common-starter`: `TenantId`, `TenantContext` (`ScopedValue`), `TenantAware`, and `CorrelationHeaders`.

## Build & Install

```powershell
.\mvnw.cmd clean install
```
