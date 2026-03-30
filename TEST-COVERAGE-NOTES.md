# Test Coverage Notes

What was added:
- New controller-layer tests across user, job, application, resume, notification, and search services
- New global exception handler tests across services
- New config-bean tests for OpenAPI, RabbitMQ, CORS, and security helper beans where safe
- New common-security tests for `JwtAuthenticationFilter`, `JwtProperties`, `JwtUserPrincipal`, and bean wiring
- New application bootstrap tests for gateway, config server, eureka server, and each microservice main class
- JaCoCo plugin added to modules that were missing coverage report generation: `common-security`, `api-gateway`, `config-server`, `eureka-server`

Recommended verification locally:
1. Build/install shared module first:
   - `cd common-security && mvn clean install`
2. Run tests module by module:
   - `mvn clean test`
3. Generate JaCoCo reports:
   - `mvn clean verify`
4. Run Sonar scan from each module or your preferred pipeline with JaCoCo XML report pickup.

Important note:
- Maven is not available in the execution container used for this edit session, so the full test suite could not be executed here. The project files were updated carefully to maximize real coverage without intentionally gaming Sonar.
