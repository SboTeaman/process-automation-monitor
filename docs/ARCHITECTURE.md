# Architecture Overview

## System Design

```

                    User Browser
                 http://localhost:3000


 HTTP Requests
 (via Vite Proxy)
¼

                   Frontend (React)
         - TypeScript + Vite + Tailwind CSS
         - Responsive UI for all pages
         - Real-time form validation


 REST API
 Port 3000 8080 (Vite Proxy)

¼

                Backend API (Java/Spring)
          http://localhost:8080/api
         - Authentication & Authorization
         - Job Management
         - Job Execution
         - Statistics & Analytics


¼

   Persistence Layer
 - Database
 - File Storage
 - Cron Scheduler

```

## Component Architecture

### Frontend (React)

```
App.tsx (Router)
 Pages/
 Dashboard.tsx      (Home page with stats)
 Login.tsx          (Authentication)
 Register.tsx       (User registration)
 Jobs.tsx           (Job list)
 JobCreateAdvanced.tsx  (Visual form builder)
 Step 1: Basic Info
 Step 2: Configuration (visual + preview)
 Step 3: Review & JSON Editor
 JobEdit.tsx        (Modify job)
 Analytics.tsx      (Performance metrics)
 Alerts.tsx         (Alert management)
 ExecutionHistory.tsx (Job run history)
 Components/
 Layout.tsx         (Navigation & sidebar)
 API/
 client.ts          (Axios configuration)
```

### Backend (process-automation-monitor)

See `process-automation-monitor/` for detailed backend architecture.

## Data Flow

### Creating a Job

```
User Input (Form)

Step 1: Basic Info (name, type, schedule)

Step 2: Configuration (type-specific fields)

Visual Form JSON Conversion

Step 3: Review with JSON Preview

User clicks "Create Job"

POST /jobs {basicInfo, config}

Backend Validation & Storage

Job Created
```

### Executing a Job

```
User clicks "Run Now" (or scheduled time)

Backend Job Scheduler

Execute Job Logic (HTTP call, CSV process, etc.)

Store Execution Result

Update Job Statistics

User sees result in UI
```

## Technology Stack

### Frontend
- **React 18** - UI framework
- **TypeScript** - Type safety
- **Vite** - Build tool & dev server
- **Tailwind CSS** - Styling
- **Lucide Icons** - Icon library
- **React Router v6** - Routing
- **Axios** - HTTP client
- **Recharts** - Charts & graphs

### Backend
- **Java/Spring Boot** - Main framework
- **Spring Data JPA** - Database abstraction
- **Spring Security** - Authentication
- **RESTful API** - API design
- **Quartz Scheduler** - Job scheduling
- **PostgreSQL/MySQL** - Database

### Development & DevOps
- **Git** - Version control
- **npm** - Package management
- **Vite Proxy** - Dev server proxy (eliminates CORS)

## Key Architectural Decisions

### 1. Visual Form Builder (Approach 3)
**Decision**: Step-by-step wizard with live JSON preview
**Why**: 
- 80% of users don't want to touch JSON
- Advanced users can still edit JSON directly
- Better UX and fewer mistakes

### 2. Vite Proxy for CORS
**Decision**: Use Vite dev server proxy instead of backend CORS
**Why**:
- Eliminates CORS complexity in development
- Production uses direct API calls (no proxy needed)
- Easier to develop without backend config changes

### 3. Relative URLs for API
**Decision**: Frontend uses relative URLs (`/jobs`, `/stats`)
**Why**:
- Works seamlessly with Vite proxy
- Flexible for different environments
- Easy to configure for production

### 4. Type-Safe API Communication
**Decision**: TypeScript interfaces for all API responses
**Why**:
- Catch errors at compile time
- Better IDE autocomplete
- Self-documenting code

## Performance Considerations

- **Code Splitting**: React Router enables route-based code splitting
- **Asset Optimization**: Vite provides automatic asset optimization
- **State Management**: Using React hooks (no Redux needed)
- **API Caching**: Axios configuration for efficient requests

## Security

- **Authentication**: Backend handles JWT tokens
- **Authorization**: Role-based access control
- **Input Validation**: Frontend validation + backend validation
- **HTTPS**: Recommended for production
- **Secrets**: Environment variables for sensitive data

## Scalability

- **Frontend**: Static assets can be cached/CDN'd
- **Backend**: Stateless design allows horizontal scaling
- **Database**: Standard relational database scaling
- **Job Execution**: Queue-based system for parallel job processing

## Deployment

### Frontend
- Build: `npm run build`
- Output: `frontend/dist/`
- Deploy to: Static hosting (Vercel, Netlify, AWS S3, etc.)

### Backend
- See `process-automation-monitor/` for deployment guide
- Typically deployed to: Docker, VPS, or cloud platform

### Environment Configuration
Production uses different configuration than development:
- `API_URL` points to real backend domain
- Database connection strings
- JWT secret keys
- CORS origins

## Future Enhancements

- [ ] Real-time job execution notifications
- [ ] Advanced job scheduling (business hours only, etc.)
- [ ] Job dependencies and workflows
- [ ] Multi-user collaboration
- [ ] API tokens for external integrations
- [ ] Webhook support for job triggers
- [ ] Custom job type plugins
