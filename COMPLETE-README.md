> LEGACY NOTE: This file was preserved from the previous version and may contain outdated examples for token flow, config expectations, and search-service storage. Use `README.md` and `FIXES-APPLIED.md` as the source of truth for the fixed project.

# Job Portal Management System — Full API Test Guide for the ZIP with Real PDF Resume Upload

This README is written for **`job-portal-management-system-real-pdf-resume-final.zip`**.
It is not theory. It is a practical testing guide for the code that is actually inside that ZIP.

Use this README when you want to:
- start the project
- open Swagger the correct way
- authorize once and test all services
- understand which service talks to which service
- test every endpoint with ready examples
- understand how the **application flow** really moves through the system

---

## 1. Main URLs

Use these after the app starts:

- **Gateway Swagger UI:** `http://localhost:8080/swagger-ui.html`
- Eureka: `http://localhost:8761`
- Config Server health: `http://localhost:8888/actuator/health`
- Zipkin: `http://localhost:9411`
- RabbitMQ Management: `http://localhost:15672`
  - username: `guest`
  - password: `guest`

---

## 2. Start the project properly

From the project root:

```bash
docker compose down -v
docker compose up -d
```

Then wait until:
- MySQL is up
- RabbitMQ is up
- Eureka is up
- Config Server is up
- all services register in Eureka
- gateway is reachable

Check this order:

1. `http://localhost:8761`
2. `http://localhost:8888/actuator/health`
3. `http://localhost:8080/swagger-ui.html`

If gateway Swagger is not opening, do **not** test APIs yet.

---

## 3. How Swagger authorization works in this project

Use only **gateway Swagger UI**:

`http://localhost:8080/swagger-ui.html`

Inside gateway Swagger, you will see docs for:
- user-service
- job-service
- application-service
- resume-service
- notification-service
- search-service

### Important

Because gateway Swagger uses `persistAuthorization=true`, you should:
1. login once
2. copy the token
3. click **Authorize** once in gateway Swagger
4. use the same UI for all services

Do **not** open different service Swagger pages on different ports and expect token sharing there.

---

## 4. Real service communication in this project

### External flow

Client -> API Gateway -> target service

### Internal synchronous communication

- `application-service` -> `job-service` using **OpenFeign**

Why?
- when a candidate applies
- when recruiter checks applications for a job
- when recruiter updates application status
- when candidate gets application history

`application-service` must fetch job info from `job-service` to know:
- whether job exists
- whether job is OPEN
- who owns that job
- what title to return in application response

### Internal asynchronous communication using RabbitMQ

- `job-service` publishes:
  - `job.created`
  - `job.closed`
- `application-service` publishes:
  - `job.applied`
  - `app.status.changed`
- `notification-service` consumes all of those and creates notifications

### Storage responsibilities

- `user-service` -> `user_db`
- `job-service` -> `job_db`
- `application-service` -> `application_db`
- `resume-service` -> `resume_db` + actual PDF files on disk/volume
- `notification-service` -> `notification_db`
- `search-service` -> reads from `job_db`

---

## 5. Full application working flow

This is the real business flow of the app.

### Step 1: User registration and login

- recruiter registers in `user-service`
- job seeker registers in `user-service`
- both login through `user-service`
- JWT token is returned

### Step 2: Recruiter creates a job

- client sends request to gateway
- gateway validates JWT
- request goes to `job-service`
- `job-service` stores the job in `job_db`
- `job-service` publishes `job.created`
- `notification-service` consumes `job.created`
- notification is stored for the recruiter

### Step 3: Candidate uploads a real resume PDF

- candidate sends multipart request through gateway
- gateway validates JWT
- request goes to `resume-service`
- `resume-service` validates `.pdf`
- file is stored on disk/volume
- metadata is stored in `resume_db`
- response returns `fileUrl` like `/api/resumes/{id}/file`

### Step 4: Candidate applies to a job

Application submission flow:

1. client -> gateway -> `application-service`
2. `application-service` receives `jobId`, `resumeId`, `coverLetter`
3. `application-service` calls `job-service` through **Feign** to fetch job summary
4. `job-service` returns job details
5. `application-service` checks:
   - user is JOB_SEEKER or ADMIN
   - job is OPEN
   - same candidate has not already applied
6. `application-service` saves application in `application_db`
7. `application-service` adds status history entry with `APPLIED`
8. `application-service` publishes `job.applied`
9. `notification-service` consumes that event
10. `notification-service` creates:
    - one notification for candidate
    - one notification for recruiter

### Step 5: Recruiter reviews applications

- recruiter gets applications for a job from `application-service`
- `application-service` again talks to `job-service` through Feign
- ownership check is performed using recruiterId from the job

### Step 6: Recruiter updates application status

- recruiter sends status update to `application-service`
- `application-service` checks recruiter ownership using `job-service`
- status changes from one state to another
- status history is saved
- `app.status.changed` event is published
- `notification-service` consumes it
- candidate gets a status update notification

### Step 7: Candidate checks notifications and history

- candidate reads notifications from `notification-service`
- candidate checks history from `application-service`

---

## 6. Roles you should create first for testing

Create these two users first.

### Recruiter example

```json
{
  "name": "Recruiter One",
  "email": "recruiter1@example.com",
  "password": "Recruiter1",
  "role": "RECRUITER",
  "phone": "9876543210"
}
```

### Job seeker example

```json
{
  "name": "Seeker One",
  "email": "seeker1@example.com",
  "password": "Seeker123",
  "role": "JOB_SEEKER",
  "phone": "9876501234"
}
```

### Important code behavior

- `ADMIN` registration is blocked through public API.
- `refresh-token` endpoint accepts **`accessToken`** in request body. That is how the current code is written.
- Resume upload is **multipart/form-data**, not JSON.
- `GET /api/resumes/{id}/file` returns **PDF binary**, not JSON.

---

## 7. Suggested master testing order

Follow this order. Do not test randomly.

1. Register recruiter
2. Register seeker
3. Login recruiter
4. Authorize with recruiter token in Swagger
5. Create two jobs
6. Get recruiter jobs
7. Login seeker
8. Authorize with seeker token in Swagger
9. Update seeker profile
10. Upload two PDF resumes
11. Set one resume as primary
12. Apply to one job
13. Check candidate applications
14. Login recruiter again
15. Get applications for that job
16. Update application status
17. Login seeker again
18. Check notifications
19. Check application history
20. Test public search APIs

---

## 8. IDs used in the examples below

Use your real IDs from Swagger responses. The ones below are only sample values.

- Recruiter userId: `1`
- Seeker userId: `2`
- Job A id: `101`
- Job B id: `102`
- Resume A id: `201`
- Resume B id: `202`
- Application A id: `301`
- Application B id: `302`
- Notification A id: `401`
- Notification B id: `402`

---

## 9. Token handling in Swagger

After login, Swagger response contains `accessToken`.

Authorize like this:

```text
Bearer paste_access_token_here
```

When switching from recruiter to seeker, replace the token in the same Swagger UI.

---

# 10. Complete API testing guide with examples

Below, every endpoint is covered.

**Rule used in this README:**
- for body-based APIs, you get **2 request JSON examples**
- for GET/DELETE/no-body APIs, you get **2 practical examples** with sample responses
- for file download endpoint, you get **2 request examples** because response is binary PDF, not JSON

---

# USER SERVICE

Base path: `/api/users`

## 10.1 POST /api/users/register
**Auth:** Public

### Request JSON example 1
```json
{
  "name": "Recruiter One",
  "email": "recruiter1@example.com",
  "password": "Recruiter1",
  "role": "RECRUITER",
  "phone": "9876543210"
}
```

### Request JSON example 2
```json
{
  "name": "Seeker One",
  "email": "seeker1@example.com",
  "password": "Seeker123",
  "role": "JOB_SEEKER",
  "phone": "9876501234"
}
```

