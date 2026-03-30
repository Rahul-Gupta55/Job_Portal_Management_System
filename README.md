# Job Portal Management System

This cleaned version fixes the major security, privacy, and architecture issues that were found in the audit.

## What was fixed
- separated access tokens and refresh tokens in `user-service`
- locked down `GET /api/users/{id}` to self or admin only
- prevented public reads of non-open jobs in `job-service`
- validated resume ownership before creating an application
- hardened resume upload validation with PDF signature checks
- moved `search-service` to its own `search_db` and sync via RabbitMQ events
- removed repo junk from the deliverable (`.git`, `.idea`, `target/`, uploaded resume files)
- added a bundled `github-config-repo/` sample so Config Server works locally without depending on a public repo

## Local run order
1. Start MySQL, RabbitMQ, and Zipkin.
2. Start `eureka-server`.
3. Start `config-server`.
4. Start `user-service`, `job-service`, `application-service`, `resume-service`, `notification-service`, `search-service`.
5. Start `api-gateway`.
6. Open gateway Swagger at `http://localhost:8080/swagger-ui.html`.

## Config server behavior
- Local default config source when running from IDE/Maven: `file:../github-config-repo`
- Docker Compose default config source: mounted local folder at `file:/config-repo`
- Private Git repo override: set `CONFIG_REPO_URI`

## Important
- The bundled config folder is only a safe sample.
- Change `MYSQL_PASSWORD` and `JWT_SECRET` before any real deployment.
- Do not keep secrets in a public GitHub config repo.
