# Job Portal Management System

A production-style microservices-based job portal backend built with **Java 21**, **Spring Boot 3.5.11**, **Spring Cloud 2025.0.1**, **JWT security**, **RabbitMQ**, **MySQL**, **Spring Cloud Config**, **Eureka**, **Zipkin**, and **Docker**.

This project supports recruiter and job seeker workflows such as user registration, authentication, job posting, job search, resume upload, job application tracking, password reset with OTP, and real email notifications.

---

## Highlights

- Microservices architecture with independent services
- JWT-based authentication and authorization
- Centralized configuration using Spring Cloud Config
- Service discovery with Eureka Server
- API Gateway as the single entry point
- Distributed tracing with Zipkin
- RabbitMQ-based event-driven communication
- Resume upload support
- Forgot password flow with OTP over email
- Real email notifications for recruiter and job seeker events
- Professional **plain-text** email output without raw internal IDs in the message body
- Swagger/OpenAPI support for API testing
- Docker support for containerized setup

---

## Services

| Service | Purpose |
|---|---|
| `api-gateway` | Single entry point for client requests |
| `eureka-server` | Service registry and discovery |
| `config-server` | Centralized configuration from external config repo |
| `common-security` | Shared JWT and security utilities |
| `user-service` | Registration, login, refresh token, user management, forgot password OTP |
| `job-service` | Job creation, update, listing, closing |
| `application-service` | Job application submission and status management |
| `resume-service` | Resume upload and resume metadata |
| `notification-service` | Real email sending and notification persistence |
| `search-service` | Search-related read operations |

---

## Main Features

### Authentication and Authorization
- User registration and login
- JWT access token support
- Refresh token support
- Role-based access control for recruiter, job seeker, and admin flows

### Job Management
- Recruiters can create, update, and close jobs
- Public and protected job listing flows
- Search integration for job discovery

### Applications
- Job seekers can apply to jobs
- Recruiters can review and update application status
- Status-change events trigger notifications

### Resume Management
- Resume upload support
- Resume ownership validation before applying to a job

### Forgot Password with OTP
- User requests OTP using email
- OTP is sent to the registered email address
- Password reset is completed only after OTP verification

### Notification System
- Event-driven notifications using RabbitMQ
- Real plain-text emails for important actions such as:
    - job application submitted
    - application status changed
    - job created / job closed notifications where applicable
    - forgot password OTP

---

## Architecture Overview

Client requests enter through **API Gateway**. The gateway routes requests to internal services registered in **Eureka**. Shared configuration is loaded from **Config Server**, which reads from the external config repository. Business events are published through **RabbitMQ**, and **notification-service** reacts to those events to send emails and store notification records. **Zipkin** is used for distributed tracing across services.

---

## Tech Stack

- Java 21
- Spring Boot 3.5.11
- Spring Cloud 2025.0.1
- Spring Security
- Spring Data JPA
- Spring Cloud Gateway
- Spring Cloud Config
- Netflix Eureka
- OpenFeign
- RabbitMQ
- MySQL 8
- Zipkin
- Swagger / Springdoc OpenAPI
- Docker / Docker Compose
- JUnit 5 / Mockito / MockMvc

---

## Project Structure

```text
api-gateway/
application-service/
common-security/
config-server/
eureka-server/
job-service/
mysql/
notification-service/
resume-service/
search-service/
user-service/
docker-compose.yml
.env
```

---

## Configuration

This project uses **Spring Cloud Config**.

The Config Server reads from the external config repository:

- `https://github.com/Madhav-kolasani/jobportal-config.git`

You should keep service configuration there, but **never commit real secrets** such as your Gmail app password.

### Important Config Files
- `application.yml`
- `user-service.yml`
- `notification-service.yml`
- other service-specific config files as needed

### Secret Handling
Use placeholders in the config repo, for example:

```yml
spring:
  mail:
    password: ${MAIL_PASSWORD:}
```

Pass the real secret using:
- IntelliJ environment variables for local IDE runs
- `.env` for Docker Compose runs

---

## Email Setup

This project uses **Gmail SMTP** for email delivery.

### Required
- Enable **2-Step Verification** on your Google account
- Create a **Gmail App Password**
- Use that app password as `MAIL_PASSWORD`

### `.env` example
Create a `.env` file in the project root:

```env
MAIL_PASSWORD=your_16_digit_gmail_app_password
```

### IntelliJ local environment variables
For `notification-service`, set:

```text
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=yagnamadhavkolasani2004@gmail.com
MAIL_PASSWORD=your_16_digit_gmail_app_password
MAIL_FROM=yagnamadhavkolasani2004@gmail.com
APP_INTERNAL_API_KEY=jobportal-internal-key
```

For `user-service`, set:

```text
APP_INTERNAL_API_KEY=jobportal-internal-key
OTP_EXPIRY_MINUTES=10
```

`APP_INTERNAL_API_KEY` must match in both `user-service` and `notification-service`.

---

## Running the Project Locally