### Sample success response 1
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "id": 1,
    "name": "Recruiter One",
    "email": "recruiter1@example.com",
    "role": "RECRUITER",
    "phone": "9876543210",
    "active": true,
    "createdAt": "2026-03-23T10:15:00"
  },
  "timestamp": "2026-03-23T10:15:00"
}
```

### Sample success response 2
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "id": 2,
    "name": "Seeker One",
    "email": "seeker1@example.com",
    "role": "JOB_SEEKER",
    "phone": "9876501234",
    "active": true,
    "createdAt": "2026-03-23T10:16:00"
  },
  "timestamp": "2026-03-23T10:16:00"
}
```

---

## 10.2 POST /api/users/login
**Auth:** Public

### Request JSON example 1
```json
{
  "email": "recruiter1@example.com",
  "password": "Recruiter1"
}
```

### Request JSON example 2
```json
{
  "email": "seeker1@example.com",
  "password": "Seeker123"
}
```

### Sample success response 1
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "userId": 1,
    "email": "recruiter1@example.com",
    "role": "RECRUITER",
    "accessToken": "<RECRUITER_JWT>",
    "tokenType": "Bearer",
    "expiresIn": 86400
  },
  "timestamp": "2026-03-23T10:20:00"
}
```

### Sample success response 2
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "userId": 2,
    "email": "seeker1@example.com",
    "role": "JOB_SEEKER",
    "accessToken": "<SEEKER_JWT>",
    "tokenType": "Bearer",
    "expiresIn": 86400
  },
  "timestamp": "2026-03-23T10:21:00"
}
```

---

## 10.3 POST /api/users/refresh-token
**Auth:** Public

### Request JSON example 1
```json
{
  "accessToken": "<EXPIRED_OR_OLD_RECRUITER_JWT>"
}
```

### Request JSON example 2
```json
{
  "accessToken": "<EXPIRED_OR_OLD_SEEKER_JWT>"
}
```

### Sample success response 1
```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "userId": 1,
    "email": "recruiter1@example.com",
    "role": "RECRUITER",
    "accessToken": "<NEW_RECRUITER_JWT>",
    "tokenType": "Bearer",
    "expiresIn": 86400
  },
  "timestamp": "2026-03-23T12:00:00"
}
```

### Sample success response 2
```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "userId": 2,
    "email": "seeker1@example.com",
    "role": "JOB_SEEKER",
    "accessToken": "<NEW_SEEKER_JWT>",
    "tokenType": "Bearer",
    "expiresIn": 86400
  },
  "timestamp": "2026-03-23T12:01:00"
}
```

---

## 10.4 GET /api/users/{id}
**Auth:** Required

### Example 1
`GET /api/users/1`

### Sample response 1
```json
{
  "success": true,
  "message": "User fetched successfully",
  "data": {
    "id": 1,
    "name": "Recruiter One",
    "email": "recruiter1@example.com",
    "role": "RECRUITER",
    "phone": "9876543210",
    "active": true,
    "createdAt": "2026-03-23T10:15:00"
  },
  "timestamp": "2026-03-23T10:30:00"
}
```

### Example 2
`GET /api/users/2`

### Sample response 2
```json
{
  "success": true,
  "message": "User fetched successfully",
  "data": {
    "id": 2,
    "name": "Seeker One",
    "email": "seeker1@example.com",
    "role": "JOB_SEEKER",
    "phone": "9876501234",
    "active": true,
    "createdAt": "2026-03-23T10:16:00"
  },
  "timestamp": "2026-03-23T10:31:00"
}
```

---

## 10.5 PUT /api/users/{id}
**Auth:** Required (self or admin intended)

### Request JSON example 1
```json
{
  "name": "Recruiter One Updated",
  "phone": "9999990001"
}
```

### Request JSON example 2
```json
{
  "name": "Seeker One Updated",
  "phone": "9999990002"
}
```

### Sample success response 1
```json
{
  "success": true,
  "message": "User updated successfully",
  "data": {
    "id": 1,
    "name": "Recruiter One Updated",
    "email": "recruiter1@example.com",
    "role": "RECRUITER",
    "phone": "9999990001",
    "active": true,
    "createdAt": "2026-03-23T10:15:00"
  },
  "timestamp": "2026-03-23T10:40:00"
}
```

### Sample success response 2
```json
{
  "success": true,
  "message": "User updated successfully",
  "data": {
    "id": 2,
    "name": "Seeker One Updated",
    "email": "seeker1@example.com",
    "role": "JOB_SEEKER",
    "phone": "9999990002",
    "active": true,
    "createdAt": "2026-03-23T10:16:00"
  },
  "timestamp": "2026-03-23T10:41:00"
}
```

---

## 10.6 PATCH /api/users/{id}/password
**Auth:** Required (self or admin intended)

### Request JSON example 1
```json
{
  "currentPassword": "Recruiter1",
  "newPassword": "Recruiter2"
}
```

### Request JSON example 2
```json
{
  "currentPassword": "Seeker123",
  "newPassword": "Seeker456"
}
```

### Sample success response 1
```json
{
  "success": true,
  "message": "Password changed successfully",
  "data": null,
  "timestamp": "2026-03-23T10:50:00"
}
```

### Sample success response 2
```json
{
  "success": true,
  "message": "Password changed successfully",
  "data": null,
  "timestamp": "2026-03-23T10:51:00"
}
```

---

## 10.7 DELETE /api/users/{id}
**Auth:** Required

> Important: in the current code, this endpoint does **not** enforce self/admin check in service logic. Test carefully.

### Example 1
`DELETE /api/users/2`

### Sample success response 1
```json
{
  "success": true,
  "message": "User deactivated successfully",
  "data": null,
  "timestamp": "2026-03-23T11:00:00"
}
```

### Example 2
`DELETE /api/users/1`

### Sample success response 2
```json
{
  "success": true,
  "message": "User deactivated successfully",
  "data": null,
  "timestamp": "2026-03-23T11:01:00"
}
```

---

## 10.8 GET /api/users
**Auth:** Required

> Important: in the current code, this endpoint is authenticated but not admin-locked in service logic.

### Example 1
`GET /api/users?page=0&size=20`

### Sample response 1
```json
{
  "success": true,
  "message": "Users fetched successfully",
  "data": {
    "content": [
      {
        "id": 1,
        "name": "Recruiter One",
        "email": "recruiter1@example.com",
        "role": "RECRUITER",
        "phone": "9876543210",
        "active": true,
        "createdAt": "2026-03-23T10:15:00"
      },
      {
        "id": 2,
        "name": "Seeker One",
        "email": "seeker1@example.com",
        "role": "JOB_SEEKER",
        "phone": "9876501234",
        "active": true,
        "createdAt": "2026-03-23T10:16:00"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20
    },
    "totalElements": 2,
    "totalPages": 1,
    "last": true,
    "first": true,
    "size": 20,
    "number": 0,
    "numberOfElements": 2,
    "empty": false
  },
  "timestamp": "2026-03-23T11:05:00"
}
```

### Example 2
`GET /api/users?role=JOB_SEEKER&page=0&size=10`

### Sample response 2
```json
{
  "success": true,
  "message": "Users fetched successfully",
  "data": {
    "content": [
      {
        "id": 2,
        "name": "Seeker One",
        "email": "seeker1@example.com",
        "role": "JOB_SEEKER",
        "phone": "9876501234",
        "active": true,
        "createdAt": "2026-03-23T10:16:00"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10
    },
    "totalElements": 1,
    "totalPages": 1,
    "last": true,
    "first": true,
    "size": 10,
    "number": 0,
    "numberOfElements": 1,
    "empty": false
  },
  "timestamp": "2026-03-23T11:06:00"
}
```

---

## 10.9 GET /api/users/{id}/profile
**Auth:** Required (self or admin)

### Example 1
`GET /api/users/1/profile`

