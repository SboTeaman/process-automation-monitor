# Documentation Index

Complete documentation for the Job Scheduling & Automation Platform.

## Guides

### [Getting Started](GETTING_STARTED.md)
Quick start guide for installation, setup, and first-time usage.
- Installation & prerequisites
- First time setup (register, login)
- Creating your first job
- Basic features overview

### [Architecture Overview](ARCHITECTURE.md)
System design and architecture documentation.
- System diagram
- Component architecture
- Data flow
- Technology stack
- Architectural decisions

### [Job Configuration Guide](JOB_CONFIGURATION.md)
Detailed guide for configuring different job types.
- Job types overview (HTTP_CALL, CSV_PROCESS, etc.)
- Configuration patterns
- Scheduling guide (cron expressions)
- Timeout and retry settings
- Advanced configuration
- Best practices

### [UI Features Guide](UI_FEATURES.md)
Complete documentation of user interface features.
- Dashboard overview
- Navigation guide
- Job creation wizard (3 steps)
- Job list and management
- Analytics and reports
- Alerts management
- Responsive design & accessibility

### [Development Guide](DEVELOPMENT.md)
For developers contributing to the project.
- Environment setup
- Project structure
- Development workflow
- Code standards & conventions
- Building & deployment
- Testing practices
- Debugging tips

### [Troubleshooting Guide](TROUBLESHOOTING.md)
Common issues and how to resolve them.
- Frontend issues
- Authentication problems
- API & data issues
- Performance issues
- Diagnostic checklist
- Getting more help

### [Contributing Guide](CONTRIBUTING.md)
How to contribute to the project.
- Getting started with development
- Code standards
- Git workflow
- Pull request process
- Code review guidelines

## Reference

### [API Reference](API_REFERENCE.md)
Complete REST API documentation.
- Authentication
- Job endpoints
- Execution endpoints
- Statistics endpoints
- Alert endpoints
- Rate limiting
- Testing examples

## Quick Links by Use Case

### I want to...

**...get started quickly**
 Start with [Getting Started](GETTING_STARTED.md)

**...understand the system**
 Read [Architecture Overview](ARCHITECTURE.md)

**...create a job**
 See [Job Configuration Guide](JOB_CONFIGURATION.md) and [Job Creation](UI_FEATURES.md#job-creation-visual-form-builder)

**...use the web interface**
 Check [UI Features Guide](UI_FEATURES.md)

**...contribute code**
 Follow [Contributing Guide](CONTRIBUTING.md) and [Development Guide](DEVELOPMENT.md)

**...integrate with the API**
 Use [API Reference](API_REFERENCE.md)

**...fix a problem**
 See [Troubleshooting Guide](TROUBLESHOOTING.md)

**...set up cron expressions**
 Read [Job Configuration > Scheduling Guide](JOB_CONFIGURATION.md#scheduling-guide)

**...understand error messages**
 Check [Troubleshooting Guide](TROUBLESHOOTING.md)

## Documentation Structure

```
docs/
 INDEX.md                    # This file
 GETTING_STARTED.md         # Quick start guide
 ARCHITECTURE.md            # System design
 JOB_CONFIGURATION.md       # Job setup guide
 UI_FEATURES.md             # Feature documentation
 DEVELOPMENT.md             # Developer guide
 TROUBLESHOOTING.md         # Common issues
 CONTRIBUTING.md            # Contribution guide
 API_REFERENCE.md           # API documentation
```

## Topics by Category

### Setup & Installation
- [Getting Started](GETTING_STARTED.md) - Installation
- [Development Guide](DEVELOPMENT.md) - Dev environment

### Features
- [Job Configuration Guide](JOB_CONFIGURATION.md) - Job setup
- [UI Features Guide](UI_FEATURES.md) - Interface guide
- [API Reference](API_REFERENCE.md) - API features

### Architecture
- [Architecture Overview](ARCHITECTURE.md) - System design
- [Development Guide](DEVELOPMENT.md) - Code structure

### Development
- [Development Guide](DEVELOPMENT.md) - Development workflow
- [Contributing Guide](CONTRIBUTING.md) - Contributing code

### Maintenance
- [Troubleshooting Guide](TROUBLESHOOTING.md) - Solving problems
- [Architecture Overview](ARCHITECTURE.md) - Scaling & deployment

## Troubleshooting Flowchart

```
Something isn't working

Check browser console (F12)
 JavaScript error? [Troubleshooting](TROUBLESHOOTING.md#frontend-issues)
 API error? [API Reference](API_REFERENCE.md)
 No error? Continue...

Check network tab (F12 Network)
 4xx error (400, 401, 403, 404)? [Troubleshooting](TROUBLESHOOTING.md#data--api-issues)
 5xx error (500)? Check backend logs
 Successful (200)? Continue...

Verify setup
 Frontend running? npm run dev
 Backend running? Check process-automation-monitor/
 Logged in? Register/Login
 Still stuck? [Troubleshooting](TROUBLESHOOTING.md)
```

## Getting Help

1. **Search documentation** - Use Ctrl+F to find topics
2. **Check Troubleshooting Guide** - Covers common issues
3. **Read relevant guide** - Based on what you're trying to do
4. **Check API Reference** - For API-related questions
5. **Create issue on GitHub** - If still stuck

## Learning Path

### Beginners
1. [Getting Started](GETTING_STARTED.md)
2. [UI Features Guide](UI_FEATURES.md)
3. [Job Configuration Guide](JOB_CONFIGURATION.md)

### Developers
1. [Development Guide](DEVELOPMENT.md)
2. [Architecture Overview](ARCHITECTURE.md)
3. [API Reference](API_REFERENCE.md)

### Contributors
1. [Contributing Guide](CONTRIBUTING.md)
2. [Development Guide](DEVELOPMENT.md)
3. [API Reference](API_REFERENCE.md)

## Document Versions

| Document | Last Updated | Status |
|----------|-------------|--------|
| Getting Started | May 2026 | Current |
| Architecture | May 2026 | Current |
| Job Configuration | May 2026 | Current |
| UI Features | May 2026 | Current |
| Development | May 2026 | Current |
| Troubleshooting | May 2026 | Current |
| Contributing | May 2026 | Current |
| API Reference | May 2026 | Current |

## Related Resources

- [Main README](../README.md) - Project overview
- [GitHub Repository](https://github.com/your-username/job-scheduler)
- [Issue Tracker](https://github.com/your-username/job-scheduler/issues)
- [Discussions](https://github.com/your-username/job-scheduler/discussions)

## Documentation Checklist

When reading documentation:
- [ ] Found what you were looking for?
- [ ] Is it clear and accurate?
- [ ] Any sections that need improvement?
- [ ] Missing documentation?

**Found an issue?** [Create an issue on GitHub](https://github.com/your-username/job-scheduler/issues/new) or contribute an update!

---

**Last Updated**: May 2026
