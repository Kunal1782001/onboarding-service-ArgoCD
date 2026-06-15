# Employee Onboarding Service

Spring Boot 3.2.4 + Flowable workflow engine for managing employee onboarding processes.

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Node.js 20.18.1+ (for frontend)

## Tech Stack

- **Framework**: Spring Boot 3.2.4
- **Workflow Engine**: Flowable 7.0.1
- **ORM**: Spring Data JPA with Hibernate
- **Database**: H2 (development), Oracle (production)
- **Security**: Spring Security + JWT
- **API Documentation**: Springdoc OpenAPI (Swagger)

## Quick Start

### Backend Setup

```bash
cd backend/onboarding-service
mvn clean install
mvn spring-boot:run
```

The application will start on `http://localhost:8080/api`

### Access Points

- **API Swagger UI**: http://localhost:8080/api/swagger-ui.html
- **H2 Console**: http://localhost:8080/api/h2-console
  - JDBC URL: `jdbc:h2:mem:onboardingdb`
  - Username: `sa`
  - Password: (leave blank)

### Frontend Setup

```bash
cd frontend/onboarding-ui
npm install
ng serve
```

The frontend will run on `http://localhost:4200`

## Project Structure

```
backend/onboarding-service/
├── src/main/java/com/flowable/onboarding/
│   ├── config/          # Configuration classes
│   ├── entity/          # JPA entities
│   ├── dto/             # Data Transfer Objects
│   ├── repository/      # Data access layer
│   ├── service/         # Business logic
│   ├── controller/      # REST API endpoints
│   └── security/        # Authentication & Authorization
├── src/main/resources/
│   ├── bpmn/           # BPMN process definitions
│   ├── db/             # Database initialization scripts
│   └── application.yml # Configuration
└── pom.xml
```

## Key Features

1. **Employee Management**: CRUD operations for employee records
2. **Onboarding Workflow**: Multi-stage BPMN process
3. **Task Management**: Assignment and completion of onboarding tasks
4. **Process Monitoring**: Real-time status tracking
5. **Role-Based Access**: HR, Manager, IT, Employee roles
6. **API Documentation**: Auto-generated Swagger documentation

## Configuration

Edit `src/main/resources/application.yml` to configure:

- Database connections
- Flowable engine settings
- JWT secret key
- Server port and context path

## Database Migration (H2 → Oracle)

To switch from H2 to Oracle:

1. Add Oracle dependency to `pom.xml`
2. Update datasource configuration in `application.yml`
3. Update Hibernate dialect to `org.hibernate.dialect.OracleDialect`
4. Run migration scripts in `database/init-scripts/`

## API Endpoints

### Employee Management
- `POST /employees` - Create new employee
- `GET /employees` - List all employees
- `GET /employees/{id}` - Get employee details
- `PUT /employees/{id}` - Update employee
- `GET /employees/{id}/status` - Get onboarding status

### Process Management
- `POST /processes/deploy` - Deploy BPMN definition
- `POST /processes/start` - Start onboarding process
- `GET /processes/{processInstanceId}` - Get process status
- `GET /processes/{processInstanceId}/variables` - Get process variables

### Task Management
- `GET /tasks` - List all tasks
- `GET /tasks/{taskId}` - Get task details
- `POST /tasks/{taskId}/complete` - Complete task
- `GET /tasks/by-assignee/{username}` - Tasks for user
- `POST /tasks/{taskId}/assign` - Assign task to user

## Testing

```bash
# Run tests
mvn test

# With coverage
mvn clean test jacoco:report
```

## Docker Support

```bash
# Build and run with Docker Compose
docker-compose up -d
```

## License

Proprietary - Employee Onboarding System

## Support

For issues or questions, contact the development team.
