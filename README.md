# Library Management System 📚

A full-stack containerized web application that automates the workflow of a physical library. Built with Spring Boot and Angular, this system enables users to create accounts, browse the library catalog, and manage book borrowing and returns, while administrators can efficiently manage both books and users through a dedicated interface.

> **Project Journey**: Started as a backend-only REST API project to strengthen Spring Boot skills, evolved into a complete full-stack containerized application. This project represents my transition from backend to full-stack developer, incorporating Angular, PostgreSQL, Docker, and industry best practices.

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-19-red.svg)](https://angular.io/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Latest-blue.svg)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED.svg)](https://www.docker.com/)

## ✨ Features

### User Features
- **Account Management**: User registration and JWT-based authentication
- **Book Catalog Browsing**: Search and filter through the library's collection
- **Advanced Search**: Filter books by title, author, and genre
- **Borrowing System**: Borrow and return books with automatic availability tracking
- **My Books Dashboard**: Personal view of currently borrowed books and borrowing history
- **Due Date Tracking**: Monitor borrowed books and return deadlines

### Admin Features
- **Book Management**: Full CRUD operations for library catalog
- **User Management**: Administer user accounts and permissions
- **Role-Based Access Control**: Secure endpoints with ADMIN and MEMBER roles
- **Inventory Oversight**: Monitor book availability and borrowing status

## 🏗️ Architecture

### Backend Architecture
- **Framework**: Spring Boot 3.5 with Java 17
- **API Design**: RESTful API with JWT authentication
- **Database**: PostgreSQL with Flyway migrations for version control
- **Architecture Patterns**:
  - Repository Pattern for data access layer
  - Service Layer architecture for business logic
  - DTO Pattern for clean data transfer
  - Global Exception Handler for consistent error responses

### Frontend Architecture
- **Framework**: Angular v19 with TypeScript
- **UI Library**: Angular Material for consistent design
- **State Management**: Component-based state with services
- **Architecture**: Feature-based module organization

### Deployment
- **Containerization**: Docker with multi-container orchestration
- **Services**: 3 containers (Backend, Frontend, PostgreSQL)
- **Orchestration**: Docker Compose for seamless local deployment

## 🛠️ Tech Stack

### Backend
- Java 17
- Spring Boot 3.5
- Spring Data JPA
- Spring Security with JWT
- PostgreSQL
- Flyway (Database Migrations)
- Maven

### Frontend
- Angular 19
- TypeScript
- Angular Material
- RxJS

### DevOps & Tools
- Docker & Docker Compose
- Git & GitHub

### Testing
- **Backend**: Unit tests, Slice tests, Integration tests
- **Frontend**: Unit tests with Jasmine/Karma

## 🚀 Quick Start

### Prerequisites
- [Docker](https://www.docker.com/get-started) (20.10+)
- [Docker Compose](https://docs.docker.com/compose/install/) (1.29+)

### Running the Application

1. **Clone the repository**
   ```bash
   git clone https://github.com/MPfria02/Library_Management_System.git
   cd Library_Management_System
   ```

2. **Start all services with Docker Compose**
   ```bash
   docker-compose up --build
   ```

3. **Access the application**
   - Frontend: http://localhost:4200
   - Backend API: http://localhost:8080/api

4. **Default Admin Credentials** (if seeded)
   ```
   Email: admin@library.com
   Password: admin123
   ```

5. **Stop the application**
   ```bash
   docker-compose down
   ```

That's it! Docker handles all dependencies, database setup, and service orchestration.

## 💻 Development Setup

For developers who want to modify the code or contribute:

### Prerequisites
- Java 17 ([Download](https://www.oracle.com/java/technologies/downloads/#java17))
- Maven 3.8+ ([Download](https://maven.apache.org/download.cgi))
- Node.js 18.20.8+ and npm ([Download](https://nodejs.org/))
- PostgreSQL 14+ (or use Docker for database only)
- Docker & Docker Compose

### Backend Development

1. **Navigate to backend directory**
   ```bash
   cd backend
   ```

2. **Configure database** (if not using Docker)
   Update `src/main/resources/application-dev.yml` with your PostgreSQL credentials

3. **Run migrations**
   ```bash
   mvn flyway:migrate
   ```

4. **Build and run**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

5. **Run tests**
   ```bash
   mvn test
   ```

### Frontend Development

1. **Navigate to frontend directory**
   ```bash
   cd frontend
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Start development server**
   ```bash
   ng serve
   ```
   Access at http://localhost:4200

4. **Run tests**
   ```bash
   npm test
   ```

5. **Build for production**
   ```bash
   ng build --configuration production
   ```

## 📁 Project Structure

```
Library_Management_System/
├── backend/                     # Spring Boot backend service
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/.../librarymanager/backend/
│   │   │   │   ├── config/              # Spring configuration & Security
│   │   │   │   ├── controller/          # REST controllers
│   │   │   │   ├── dto/                 # Data Transfer Objects
│   │   │   │   ├── entity/              # JPA entities
│   │   │   │   ├── exception/           # Global exception handlers
│   │   │   │   ├── mapper/              # Entity-DTO mappers
│   │   │   │   ├── repository/          # Spring Data repositories
│   │   │   │   ├── security/            # JWT configuration
│   │   │   │   └── service/             # Business logic layer
│   │   │   └── resources/
│   │   │       ├── application.yml      # Application configuration
│   │   │       └── db/migration/        # Flyway migration scripts
│   │   └── test/                        # Unit, slice, and integration tests
│   ├── Dockerfile
│   └── pom.xml
│
├── frontend/                    # Angular frontend application
│   ├── src/
│   │   ├── app/
│   │   │   ├── features/
│   │   │   │   ├── books/              # Book management module
│   │   │   │   ├── borrows/            # Borrowing functionality
│   │   │   │   └── users/              # User management
│   │   │   ├── auth/                   # Authentication & guards
│   │   │   ├── shared/                 # Shared models & services
│   │   │   └── core/                   # Core functionality
│   │   └── assets/                     # Static assets & icons
│   ├── Dockerfile
│   ├── angular.json
│   └── package.json
│
└── docker-compose.yml           # Multi-container orchestration
```

## 📚 API Documentation

The API follows RESTful principles with JWT authentication. Key endpoints include:

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Authenticate and receive JWT token

### Books
- `GET /api/books` - Get all books (with search & pagination)
- `GET /api/books/{id}` - Get book details
- `POST /api/books` - Create book (Admin only)
- `PUT /api/books/{id}` - Update book (Admin only)
- `DELETE /api/books/{id}` - Delete book (Admin only)

### Inventory
- `POST /api/inventory/books/{bookId}/borrow` - Borrow a book
- `POST /api/inventory/books/{bookId}/return` - Return a book
- `GET /api/inventory/books/{bookId}/status` - Check book status
- `GET /api/inventory/books`                 - Get user's borrow records

### Users (Admin Only)
- `GET /api/users` - Get all users
- `PUT /api/users/{id}` - Update user

## 🗄️ Database

### Schema Overview
The application uses PostgreSQL with the following main entities:
- **Users**: Authentication and profile information
- **Books**: Library catalog with availability tracking
- **BorrowRecords**: Borrowing history and current loans

### Migrations
Database schema is managed with Flyway migrations, ensuring version-controlled, reproducible database changes across environments.

Migration files location: `backend/src/main/resources/db/migration/`

## 🧪 Testing

### Backend Testing Strategy
- **Unit Tests**: Service layer business logic
- **Slice Tests**: Repository layer with test database
- **Integration Tests**: End-to-end API testing

Run all backend tests:
```bash
cd backend
mvn test
```

### Frontend Testing
- **Unit Tests**: Component and service testing with Jasmine/Karma

Run frontend tests:
```bash
cd frontend
npm test
```

## 🎯 What I Learned

This project was a comprehensive learning journey that transformed my skill set:

### Technical Growth
- **Full-Stack Development**: Transitioned from backend-only to building complete full-stack applications
- **Angular Framework**: First time incorporating Angular into my developer toolkit
- **Database Expertise**: Gained hands-on experience with PostgreSQL and complex queries
- **DevOps**: First containerized application using Docker and Docker Compose
- **Database Migrations**: Implemented Flyway for professional database version control
- **Software Development Lifecycle**: Practiced planning, documentation, and iterative development

### Best Practices Implemented
- Clean architecture with separation of concerns
- RESTful API design principles
- JWT-based authentication and authorization
- Frontend component architecture and reusability
- Comprehensive testing strategy
- Version control with meaningful commits

### Challenges Overcome
- Integrating JWT authentication across frontend and backend
- Managing state in Angular components
- Designing a normalized database schema
- Containerizing multi-service applications
- Implementing role-based access control

## 🚧 Future Enhancements

- [ ] Deploy to cloud platform (AWS/Azure/GCP)
- [ ] Implement email notifications for due dates and overdue books
- [ ] Add book reservation system
- [ ] Implement borrowing statistics and analytics dashboard
- [ ] Add book cover image uploads
- [ ] Implement fine calculation for overdue books
- [ ] Add book review and rating system
- [ ] Implement advanced admin reporting features
- [ ] Add CI/CD pipeline with automated testing
- [ ] Implement real-time notifications with WebSockets

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Feel free to check the [issues page](https://github.com/MPfria02/Library_Management_System/issues).

### How to Contribute
1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👤 Author

**Marcel Pulido**

- GitHub: [@MPfria02](https://github.com/MPfria02)
- LinkedIn: [Marcel Pulido](https://www.linkedin.com/in/marcel-pulido)

## 🙏 Acknowledgments

- Spring Boot and Angular communities for excellent documentation
- Docker for simplifying deployment
- All open-source contributors whose libraries made this project possible

---

⭐️ If you found this project helpful or interesting, please consider giving it a star!

**Built with ❤️ as a learning journey from backend specialist to full-stack developer**