### Sample response 1
```json
{
  "success": true,
  "message": "Profile fetched successfully",
  "data": {
    "id": 11,
    "userId": 1,
    "bio": "Hiring Java backend engineers for product teams.",
    "skills": "Hiring, Java, Spring Boot, Microservices",
    "experienceYrs": 8,
    "currentCompany": "Acme Corp",
    "location": "Bangalore",
    "linkedinUrl": "https://linkedin.com/in/recruiter-one",
    "githubUrl": "https://github.com/recruiter-one",
    "updatedAt": "2026-03-23T11:10:00"
  },
  "timestamp": "2026-03-23T11:10:00"
}
```

### Example 2
`GET /api/users/2/profile`

### Sample response 2
```json
{
  "success": true,
  "message": "Profile fetched successfully",
  "data": {
    "id": 12,
    "userId": 2,
    "bio": "Backend developer looking for Java and Spring roles.",
    "skills": "Java, Spring Boot, MySQL, REST API",
    "experienceYrs": 2,
    "currentCompany": "Freelance",
    "location": "Hyderabad",
    "linkedinUrl": "https://linkedin.com/in/seeker-one",
    "githubUrl": "https://github.com/seeker-one",
    "updatedAt": "2026-03-23T11:11:00"
  },
  "timestamp": "2026-03-23T11:11:00"
}
```

---

## 10.10 PUT /api/users/{id}/profile
**Auth:** Required (self or admin)

### Request JSON example 1
```json
{
  "bio": "Hiring Java backend engineers for scalable platform teams.",
  "skills": "Java, Spring Boot, Kafka, Hiring",
  "experienceYrs": 8,
  "currentCompany": "Acme Corp",
  "location": "Bangalore",
  "linkedinUrl": "https://linkedin.com/in/recruiter-one",
  "githubUrl": "https://github.com/recruiter-one"
}
```

### Request JSON example 2
```json
{
  "bio": "Backend developer interested in Java, Spring Boot and microservices roles.",
  "skills": "Java, Spring Boot, MySQL, Docker, AWS",
  "experienceYrs": 2,
  "currentCompany": "Self Employed",
  "location": "Hyderabad",
  "linkedinUrl": "https://linkedin.com/in/seeker-one",
  "githubUrl": "https://github.com/seeker-one"
}
```

### Sample success response 1
```json
{
  "success": true,
  "message": "Profile updated successfully",
  "data": {
    "id": 11,
    "userId": 1,
    "bio": "Hiring Java backend engineers for scalable platform teams.",
    "skills": "Java, Spring Boot, Kafka, Hiring",
    "experienceYrs": 8,
    "currentCompany": "Acme Corp",
    "location": "Bangalore",
    "linkedinUrl": "https://linkedin.com/in/recruiter-one",
    "githubUrl": "https://github.com/recruiter-one",
    "updatedAt": "2026-03-23T11:20:00"
  },
  "timestamp": "2026-03-23T11:20:00"
}
```

### Sample success response 2
```json
{
  "success": true,
  "message": "Profile updated successfully",
  "data": {
    "id": 12,
    "userId": 2,
    "bio": "Backend developer interested in Java, Spring Boot and microservices roles.",
    "skills": "Java, Spring Boot, MySQL, Docker, AWS",
    "experienceYrs": 2,
    "currentCompany": "Self Employed",
    "location": "Hyderabad",
    "linkedinUrl": "https://linkedin.com/in/seeker-one",
    "githubUrl": "https://github.com/seeker-one",
    "updatedAt": "2026-03-23T11:21:00"
  },
  "timestamp": "2026-03-23T11:21:00"
}
```

---

# JOB SERVICE

Base path: `/api/jobs`

## 10.11 POST /api/jobs
**Auth:** Recruiter/Admin

### Request JSON example 1
```json
{
  "title": "Java Backend Developer",
  "company": "Acme Corp",
  "location": "Bangalore",
  "salaryRange": "8-12 LPA",
  "description": "We are looking for a Java Backend Developer with strong Spring Boot, MySQL, REST API and microservices fundamentals for product backend development.",
  "requirements": "Java, Spring Boot, MySQL, REST, Git",
  "jobType": "FULL_TIME",
  "status": "OPEN",
  "skills": ["Java", "Spring Boot", "MySQL", "REST API"],
  "experienceMin": 1,
  "experienceMax": 3,
  "deadline": "2026-12-31"
}
```

### Request JSON example 2
```json
{
  "title": "Spring Boot Intern",
  "company": "Beta Labs",
  "location": "Hyderabad",
  "salaryRange": "Stipend 25000/month",
  "description": "We need an intern who can work on Spring Boot APIs, SQL queries, debugging and basic Docker workflows in a guided environment.",
  "requirements": "Java basics, OOP, SQL, Git",
  "jobType": "INTERNSHIP",
  "status": "DRAFT",
  "skills": ["Java", "SQL", "Git"],
  "experienceMin": 0,
  "experienceMax": 1,
  "deadline": "2026-09-30"
}
```

### Sample success response 1
```json
{
  "success": true,
  "message": "Job created successfully",
  "data": {
    "id": 101,
    "title": "Java Backend Developer",
    "company": "Acme Corp",
    "location": "Bangalore",
    "salaryRange": "8-12 LPA",
    "description": "We are looking for a Java Backend Developer with strong Spring Boot, MySQL, REST API and microservices fundamentals for product backend development.",
    "requirements": "Java, Spring Boot, MySQL, REST, Git",
    "jobType": "FULL_TIME",
    "status": "OPEN",
    "recruiterId": 1,
    "skills": ["Java", "Spring Boot", "MySQL", "REST API"],
    "experienceMin": 1,
    "experienceMax": 3,
    "deadline": "2026-12-31",
    "createdAt": "2026-03-23T11:30:00"
  },
  "timestamp": "2026-03-23T11:30:00"
}
```

### Sample success response 2
```json
{
  "success": true,
  "message": "Job created successfully",
  "data": {
    "id": 102,
    "title": "Spring Boot Intern",
    "company": "Beta Labs",
    "location": "Hyderabad",
    "salaryRange": "Stipend 25000/month",
    "description": "We need an intern who can work on Spring Boot APIs, SQL queries, debugging and basic Docker workflows in a guided environment.",
    "requirements": "Java basics, OOP, SQL, Git",
    "jobType": "INTERNSHIP",
    "status": "DRAFT",
    "recruiterId": 1,
    "skills": ["Java", "SQL", "Git"],
    "experienceMin": 0,
    "experienceMax": 1,
    "deadline": "2026-09-30",
    "createdAt": "2026-03-23T11:31:00"
  },
  "timestamp": "2026-03-23T11:31:00"
}
```

---

## 10.12 GET /api/jobs
**Auth:** Public

### Example 1
`GET /api/jobs?page=0&size=10`

### Sample response 1
```json
{
  "success": true,
  "message": "Jobs fetched successfully",
  "data": {
    "content": [
      {
        "id": 101,
        "title": "Java Backend Developer",
        "company": "Acme Corp",
        "location": "Bangalore",
        "salaryRange": "8-12 LPA",
        "description": "We are looking for a Java Backend Developer with strong Spring Boot, MySQL, REST API and microservices fundamentals for product backend development.",
        "requirements": "Java, Spring Boot, MySQL, REST, Git",
        "jobType": "FULL_TIME",
        "status": "OPEN",
        "recruiterId": 1,
        "skills": ["Java", "Spring Boot", "MySQL", "REST API"],
        "experienceMin": 1,
        "experienceMax": 3,
        "deadline": "2026-12-31",
        "createdAt": "2026-03-23T11:30:00"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10
    },
    "totalElements": 1,
    "totalPages": 1,
    "last": true,
    "first": true,
    "size": 10,
    "number": 0,
    "numberOfElements": 1,
    "empty": false
  },
  "timestamp": "2026-03-23T11:35:00"
}
```

### Example 2
`GET /api/jobs?page=1&size=5`

