# ZeroMonos

A full-stack web application for managing bulky waste collection requests. Built with Java Spring Boot backend and Thymeleaf frontend.

## Overview

ZeroMonos is a platform that connects citizens who need to dispose of bulky waste with municipal staff who manage collection requests. Citizens can create booking requests, track their status using a token, and cancel requests if needed. Staff can view, filter, and manage all bookings across municipalities.

## Features

- **Citizen Interface**: Create booking requests specifying name, municipality, date, and item description
- **Request Tracking**: Citizens receive a unique token to check booking status and cancel requests
- **Staff Dashboard**: View, update, and delete bookings with filtering by municipality
- **External API Integration**: Dynamically fetches Portuguese municipalities from geoapi.pt
- **Database Persistence**: PostgreSQL database for reliable data storage
- **REST API**: Full API endpoints for external integrations
- **Continuous Integration**: GitHub Actions pipeline with automated testing and SonarQube analysis

## Getting Started

### Prerequisites

- Java JDK 11 or higher
- PostgreSQL database
- Node.js (for frontend dependencies)

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/duartebranco/ZeroMonos.git
   cd ZeroMonos
   ```

2. Set up PostgreSQL database and configure connection in `application.properties`

3. Build and run:
   ```bash
   mvn clean spring-boot:run
   ```

4. Access the application at `http://localhost:8080`

## Project Structure

- `/src/main/java` - Spring Boot backend with REST controllers and business logic
- `/src/main/resources/templates` - Thymeleaf templates for web pages
- `/src/test` - Unit, integration, and acceptance tests
- `/docs` - Project documentation and reports

## Technology Stack

- **Backend**: Java, Spring Boot, PostgreSQL
- **Frontend**: HTML, CSS, JavaScript, Thymeleaf
- **Testing**: JUnit 5, Mockito, Selenium WebDriver, k6
- **CI/CD**: GitHub Actions, SonarQube
- **Code Coverage**: 88.3%

## API Endpoints

- `POST /bookings` - Create a new booking
- `GET /bookings/{token}` - Check booking status
- `GET /bookings` - List all bookings (staff)
- `PATCH /bookings/{id}?status={newStatus}` - Update booking status
- `DELETE /bookings/{id}` - Delete a booking
- `GET /municipalities` - Get available municipalities

## Testing

The project includes comprehensive testing:
- Unit and service tests for business logic
- Integration tests for API endpoints
- Acceptance tests using Selenium WebDriver
- Performance tests using k6
- Code quality analysis with SonarQube

## About

This project was developed for the [Software Testing and Quality Control](https://www.ua.pt/en/uc/8109) class at Universidade de Aveiro.

**Author:** Duarte Branco