### Prerequisites
- JDK 21
- Maven 3.9+
- Docker Desktop
- MySQL, RabbitMQ, and Zipkin available either through Docker or locally

### Start infrastructure first
You need:
- MySQL
- RabbitMQ
- Zipkin

Then start services in this order:
1. `eureka-server`
2. `config-server`
3. `user-service`
4. `job-service`
5. `application-service`
6. `resume-service`
7. `notification-service`
8. `search-service`
9. `api-gateway`

### Build order
Build shared module first:

```bash
mvn -f common-security/pom.xml clean install
```

Then build each service:

```bash
mvn -f eureka-server/pom.xml clean package
mvn -f config-server/pom.xml clean package
mvn -f user-service/pom.xml clean package
mvn -f job-service/pom.xml clean package
mvn -f application-service/pom.xml clean package
mvn -f resume-service/pom.xml clean package
mvn -f notification-service/pom.xml clean package
mvn -f search-service/pom.xml clean package
mvn -f api-gateway/pom.xml clean package
```

### Swagger
After starting the system, open:

- `http://localhost:8080/swagger-ui.html`

---

## Running with Docker

### Important
This repository’s `docker-compose.yml` uses **image names**, not service-level `build:` blocks.

That means you must build the Docker images first before running `docker compose up`.

### Build Docker images
From the project root:

```bash
mvn -f common-security/pom.xml clean install -DskipTests

mvn -f eureka-server/pom.xml clean package -DskipTests
mvn -f config-server/pom.xml clean package -DskipTests
mvn -f user-service/pom.xml clean package -DskipTests
mvn -f job-service/pom.xml clean package -DskipTests
mvn -f application-service/pom.xml clean package -DskipTests
mvn -f resume-service/pom.xml clean package -DskipTests
mvn -f notification-service/pom.xml clean package -DskipTests
mvn -f search-service/pom.xml clean package -DskipTests
mvn -f api-gateway/pom.xml clean package -DskipTests
```

Build images with the tags used by Docker Compose:

```bash
docker build -t madhavkolasani/jobportal-eureka-server:latest ./eureka-server
docker build -t madhavkolasani/jobportal-config-server:latest ./config-server
docker build -t madhavkolasani/jobportal-user-service:latest ./user-service
docker build -t madhavkolasani/jobportal-job-service:latest ./job-service
docker build -t madhavkolasani/jobportal-application-service:latest ./application-service
docker build -t madhavkolasani/jobportal-resume-service:latest ./resume-service
docker build -t madhavkolasani/jobportal-notification-service:latest ./notification-service
docker build -t madhavkolasani/jobportal-search-service:latest ./search-service
docker build -t madhavkolasani/jobportal-api-gateway:latest ./api-gateway
```

Then start the stack:

```bash
docker compose up -d
```

To stop it:

```bash
docker compose down
```

Do **not** use `docker compose down -v` unless you intentionally want to remove your database volume.

---

## Important API Flows

### Register
- `POST /api/users/register`

### Login
- `POST /api/users/login`

### Refresh Token
- `POST /api/users/refresh-token`

### Forgot Password - Request OTP
- `POST /api/users/forgot-password/request-otp`

Example request:

```json
{
  "email": "user@example.com"
}
```

### Forgot Password - Reset Password
- `POST /api/users/forgot-password/reset`

Example request:

```json
{
  "email": "user@example.com",
  "otp": "123456",
  "newPassword": "NewPass@123"
}
```

---

## Notes About the Email Implementation

- OTP emails are sent through the internal notification API
- Event-driven notifications are triggered through RabbitMQ
- Notification emails are now professional **plain-text** emails
- Raw internal IDs are intentionally not exposed in the message body
- Notification records should reflect actual delivery behavior instead of fake success flags

---

## Troubleshooting

### 1. Forgot password returns 403
Check `api-gateway` security rules and ensure forgot-password endpoints are allowed without JWT.

### 2. OTP request fails with database column issues
Make sure the `password_reset_otps` table matches the current JPA entity mapping.

### 3. OTP mail works but recruiter/job seeker event mail does not
Make sure:
- `notification-service` is running
- RabbitMQ is running
- `APP_INTERNAL_API_KEY` matches where required
- the latest notification-service image is built and used

### 4. Docker starts old code
You probably restarted old containers without rebuilding images. Build the latest JARs and Docker images first.

### 5. Config changes are not reflected
Restart `config-server` and the affected services after updating the config repo.

---

## Security Notes

- Do not commit secrets to GitHub
- Keep `MAIL_PASSWORD` outside the config repo
- Change default database and JWT secrets before real deployment
- Review CORS and public endpoints before exposing the system publicly

---

## Future Improvements

- Add frontend integration
- Add pagination and filtering improvements
- Add audit logs for sensitive actions
- Add retry/dead-letter strategy for failed notifications
- Add CI/CD pipeline for image build and deployment
- Add better email content personalization

---

## Author

**Madhav Kolasani**

If you use this project publicly, keep the configuration clean, avoid committing secrets, and rebuild images whenever service code changes.