# 🚀 SystemForge

SystemForge is an enterprise-grade backend system design builder platform. Instead of guessing architecture configurations, developers and backend engineers can use SystemForge to design, customize, and generate production-ready backend system design blueprints.

SystemForge acts as a **System Design Intelligence Engine**, recommending the best architecture, database choices, authentication strategies, and caching solutions based on the application type, scale, and feature requirements.

---

## 🧠 Core Features

1. **System Builder**
   - Select and configure core backend systems (Authentication, Payments, Notifications, Databases, etc.).
   - Tailor configuration properties (e.g., OTP login, JWT refresh token duration, provider integration).

2. **Recommendation Engine**
   - Intelligently suggests architecture styles (Modular Monolith vs. Microservices), caching tiers, message queues, and databases.
   - Tailored recommendations based on scale (Small, Medium, Large) and app profile (e.g., Ride-Hailing, SaaS, E-Commerce).

3. **Output Generator**
   - Generates API structures, database schemas (MySQL/PostgreSQL), and tech stacks.
   - Provides clear, production-ready design documentation and architectural explanation blueprints.

4. **Template System**
   - Predefined industry-standard templates (e.g., OTP Authentication for Ride-Hailing Apps, JWT Authentication for SaaS platforms).

---

## 🧱 Technology Stack

### Backend
- **Core**: Java 21 & Spring Boot 3.x
- **Build Tool**: Maven
- **Database**: MySQL (Primary Database), Redis (Planned for caching)
- **Security**: Stateless JWT (Access & Refresh tokens), secure SecureRandom OTP authentication, token hashing (SHA-256)
- **Architecture**: Modular Monolith design (engineered for a seamless future migration to Microservices)

### Frontend
- **Framework**: Next.js (React)
- **Styling**: Tailwind CSS & custom design components
- **State Management**: React Hooks & Server-Sent Events (SSE) for real-time progress streaming during architecture generation

---

## 🏗️ Project Architecture & Structure

SystemForge is split into two primary folders:

```
SystemForge/
├── systemforge-backend/   # Spring Boot modular monolith backend
└── systemforge-frontend/  # Next.js frontend application
```

### Backend Module Structure
The backend is built as a Modular Monolith where modules are isolated and communicate through clean service layers, preventing direct cross-repository access.
- **Auth Module**: Handles secure authentication, email/password log-ins, OTPs, JWT issuance, and refresh token rotation.
- **User Module**: Handles user profiles, roles (Developer, Admin), and soft deletion.
- **Recommendation Module**: The "brain" of the platform that executes plug-and-play rules to suggest system designs.
- **Notification Module**: Responsible for sending notifications (e.g., OTP verification emails).
- **Common Module**: Houses global utilities, custom exceptions, and standard API responses.

---

## 🚀 Getting Started

### Prerequisites
- **Java**: JDK 21
- **Node.js**: v18.x or higher
- **Docker & Docker Compose**: Installed and running (for databases/containers)

### 1. Running the Backend
1. Navigate to the backend directory:
   ```bash
   cd systemforge-backend
   ```
2. Configure your environment variables in `.env` (refer to `.env.example`).
3. Start dependencies (MySQL) via Docker Compose:
   ```bash
   docker-compose up -d
   ```
4. Run the Spring Boot application:
   ```bash
   mvnw spring-boot:run
   ```

### 2. Running the Frontend
1. Navigate to the frontend directory:
   ```bash
   cd systemforge-frontend
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Configure the environment variables in `.env.local` (refer to `.env.example`).
4. Run the development server:
   ```bash
   npm run dev
   ```
5. Open [http://localhost:3000](http://localhost:3000) in your browser.

---

## 🔮 Future Enhancements & Roadmap
- **AI-Powered Suggestions**: Deep learning-based architecture recommendations.
- **Auto Code Generation**: Instant generation of bootable Spring Boot starter code based on your custom blueprint.
- **Diagram Generator**: Automatic interactive architectural flow diagrams.
- **Template Marketplace**: Share, sell, or buy architectural blueprints.