### Sample response 2
```json
{
  "success": true,
  "message": "Jobs fetched successfully",
  "data": {
    "content": [],
    "pageable": {
      "pageNumber": 1,
      "pageSize": 5
    },
    "totalElements": 1,
    "totalPages": 1,
    "last": true,
    "first": false,
    "size": 5,
    "number": 1,
    "numberOfElements": 0,
    "empty": true
  },
  "timestamp": "2026-03-23T11:36:00"
}
```

---

## 10.13 GET /api/jobs/{id}
**Auth:** Public

### Example 1
`GET /api/jobs/101`

### Sample response 1
```json
{
  "success": true,
  "message": "Job fetched successfully",
  "data": {
    "id": 101,
    "title": "Java Backend Developer",
    "company": "Acme Corp",
    "location": "Bangalore",
    "salaryRange": "8-12 LPA",
    "description": "We are looking for a Java Backend Developer with strong Spring Boot, MySQL, REST API and microservices fundamentals for product backend development.",
    "requirements": "Java, Spring Boot, MySQL, REST, Git",
    "jobType": "FULL_TIME",
    "status": "OPEN",
    "recruiterId": 1,
    "skills": ["Java", "Spring Boot", "MySQL", "REST API"],
    "experienceMin": 1,
    "experienceMax": 3,
    "deadline": "2026-12-31",
    "createdAt": "2026-03-23T11:30:00"
  },
  "timestamp": "2026-03-23T11:40:00"
}
```

### Example 2
`GET /api/jobs/102`

### Sample response 2
```json
{
  "success": true,
  "message": "Job fetched successfully",
  "data": {
    "id": 102,
    "title": "Spring Boot Intern",
    "company": "Beta Labs",
    "location": "Hyderabad",
    "salaryRange": "Stipend 25000/month",
    "description": "We need an intern who can work on Spring Boot APIs, SQL queries, debugging and basic Docker workflows in a guided environment.",
    "requirements": "Java basics, OOP, SQL, Git",
    "jobType": "INTERNSHIP",
    "status": "DRAFT",
    "recruiterId": 1,
    "skills": ["Java", "SQL", "Git"],
    "experienceMin": 0,
    "experienceMax": 1,
    "deadline": "2026-09-30",
    "createdAt": "2026-03-23T11:31:00"
  },
  "timestamp": "2026-03-23T11:41:00"
}
```

---

## 10.14 PUT /api/jobs/{id}
**Auth:** Recruiter/Admin

### Request JSON example 1
```json
{
  "title": "Java Backend Developer - Updated",
  "company": "Acme Corp",
  "location": "Bangalore",
  "salaryRange": "10-14 LPA",
  "description": "We are looking for a Java Backend Developer with Spring Boot, Microservices, SQL, Docker and cloud fundamentals to build backend APIs.",
  "requirements": "Java, Spring Boot, MySQL, Docker, REST",
  "jobType": "FULL_TIME",
  "status": "OPEN",
  "skills": ["Java", "Spring Boot", "Docker", "MySQL"],
  "experienceMin": 2,
  "experienceMax": 4,
  "deadline": "2027-01-31"
}
```

### Request JSON example 2
```json
{
  "title": "Spring Boot Intern - Updated",
  "company": "Beta Labs",
  "location": "Hyderabad",
  "salaryRange": "Stipend 30000/month",
  "description": "We need an intern to support API development, SQL debugging, documentation and basic containerized local setup tasks.",
  "requirements": "Java basics, SQL, Git, debugging",
  "jobType": "INTERNSHIP",
  "status": "DRAFT",
  "skills": ["Java", "SQL", "Git", "Postman"],
  "experienceMin": 0,
  "experienceMax": 1,
  "deadline": "2026-10-31"
}
```

### Sample success response 1
```json
{
  "success": true,
  "message": "Job updated successfully",
  "data": {
    "id": 101,
    "title": "Java Backend Developer - Updated",
    "company": "Acme Corp",
    "location": "Bangalore",
    "salaryRange": "10-14 LPA",
    "description": "We are looking for a Java Backend Developer with Spring Boot, Microservices, SQL, Docker and cloud fundamentals to build backend APIs.",
    "requirements": "Java, Spring Boot, MySQL, Docker, REST",
    "jobType": "FULL_TIME",
    "status": "OPEN",
    "recruiterId": 1,
    "skills": ["Java", "Spring Boot", "Docker", "MySQL"],
    "experienceMin": 2,
    "experienceMax": 4,
    "deadline": "2027-01-31",
    "createdAt": "2026-03-23T11:30:00"
  },
  "timestamp": "2026-03-23T11:50:00"
}
```

### Sample success response 2
```json
{
  "success": true,
  "message": "Job updated successfully",
  "data": {
    "id": 102,
    "title": "Spring Boot Intern - Updated",
    "company": "Beta Labs",
    "location": "Hyderabad",
    "salaryRange": "Stipend 30000/month",
    "description": "We need an intern to support API development, SQL debugging, documentation and basic containerized local setup tasks.",
    "requirements": "Java basics, SQL, Git, debugging",
    "jobType": "INTERNSHIP",
    "status": "DRAFT",
    "recruiterId": 1,
    "skills": ["Java", "SQL", "Git", "Postman"],
    "experienceMin": 0,
    "experienceMax": 1,
    "deadline": "2026-10-31",
    "createdAt": "2026-03-23T11:31:00"
  },
  "timestamp": "2026-03-23T11:51:00"
}
```

---

## 10.15 PUT /api/jobs/{id}/close
**Auth:** Recruiter/Admin

### Example 1
`PUT /api/jobs/101/close`

### Sample response 1
```json
{
  "success": true,
  "message": "Job closed successfully",
  "data": {
    "id": 101,
    "title": "Java Backend Developer - Updated",
    "company": "Acme Corp",
    "location": "Bangalore",
    "salaryRange": "10-14 LPA",
    "description": "We are looking for a Java Backend Developer with Spring Boot, Microservices, SQL, Docker and cloud fundamentals to build backend APIs.",
    "requirements": "Java, Spring Boot, MySQL, Docker, REST",
    "jobType": "FULL_TIME",
    "status": "CLOSED",
    "recruiterId": 1,
    "skills": ["Java", "Spring Boot", "Docker", "MySQL"],
    "experienceMin": 2,
    "experienceMax": 4,
    "deadline": "2027-01-31",
    "createdAt": "2026-03-23T11:30:00"
  },
  "timestamp": "2026-03-23T12:00:00"
}
```

### Example 2
`PUT /api/jobs/103/close`

### Sample response 2
```json
{
  "success": true,
  "message": "Job closed successfully",
  "data": {
    "id": 103,
    "title": "QA Engineer",
    "company": "Gamma Tech",
    "location": "Pune",
    "salaryRange": "6-9 LPA",
    "description": "QA engineer role focusing on API testing, regression validation and bug reporting for backend and web applications.",
    "requirements": "Testing, Postman, SQL",
    "jobType": "FULL_TIME",
    "status": "CLOSED",
    "recruiterId": 1,
    "skills": ["Testing", "Postman", "SQL"],
    "experienceMin": 1,
    "experienceMax": 3,
    "deadline": "2026-11-30",
    "createdAt": "2026-03-23T11:45:00"
  },
  "timestamp": "2026-03-23T12:01:00"
}
```

---

## 10.16 PUT /api/jobs/{id}/reopen
**Auth:** Recruiter/Admin

### Example 1
`PUT /api/jobs/101/reopen`

### Sample response 1
```json
{
  "success": true,
  "message": "Job reopened successfully",
  "data": {
    "id": 101,
    "title": "Java Backend Developer - Updated",
    "company": "Acme Corp",
    "location": "Bangalore",
    "salaryRange": "10-14 LPA",
    "description": "We are looking for a Java Backend Developer with Spring Boot, Microservices, SQL, Docker and cloud fundamentals to build backend APIs.",
    "requirements": "Java, Spring Boot, MySQL, Docker, REST",
    "jobType": "FULL_TIME",
    "status": "OPEN",
    "recruiterId": 1,
    "skills": ["Java", "Spring Boot", "Docker", "MySQL"],
    "experienceMin": 2,
    "experienceMax": 4,
    "deadline": "2027-01-31",
    "createdAt": "2026-03-23T11:30:00"
  },
  "timestamp": "2026-03-23T12:05:00"
}
```

