# Cloud Storage Backend API

A secure RESTful backend service for cloud file storage built with
Spring Boot. The application supports user authentication, folder
management, file upload/download, and public file sharing.

Designed as a production-style backend project with Docker support,
database migrations, and automated tests.

------------------------------------------------------------------------

## 🚀 Features

-   JWT-based authentication (stateless)
-   User registration & login
-   Create, rename, move and delete folders
-   Upload and download files
-   Public file sharing via token
-   PostgreSQL database
-   Flyway database migrations
-   Unit tests (JUnit + Mockito)
-   API tests (MockMvc)
-   Fully Dockerized

------------------------------------------------------------------------

## 🛠 Tech Stack

-   Java 21
-   Spring Boot 3
-   Spring Security (JWT)
-   Spring Data JPA
-   PostgreSQL
-   Flyway
-   Docker & Docker Compose
-   JUnit 5 / Mockito / MockMvc

------------------------------------------------------------------------

## 🏗 Architecture

Controller → Service → Repository → Database

Security: - Stateless JWT authentication - Custom authentication
filter - Centralized exception handling

File Storage: - File metadata stored in PostgreSQL - Files stored on
disk - Access control based on authenticated user

------------------------------------------------------------------------

## 🐳 Running the Application (Docker)

Build and start:

docker compose --env-file .env.prod up --build -d

Application URL:

http://localhost:8080

------------------------------------------------------------------------

## 🔐 Authentication Flow

1.  Register a user
2.  Login to receive a JWT access token
3.  Send token in the Authorization header:

Authorization: Bearer `<your_token>`

------------------------------------------------------------------------

## 📡 API Usage Examples

### Register

curl -X POST http://localhost:8080/api/users/register\
-H "Content-Type: application/json"\
-d '{ "username": "user1", "password": "password123" }'

------------------------------------------------------------------------

### Login

curl -X POST http://localhost:8080/api/users/login\
-H "Content-Type: application/json"\
-d '{ "username": "user1", "password": "password123" }'

------------------------------------------------------------------------

### Create Folder

curl -X POST http://localhost:8080/api/folders\
-H "Authorization: Bearer YOUR_TOKEN"\
-H "Content-Type: application/json"\
-d '{ "name": "documents" }'

------------------------------------------------------------------------

### Upload File

curl -X POST http://localhost:8080/api/files\
-H "Authorization: Bearer YOUR_TOKEN"\
-F "file=@example.txt"\
-F "folderId=1"

------------------------------------------------------------------------

## 🧪 Running Tests

./mvnw test

------------------------------------------------------------------------

## 📦 Environment Variables (.env.prod)

POSTGRES_DB=, POSTGRES_USER=, POSTGRES_PASSWORD=, POSTGRES_PORT=,  POSTGRES_VERSION=, DB_PORT=, DB_HOST=, SERVER_PORT=, STORAGE_ROOT_DIR=, JWT_SECRET=, JWT_EXP_MINUTES=, JWT_ISSUER=

------------------------------------------------------------------------

## 🗄 Database Migrations

Location:

src/main/resources/db/migration

------------------------------------------------------------------------

## 📌 Future Improvements

-   Refresh token support
-   Rate limiting
-   Cloud storage integration (AWS S3)
-   Swagger / OpenAPI documentation

------------------------------------------------------------------------

## 👨‍💻 Author

Kalabay Aslanbek