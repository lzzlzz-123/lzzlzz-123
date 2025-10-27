# Weibo-like Personal Blog System

This project implements a microblog-style personal blog platform inspired by Weibo. It is structured as a full-stack application with a Spring Boot backend, a Vue 3 frontend, MySQL for persistence, Redis for caching user timelines, and Nginx for serving the SPA and proxying API requests.

## Project Structure

```
.
├── backend/                 # Spring Boot service
│   ├── pom.xml
│   └── src/
├── frontend/                # Vue 3 + Vite single page application
│   ├── package.json
│   └── src/
├── deploy/
│   └── nginx.conf           # Nginx configuration for static caching and proxying
├── docker-compose.yml       # Local orchestration for MySQL, Redis, backend, frontend, and Nginx
└── README.md
```

## Features

### Backend (Spring Boot)
- User registration and authentication using JWT tokens
- Password hashing with BCrypt
- Follow/unfollow system
- Post creation, deletion, liking, and commenting
- Fan-out timeline cached in Redis (global feed and per-user feed)
- MySQL persistence using Spring Data JPA
- RESTful APIs under `/api/**`
- Comprehensive exception handling with consistent JSON error responses

### Frontend (Vue 3 + Vite)
- Login and registration views
- Global feed timeline with infinite scroll mock-up
- Post composer with image URL attachments
- User profile overview including follow/follower counts
- Post detail view with like and comment interactions
- Pinia-based global stores for authentication and timeline state
- Axios API client with interceptors for JWT handling

### Infrastructure
- Redis cache for timeline acceleration
- Nginx configured to cache SPA assets and proxy backend API requests
- Docker Compose for local development, wiring MySQL, Redis, backend, frontend, and Nginx together

## Getting Started

### Prerequisites
- Docker and Docker Compose (recommended for the easiest setup)
- Alternatively:
  - JDK 17+
  - Maven 3.9+
  - Node.js 18+
  - MySQL 8+
  - Redis 7+

### Environment Variables

Create a `.env` file (or provide variables through your environment) with the following values:

```
MYSQL_ROOT_PASSWORD=supersecret
MYSQL_DATABASE=weiboblog
MYSQL_USER=weiboblog
MYSQL_PASSWORD=supersecret
JWT_SECRET=change_this_secret
```

> **Note:** The backend reads `JWT_SECRET` from the environment (or defaults to a fallback value for development). Customize this for production usage.

### Running with Docker Compose

1. Ensure the `.env` file is created at the project root.
2. Run:
   ```bash
   docker-compose up --build
   ```
3. The services will be available at:
   - Backend API: http://localhost:8080
   - Frontend SPA (through Nginx): http://localhost:80
   - MySQL: localhost:3306
   - Redis: localhost:6379

### Running Manually

#### Backend
```bash
cd backend
./mvnw spring-boot:run
```

#### Frontend
```bash
cd frontend
npm install
npm run dev
```

Ensure MySQL and Redis are running and the backend's `application.yml` is populated with valid credentials.

## API Overview

| Method | Endpoint                          | Description                      |
|--------|-----------------------------------|----------------------------------|
| POST   | `/api/auth/register`              | Register a new user              |
| POST   | `/api/auth/login`                 | Authenticate and receive a token |
| GET    | `/api/users/me`                   | Retrieve the current user        |
| GET    | `/api/users/{id}`                 | Fetch a profile by ID            |
| POST   | `/api/users/{id}/follow`          | Follow a user                    |
| DELETE | `/api/users/{id}/follow`          | Unfollow a user                  |
| GET    | `/api/posts/feed`                 | Timeline feed                    |
| POST   | `/api/posts`                      | Create a new post                |
| POST   | `/api/posts/{id}/like`            | Like a post                      |
| DELETE | `/api/posts/{id}/like`            | Unlike a post                    |
| GET    | `/api/posts/{id}/comments`        | Retrieve comments for a post     |
| POST   | `/api/posts/{id}/comments`        | Create a comment                 |

Detailed request/response payloads are defined through DTOs and can be explored via the OpenAPI documentation (available at `/swagger-ui.html` when the backend is running).

## Database Schema Overview

- `users`: Base account information
- `posts`: Microblog posts authored by users
- `post_media`: Optional image URL attachments for posts
- `comments`: Comments linked to posts and authors
- `post_likes`: User likes on posts
- `follows`: Follower/followee relationships

## Development Notes

- All timestamps are stored as UTC `OffsetDateTime`
- Redis keys follow the pattern `timeline:global` and `timeline:user:{userId}`
- The backend uses `springdoc-openapi` for Swagger UI documentation
- The frontend is TypeScript-first with strict typings enabled

## Production Considerations

- Replace the sample JWT secret with one managed by a secrets manager
- Configure HTTPS for Nginx and tune caching headers to your requirements
- Add background workers or message queues for heavier fan-out workloads
- Implement rate limiting and auditing as needed

## License

This project is provided as-is for demonstration and educational purposes.