### Example 2
`PUT /api/jobs/103/reopen`

### Sample response 2
```json
{
  "success": true,
  "message": "Job reopened successfully",
  "data": {
    "id": 103,
    "title": "QA Engineer",
    "company": "Gamma Tech",
    "location": "Pune",
    "salaryRange": "6-9 LPA",
    "description": "QA engineer role focusing on API testing, regression validation and bug reporting for backend and web applications.",
    "requirements": "Testing, Postman, SQL",
    "jobType": "FULL_TIME",
    "status": "OPEN",
    "recruiterId": 1,
    "skills": ["Testing", "Postman", "SQL"],
    "experienceMin": 1,
    "experienceMax": 3,
    "deadline": "2026-11-30",
    "createdAt": "2026-03-23T11:45:00"
  },
  "timestamp": "2026-03-23T12:06:00"
}
```

---

## 10.17 DELETE /api/jobs/{id}
**Auth:** Recruiter/Admin

> This is intended for deleting a **DRAFT** job.

### Example 1
`DELETE /api/jobs/102`

### Sample response 1
```json
{
  "success": true,
  "message": "Draft job deleted successfully",
  "data": null,
  "timestamp": "2026-03-23T12:10:00"
}
```

### Example 2
`DELETE /api/jobs/104`

### Sample response 2
```json
{
  "success": true,
  "message": "Draft job deleted successfully",
  "data": null,
  "timestamp": "2026-03-23T12:11:00"
}
```

---

## 10.18 GET /api/jobs/recruiter/{recruiterId}
**Auth:** Recruiter/Admin

### Example 1
`GET /api/jobs/recruiter/1`

### Sample response 1
```json
{
  "success": true,
  "message": "Recruiter jobs fetched successfully",
  "data": [
    {
      "id": 101,
      "title": "Java Backend Developer - Updated",
      "company": "Acme Corp",
      "location": "Bangalore",
      "salaryRange": "10-14 LPA",
      "description": "We are looking for a Java Backend Developer with Spring Boot, Microservices, SQL, Docker and cloud fundamentals to build backend APIs.",
      "requirements": "Java, Spring Boot, MySQL, Docker, REST",
      "jobType": "FULL_TIME",
      "status": "OPEN",
      "recruiterId": 1,
      "skills": ["Java", "Spring Boot", "Docker", "MySQL"],
      "experienceMin": 2,
      "experienceMax": 4,
      "deadline": "2027-01-31",
      "createdAt": "2026-03-23T11:30:00"
    }
  ],
  "timestamp": "2026-03-23T12:15:00"
}
```

### Example 2
`GET /api/jobs/recruiter/5`

### Sample response 2
```json
{
  "success": true,
  "message": "Recruiter jobs fetched successfully",
  "data": [
    {
      "id": 205,
      "title": "DevOps Engineer",
      "company": "InfraWorks",
      "location": "Chennai",
      "salaryRange": "12-16 LPA",
      "description": "DevOps engineer role focused on CI/CD, Docker, monitoring, automation and deployment pipeline improvements.",
      "requirements": "Linux, Docker, CI/CD, Monitoring",
      "jobType": "FULL_TIME",
      "status": "OPEN",
      "recruiterId": 5,
      "skills": ["Docker", "Linux", "CI/CD", "Monitoring"],
      "experienceMin": 3,
      "experienceMax": 5,
      "deadline": "2027-02-28",
      "createdAt": "2026-03-23T12:14:00"
    }
  ],
  "timestamp": "2026-03-23T12:16:00"
}
```

---

## 10.19 GET /api/jobs/{id}/skills
**Auth:** Public

### Example 1
`GET /api/jobs/101/skills`

### Sample response 1
```json
{
  "success": true,
  "message": "Job skills fetched successfully",
  "data": ["Java", "Spring Boot", "Docker", "MySQL"],
  "timestamp": "2026-03-23T12:20:00"
}
```

### Example 2
`GET /api/jobs/103/skills`

### Sample response 2
```json
{
  "success": true,
  "message": "Job skills fetched successfully",
  "data": ["Testing", "Postman", "SQL"],
  "timestamp": "2026-03-23T12:21:00"
}
```

---

# APPLICATION SERVICE

Base path: `/api/applications`

## 10.20 POST /api/applications
**Auth:** Job seeker/Admin

### Request JSON example 1
```json
{
  "jobId": 101,
  "resumeId": 201,
  "coverLetter": "I am interested in this role because my backend skills match the Java, Spring Boot and MySQL requirements."
}
```

### Request JSON example 2
```json
{
  "jobId": 103,
  "resumeId": 202,
  "coverLetter": "I have experience with API testing, SQL validation and debugging workflows and would like to apply for this position."
}
```

### Sample success response 1
```json
{
  "success": true,
  "message": "Applied successfully",
  "data": {
    "id": 301,
    "jobId": 101,
    "jobTitle": "Java Backend Developer - Updated",
    "candidateId": 2,
    "resumeId": 201,
    "status": "APPLIED",
    "coverLetter": "I am interested in this role because my backend skills match the Java, Spring Boot and MySQL requirements.",
    "appliedAt": "2026-03-23T12:30:00"
  },
  "timestamp": "2026-03-23T12:30:00"
}
```

### Sample success response 2
```json
{
  "success": true,
  "message": "Applied successfully",
  "data": {
    "id": 302,
    "jobId": 103,
    "jobTitle": "QA Engineer",
    "candidateId": 2,
    "resumeId": 202,
    "status": "APPLIED",
    "coverLetter": "I have experience with API testing, SQL validation and debugging workflows and would like to apply for this position.",
    "appliedAt": "2026-03-23T12:31:00"
  },
  "timestamp": "2026-03-23T12:31:00"
}
```

---

## 10.21 GET /api/applications/{id}
**Auth:** Candidate owner / owning recruiter / admin

### Example 1
`GET /api/applications/301`

### Sample response 1
```json
{
  "success": true,
  "message": "Application fetched successfully",
  "data": {
    "id": 301,
    "jobId": 101,
    "jobTitle": "Java Backend Developer - Updated",
    "candidateId": 2,
    "resumeId": 201,
    "status": "APPLIED",
    "coverLetter": "I am interested in this role because my backend skills match the Java, Spring Boot and MySQL requirements.",
    "appliedAt": "2026-03-23T12:30:00"
  },
  "timestamp": "2026-03-23T12:35:00"
}
```

### Example 2
`GET /api/applications/302`

### Sample response 2
```json
{
  "success": true,
  "message": "Application fetched successfully",
  "data": {
    "id": 302,
    "jobId": 103,
    "jobTitle": "QA Engineer",
    "candidateId": 2,
    "resumeId": 202,
    "status": "APPLIED",
    "coverLetter": "I have experience with API testing, SQL validation and debugging workflows and would like to apply for this position.",
    "appliedAt": "2026-03-23T12:31:00"
  },
  "timestamp": "2026-03-23T12:36:00"
}
```

---

## 10.22 GET /api/applications/job/{jobId}
**Auth:** Owning recruiter/Admin

### Example 1
`GET /api/applications/job/101`

### Sample response 1
```json
{
  "success": true,
  "message": "Applications fetched successfully",
  "data": [
    {
      "id": 301,
      "jobId": 101,
      "jobTitle": "Java Backend Developer - Updated",
      "candidateId": 2,
      "resumeId": 201,
      "status": "APPLIED",
      "coverLetter": "I am interested in this role because my backend skills match the Java, Spring Boot and MySQL requirements.",
      "appliedAt": "2026-03-23T12:30:00"
    }
  ],
  "timestamp": "2026-03-23T12:40:00"
}
```

### Example 2
`GET /api/applications/job/103`

