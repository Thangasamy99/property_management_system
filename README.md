# PCC Cameroon — Property & Land Management System

Enterprise internal platform for PCC Cameroon to manage properties, buildings, apartments and land assets from one centralized application.

## Structure

```
frontend/                        React 19 + Vite + Tailwind CSS admin panel & login
backend/pcc-identity-service/    Spring Boot 4.1 microservice — auth, users, roles,
                                 permissions, audit logs (local port 9002, MySQL)
```

## Tech Stack

- **Frontend**: React 19, Vite, Tailwind CSS v4, React Router, lucide-react
- **Backend**: Java 21, Spring Boot, Spring Security (JWT + refresh rotation), Spring Data JPA, Flyway, MySQL 8, BCrypt
- **Docs**: Swagger UI at `http://localhost:9002/swagger-ui.html`

## Quick Start

### Backend

```bash
cd backend/pcc-identity-service
export DB_PASSWORD="your-mysql-password"
export JWT_SECRET="a-64-plus-character-random-secret"
./mvnw spring-boot:run          # or: java -jar target/pcc-identity-service-0.0.1-SNAPSHOT.jar
```

First startup creates `pcc_identity_db` tables via Flyway and bootstraps an admin account (see `backend/pcc-identity-service/README.md`).

### Frontend

```bash
cd frontend
npm install
npm run dev                     # http://localhost:3000
```

## Roles

Admin · Manager · Accountant · Property Officer · Land Officer — database-driven permissions enforced server-side.

See `backend/pcc-identity-service/README.md` for full API docs, environment variables and the ER diagram.