### Sample response 2
```json
{
  "success": true,
  "message": "Applications fetched successfully",
  "data": [
    {
      "id": 302,
      "jobId": 103,
      "jobTitle": "QA Engineer",
      "candidateId": 2,
      "resumeId": 202,
      "status": "APPLIED",
      "coverLetter": "I have experience with API testing, SQL validation and debugging workflows and would like to apply for this position.",
      "appliedAt": "2026-03-23T12:31:00"
    }
  ],
  "timestamp": "2026-03-23T12:41:00"
}
```

---

## 10.23 GET /api/applications/candidate/{candidateId}
**Auth:** Candidate self/Admin

### Example 1
`GET /api/applications/candidate/2`

### Sample response 1
```json
{
  "success": true,
  "message": "Applications fetched successfully",
  "data": [
    {
      "id": 301,
      "jobId": 101,
      "jobTitle": "Java Backend Developer - Updated",
      "candidateId": 2,
      "resumeId": 201,
      "status": "APPLIED",
      "coverLetter": "I am interested in this role because my backend skills match the Java, Spring Boot and MySQL requirements.",
      "appliedAt": "2026-03-23T12:30:00"
    },
    {
      "id": 302,
      "jobId": 103,
      "jobTitle": "QA Engineer",
      "candidateId": 2,
      "resumeId": 202,
      "status": "APPLIED",
      "coverLetter": "I have experience with API testing, SQL validation and debugging workflows and would like to apply for this position.",
      "appliedAt": "2026-03-23T12:31:00"
    }
  ],
  "timestamp": "2026-03-23T12:45:00"
}
```

### Example 2
`GET /api/applications/candidate/7`

### Sample response 2
```json
{
  "success": true,
  "message": "Applications fetched successfully",
  "data": [
    {
      "id": 410,
      "jobId": 220,
      "jobTitle": "Frontend Developer",
      "candidateId": 7,
      "resumeId": 350,
      "status": "SHORTLISTED",
      "coverLetter": "Strong frontend portfolio and React experience.",
      "appliedAt": "2026-03-22T15:10:00"
    }
  ],
  "timestamp": "2026-03-23T12:46:00"
}
```

---

## 10.24 PUT /api/applications/{id}/status
**Auth:** Owning recruiter/Admin

### Request JSON example 1
```json
{
  "status": "SHORTLISTED",
  "notes": "Good backend fundamentals and relevant project experience"
}
```

### Request JSON example 2
```json
{
  "status": "INTERVIEW_SCHEDULED",
  "notes": "Technical interview scheduled for next Monday"
}
```

### Sample success response 1
```json
{
  "success": true,
  "message": "Application status updated successfully",
  "data": {
    "id": 301,
    "jobId": 101,
    "jobTitle": "Java Backend Developer - Updated",
    "candidateId": 2,
    "resumeId": 201,
    "status": "SHORTLISTED",
    "coverLetter": "I am interested in this role because my backend skills match the Java, Spring Boot and MySQL requirements.",
    "appliedAt": "2026-03-23T12:30:00"
  },
  "timestamp": "2026-03-23T12:50:00"
}
```

### Sample success response 2
```json
{
  "success": true,
  "message": "Application status updated successfully",
  "data": {
    "id": 301,
    "jobId": 101,
    "jobTitle": "Java Backend Developer - Updated",
    "candidateId": 2,
    "resumeId": 201,
    "status": "INTERVIEW_SCHEDULED",
    "coverLetter": "I am interested in this role because my backend skills match the Java, Spring Boot and MySQL requirements.",
    "appliedAt": "2026-03-23T12:30:00"
  },
  "timestamp": "2026-03-23T12:55:00"
}
```

---

## 10.25 PUT /api/applications/{id}/withdraw
**Auth:** Candidate self/Admin

### Example 1
`PUT /api/applications/302/withdraw`

### Sample response 1
```json
{
  "success": true,
  "message": "Application withdrawn successfully",
  "data": {
    "id": 302,
    "jobId": 103,
    "jobTitle": "QA Engineer",
    "candidateId": 2,
    "resumeId": 202,
    "status": "WITHDRAWN",
    "coverLetter": "I have experience with API testing, SQL validation and debugging workflows and would like to apply for this position.",
    "appliedAt": "2026-03-23T12:31:00"
  },
  "timestamp": "2026-03-23T13:00:00"
}
```

### Example 2
`PUT /api/applications/410/withdraw`

### Sample response 2
```json
{
  "success": true,
  "message": "Application withdrawn successfully",
  "data": {
    "id": 410,
    "jobId": 220,
    "jobTitle": "Frontend Developer",
    "candidateId": 7,
    "resumeId": 350,
    "status": "WITHDRAWN",
    "coverLetter": "Strong frontend portfolio and React experience.",
    "appliedAt": "2026-03-22T15:10:00"
  },
  "timestamp": "2026-03-23T13:01:00"
}
```

---

## 10.26 GET /api/applications/{id}/history
**Auth:** Candidate owner / owning recruiter / admin

### Example 1
`GET /api/applications/301/history`

### Sample response 1
```json
{
  "success": true,
  "message": "Application history fetched successfully",
  "data": [
    {
      "id": 1,
      "oldStatus": null,
      "newStatus": "APPLIED",
      "changedBy": 2,
      "notes": "Application submitted",
      "changedAt": "2026-03-23T12:30:00"
    },
    {
      "id": 2,
      "oldStatus": "APPLIED",
      "newStatus": "SHORTLISTED",
      "changedBy": 1,
      "notes": "Good backend fundamentals and relevant project experience",
      "changedAt": "2026-03-23T12:50:00"
    },
    {
      "id": 3,
      "oldStatus": "SHORTLISTED",
      "newStatus": "INTERVIEW_SCHEDULED",
      "changedBy": 1,
      "notes": "Technical interview scheduled for next Monday",
      "changedAt": "2026-03-23T12:55:00"
    }
  ],
  "timestamp": "2026-03-23T13:05:00"
}
```

### Example 2
`GET /api/applications/302/history`

### Sample response 2
```json
{
  "success": true,
  "message": "Application history fetched successfully",
  "data": [
    {
      "id": 10,
      "oldStatus": null,
      "newStatus": "APPLIED",
      "changedBy": 2,
      "notes": "Application submitted",
      "changedAt": "2026-03-23T12:31:00"
    },
    {
      "id": 11,
      "oldStatus": "APPLIED",
      "newStatus": "WITHDRAWN",
      "changedBy": 2,
      "notes": "Application withdrawn",
      "changedAt": "2026-03-23T13:00:00"
    }
  ],
  "timestamp": "2026-03-23T13:06:00"
}
```

---

# RESUME SERVICE

Base path: `/api/resumes`

## 10.27 POST /api/resumes
**Auth:** Job seeker/Admin
**Content-Type:** `multipart/form-data`

> This endpoint does **not** accept JSON. It accepts a real PDF file.

### Example 1 form-data
- `file` = `resume_backend_java.pdf`
- `primary` = `true`

### Example 1 curl
```bash
curl -X POST "http://localhost:8080/api/resumes" \
  -H "Authorization: Bearer <SEEKER_JWT>" \
  -F "file=@resume_backend_java.pdf;type=application/pdf" \
  -F "primary=true"
```

### Example 2 form-data
- `file` = `resume_testing_profile.pdf`
- `primary` = `false`

### Example 2 curl
```bash
curl -X POST "http://localhost:8080/api/resumes" \
  -H "Authorization: Bearer <SEEKER_JWT>" \
  -F "file=@resume_testing_profile.pdf;type=application/pdf" \
  -F "primary=false"
```

### Sample success response 1
```json
{
  "success": true,
  "message": "Resume uploaded successfully",
  "data": {
    "id": 201,
    "userId": 2,
    "fileName": "resume_backend_java.pdf",
    "fileUrl": "/api/resumes/201/file",
    "fileSize": 245760,
    "primary": true,
    "createdAt": "2026-03-23T13:15:00"
  },
  "timestamp": "2026-03-23T13:15:00"
}
```

### Sample success response 2
```json
{
  "success": true,
  "message": "Resume uploaded successfully",
  "data": {
    "id": 202,
    "userId": 2,
    "fileName": "resume_testing_profile.pdf",
    "fileUrl": "/api/resumes/202/file",
    "fileSize": 198450,
    "primary": false,
    "createdAt": "2026-03-23T13:16:00"
  },
  "timestamp": "2026-03-23T13:16:00"
}
```

---

## 10.28 GET /api/resumes/{id}
**Auth:** Owner/Admin

### Example 1
`GET /api/resumes/201`

### Sample response 1
```json
{
  "success": true,
  "message": "Resume fetched successfully",
  "data": {
    "id": 201,
    "userId": 2,
    "fileName": "resume_backend_java.pdf",
    "fileUrl": "/api/resumes/201/file",
    "fileSize": 245760,
    "primary": true,
    "createdAt": "2026-03-23T13:15:00"
  },
  "timestamp": "2026-03-23T13:20:00"
}
```

### Example 2
`GET /api/resumes/202`

### Sample response 2
```json
{
  "success": true,
  "message": "Resume fetched successfully",
  "data": {
    "id": 202,
    "userId": 2,
    "fileName": "resume_testing_profile.pdf",
    "fileUrl": "/api/resumes/202/file",
    "fileSize": 198450,
    "primary": false,
    "createdAt": "2026-03-23T13:16:00"
  },
  "timestamp": "2026-03-23T13:21:00"
}
```

---

## 10.29 GET /api/resumes/user/{userId}
**Auth:** Owner/Admin

### Example 1
`GET /api/resumes/user/2`

### Sample response 1
```json
{
  "success": true,
  "message": "Resumes fetched successfully",
  "data": [
    {
      "id": 202,
      "userId": 2,
      "fileName": "resume_testing_profile.pdf",
      "fileUrl": "/api/resumes/202/file",
      "fileSize": 198450,
      "primary": false,
      "createdAt": "2026-03-23T13:16:00"
    },
    {
      "id": 201,
      "userId": 2,
      "fileName": "resume_backend_java.pdf",
      "fileUrl": "/api/resumes/201/file",
      "fileSize": 245760,
      "primary": true,
      "createdAt": "2026-03-23T13:15:00"
    }
  ],
  "timestamp": "2026-03-23T13:25:00"
}
```

### Example 2
`GET /api/resumes/user/7`

### Sample response 2
```json
{
  "success": true,
  "message": "Resumes fetched successfully",
  "data": [
    {
      "id": 350,
      "userId": 7,
      "fileName": "resume_frontend.pdf",
      "fileUrl": "/api/resumes/350/file",
      "fileSize": 188000,
      "primary": true,
      "createdAt": "2026-03-22T09:20:00"
    }
  ],
  "timestamp": "2026-03-23T13:26:00"
}
```

---

## 10.30 PUT /api/resumes/{id}/primary
**Auth:** Owner/Admin

### Example 1
`PUT /api/resumes/202/primary`

### Sample response 1
```json
{
  "success": true,
  "message": "Primary resume updated successfully",
  "data": {
    "id": 202,
    "userId": 2,
    "fileName": "resume_testing_profile.pdf",
    "fileUrl": "/api/resumes/202/file",
    "fileSize": 198450,
    "primary": true,
    "createdAt": "2026-03-23T13:16:00"
  },
  "timestamp": "2026-03-23T13:30:00"
}
```

### Example 2
`PUT /api/resumes/201/primary`

### Sample response 2
```json
{
  "success": true,
  "message": "Primary resume updated successfully",
  "data": {
    "id": 201,
    "userId": 2,
    "fileName": "resume_backend_java.pdf",
    "fileUrl": "/api/resumes/201/file",
    "fileSize": 245760,
    "primary": true,
    "createdAt": "2026-03-23T13:15:00"
  },
  "timestamp": "2026-03-23T13:31:00"
}
```

---

## 10.31 GET /api/resumes/{id}/file
**Auth:** Owner/Admin
**Response type:** `application/pdf`

> This endpoint returns the actual PDF file bytes, not JSON.

### Example 1 request
```bash
curl -X GET "http://localhost:8080/api/resumes/201/file" \
  -H "Authorization: Bearer <SEEKER_JWT>" \
  --output downloaded_resume_201.pdf
```

### Expected behavior 1
- HTTP 200
- `Content-Type: application/pdf`
- browser opens inline or curl saves the file

### Example 2 request
```bash
curl -X GET "http://localhost:8080/api/resumes/202/file" \
  -H "Authorization: Bearer <SEEKER_JWT>" \
  --output downloaded_resume_202.pdf
```

### Expected behavior 2
- HTTP 200
- `Content-Disposition: inline; filename="resume_testing_profile.pdf"`
- actual PDF is returned

---

## 10.32 DELETE /api/resumes/{id}
**Auth:** Owner/Admin

### Example 1
`DELETE /api/resumes/202`

### Sample response 1
```json
{
  "success": true,
  "message": "Resume deleted successfully",
  "data": null,
  "timestamp": "2026-03-23T13:40:00"
}
```

### Example 2
`DELETE /api/resumes/350`

### Sample response 2
```json
{
  "success": true,
  "message": "Resume deleted successfully",
  "data": null,
  "timestamp": "2026-03-23T13:41:00"
}
```

---

# NOTIFICATION SERVICE

Base path: `/api/notifications`

## 10.33 GET /api/notifications/user/{userId}
**Auth:** Owner/Admin

### Example 1
`GET /api/notifications/user/2`

### Sample response 1
```json
{
  "success": true,
  "message": "Notifications fetched successfully",
  "data": [
    {
      "id": 401,
      "recipientId": 2,
      "type": "EMAIL",
      "subject": "Application status updated",
      "body": "Your application status has been changed to INTERVIEW_SCHEDULED.",
      "status": "SENT",
      "eventType": "app.status.changed",
      "referenceId": 301,
      "read": false,
      "sentAt": "2026-03-23T12:55:01",
      "createdAt": "2026-03-23T12:55:01"
    },
    {
      "id": 402,
      "recipientId": 2,
      "type": "EMAIL",
      "subject": "Application submitted",
      "body": "Your application for job ID 101 has been submitted successfully.",
      "status": "SENT",
      "eventType": "job.applied",
      "referenceId": 301,
      "read": false,
      "sentAt": "2026-03-23T12:30:01",
      "createdAt": "2026-03-23T12:30:01"
    }
  ],
  "timestamp": "2026-03-23T13:50:00"
}
```

### Example 2
`GET /api/notifications/user/1`

### Sample response 2
```json
{
  "success": true,
  "message": "Notifications fetched successfully",
  "data": [
    {
      "id": 450,
      "recipientId": 1,
      "type": "EMAIL",
      "subject": "New application received",
      "body": "A candidate has applied to your job ID 101.",
      "status": "SENT",
      "eventType": "job.applied",
      "referenceId": 301,
      "read": false,
      "sentAt": "2026-03-23T12:30:01",
      "createdAt": "2026-03-23T12:30:01"
    },
    {
      "id": 451,
      "recipientId": 1,
      "type": "EMAIL",
      "subject": "Job created successfully",
      "body": "Your job 'Java Backend Developer' at Acme Corp was created.",
      "status": "SENT",
      "eventType": "job.created",
      "referenceId": 101,
      "read": true,
      "sentAt": "2026-03-23T11:30:01",
      "createdAt": "2026-03-23T11:30:01"
    }
  ],
  "timestamp": "2026-03-23T13:51:00"
}
```

---

## 10.34 PUT /api/notifications/{id}/read
**Auth:** Owner/Admin

### Example 1
`PUT /api/notifications/401/read`

### Sample response 1
```json
{
  "success": true,
  "message": "Notification marked as read",
  "data": {
    "id": 401,
    "recipientId": 2,
    "type": "EMAIL",
    "subject": "Application status updated",
    "body": "Your application status has been changed to INTERVIEW_SCHEDULED.",
    "status": "SENT",
    "eventType": "app.status.changed",
    "referenceId": 301,
    "read": true,
    "sentAt": "2026-03-23T12:55:01",
    "createdAt": "2026-03-23T12:55:01"
  },
  "timestamp": "2026-03-23T13:55:00"
}
```

### Example 2
`PUT /api/notifications/450/read`

### Sample response 2
```json
{
  "success": true,
  "message": "Notification marked as read",
  "data": {
    "id": 450,
    "recipientId": 1,
    "type": "EMAIL",
    "subject": "New application received",
    "body": "A candidate has applied to your job ID 101.",
    "status": "SENT",
    "eventType": "job.applied",
    "referenceId": 301,
    "read": true,
    "sentAt": "2026-03-23T12:30:01",
    "createdAt": "2026-03-23T12:30:01"
  },
  "timestamp": "2026-03-23T13:56:00"
}
```

---

## 10.35 PUT /api/notifications/user/{userId}/read-all
**Auth:** Owner/Admin

### Example 1
`PUT /api/notifications/user/2/read-all`

### Sample response 1
```json
{
  "success": true,
  "message": "All notifications marked as read",
  "data": null,
  "timestamp": "2026-03-23T14:00:00"
}
```

### Example 2
`PUT /api/notifications/user/1/read-all`

### Sample response 2
```json
{
  "success": true,
  "message": "All notifications marked as read",
  "data": null,
  "timestamp": "2026-03-23T14:01:00"
}
```

---

## 10.36 DELETE /api/notifications/{id}
**Auth:** Owner/Admin

### Example 1
`DELETE /api/notifications/401`

### Sample response 1
```json
{
  "success": true,
  "message": "Notification deleted successfully",
  "data": null,
  "timestamp": "2026-03-23T14:05:00"
}
```

### Example 2
`DELETE /api/notifications/450`

### Sample response 2
```json
{
  "success": true,
  "message": "Notification deleted successfully",
  "data": null,
  "timestamp": "2026-03-23T14:06:00"
}
```

---

# SEARCH SERVICE

Base path: `/api/search`

## 10.37 GET /api/search/jobs
**Auth:** Public

### Example 1
`GET /api/search/jobs?keyword=java&location=bangalore&type=FULL_TIME&expMin=1&expMax=4&page=0&size=10`

### Sample response 1
```json
{
  "success": true,
  "message": "Search results fetched successfully",
  "data": {
    "content": [
      {
        "id": 101,
        "title": "Java Backend Developer - Updated",
        "company": "Acme Corp",
        "location": "Bangalore",
        "salaryRange": "10-14 LPA",
        "jobType": "FULL_TIME",
        "status": "OPEN",
        "experienceMin": 2,
        "experienceMax": 4,
        "deadline": "2027-01-31"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1
  },
  "timestamp": "2026-03-23T14:10:00"
}
```

### Example 2
`GET /api/search/jobs?company=Beta&type=INTERNSHIP&page=0&size=5`

### Sample response 2
```json
{
  "success": true,
  "message": "Search results fetched successfully",
  "data": {
    "content": [
      {
        "id": 102,
        "title": "Spring Boot Intern - Updated",
        "company": "Beta Labs",
        "location": "Hyderabad",
        "salaryRange": "Stipend 30000/month",
        "jobType": "INTERNSHIP",
        "status": "DRAFT",
        "experienceMin": 0,
        "experienceMax": 1,
        "deadline": "2026-10-31"
      }
    ],
    "page": 0,
    "size": 5,
    "totalElements": 1,
    "totalPages": 1
  },
  "timestamp": "2026-03-23T14:11:00"
}
```

---

## 10.38 GET /api/search/suggestions
**Auth:** Public

### Example 1
`GET /api/search/suggestions?q=java`

### Sample response 1
```json
{
  "success": true,
  "message": "Suggestions fetched successfully",
  "data": ["Java Backend Developer - Updated", "Java Developer", "Java API Engineer"],
  "timestamp": "2026-03-23T14:15:00"
}
```

### Example 2
`GET /api/search/suggestions?q=spring`

### Sample response 2
```json
{
  "success": true,
  "message": "Suggestions fetched successfully",
  "data": ["Spring Boot Intern - Updated", "Spring Developer", "Spring API Engineer"],
  "timestamp": "2026-03-23T14:16:00"
}
```

---

# 11. Best end-to-end demo walkthrough

Use this exact demo in Swagger.

## Recruiter flow
1. `POST /api/users/register` with recruiter JSON
2. `POST /api/users/login`
3. Authorize using recruiter token
4. `PUT /api/users/1/profile`
5. `POST /api/jobs` create job A
6. `POST /api/jobs` create job B
7. `GET /api/jobs/recruiter/1`
8. `GET /api/jobs/101`
9. `GET /api/jobs/101/skills`

## Seeker flow
10. `POST /api/users/register` with seeker JSON
11. `POST /api/users/login`
12. Authorize using seeker token
13. `PUT /api/users/2/profile`
14. `POST /api/resumes` upload first PDF
15. `POST /api/resumes` upload second PDF
16. `PUT /api/resumes/201/primary` or `PUT /api/resumes/202/primary`
17. `GET /api/resumes/user/2`
18. `POST /api/applications` apply to job 101
19. `GET /api/applications/candidate/2`

## Recruiter review flow
20. Switch token back to recruiter
21. `GET /api/applications/job/101`
22. `PUT /api/applications/301/status` -> SHORTLISTED
23. `PUT /api/applications/301/status` -> INTERVIEW_SCHEDULED

## Seeker follow-up flow
24. Switch token back to seeker
25. `GET /api/notifications/user/2`
26. `GET /api/applications/301/history`
27. `GET /api/resumes/201/file`

## Public search flow
28. `GET /api/search/jobs?keyword=java&location=bangalore`
29. `GET /api/search/suggestions?q=java`

---

# 12. What to verify to know the app is really working

## User service checks
- duplicate email should fail
- login should fail for wrong password
- profile update should work only for correct user/admin

## Job service checks
- seeker token should fail on create/update/close/delete
- public listing should work without token
- recruiter job listing should return recruiter-owned jobs

## Resume service checks
- non-PDF upload should fail
- upload bigger than configured limit should fail
- owner should be able to download PDF
- delete should remove metadata and stored file

## Application service checks
- recruiter should not be allowed to apply
- same candidate should not apply twice to same job
- only OPEN jobs should accept applications
- candidate should see own applications
- recruiter should see applications for own job

## Notification service checks
- after job creation, recruiter should get notification
- after application submission, candidate + recruiter should get notifications
- after status update, candidate should get notification

## Search service checks
- newly created jobs should appear in search
- suggestions should return matching titles

---

# 13. Common mistakes you will hit

## 401 Unauthorized
Usually:
- token missing
- wrong `Bearer ` format
- expired token

## 403 Forbidden
Usually:
- wrong role
- seeker trying recruiter-only endpoint
- recruiter trying owner-only seeker endpoint

## 400 Bad Request
Usually:
- password does not meet pattern
- job description too short
- invalid enum value like wrong `jobType`, `status`, or `role`
- resume file is not PDF

## 404 Not Found
Usually:
- wrong id
- trying application/resume/job before creating it

## Notifications not showing up
Usually:
- RabbitMQ not started
- event did not publish
- consumer not running

## Resume download not working
Usually:
- file never uploaded
- volume issue
- trying wrong resume id

---

# 14. Final blunt truth

This README gives you:
- every endpoint covered
- two examples for every endpoint
- real application flow
- resume PDF upload usage
- service communication map

But this README does **not** magically prove every endpoint works on your machine.
The only real proof is:
1. start the project
2. use gateway Swagger
3. test in the order given above

That is how you find what is truly working and what is actually broken.
