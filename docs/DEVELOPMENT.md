# Development Guide

## Environment Setup

### Prerequisites
- Node.js 18+
- npm 8+
- Git
- Code editor (VS Code recommended)

### Frontend Setup

```bash
# Clone repository (or navigate to existing)
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev

# Server runs on http://localhost:3000
```

### Backend Setup

Refer to `process-automation-monitor/` documentation.

Ensure backend is running on `http://localhost:8080` before starting frontend.

## Project Structure

```
frontend/
 src/
 pages/
 Dashboard.tsx
 Jobs.tsx
 JobCreateAdvanced.tsx    (Main feature)
 JobEdit.tsx
 Analytics.tsx
 Alerts.tsx
 ExecutionHistory.tsx
 Login.tsx
 Register.tsx
 components/
 Layout.tsx
 api/
 client.ts                (Axios config)
 store/
 authStore.ts             (Auth state)
 App.tsx                      (Router)
 main.tsx                     (Entry point)
 public/                          (Static assets)
 vite.config.ts                   (Vite config with proxy)
 tsconfig.json                    (TypeScript config)
 package.json
 README.md
```

## Development Workflow

### 1. Creating a New Feature

```bash
# Create new page component
touch src/pages/MyFeature.tsx

# Add route in App.tsx
<Route path="/my-feature" element={<MyFeature />} />

# Use API client for backend calls
import client from '../api/client'

async function fetchData() {
  const response = await client.get('/endpoint')
  return response.data
}
```

### 2. Styling with Tailwind CSS

All components use Tailwind CSS for styling.

```tsx
function MyComponent() {
  return (
    <div className="p-8 bg-white rounded-lg shadow">
      <h1 className="text-2xl font-bold text-gray-900">Title</h1>
      <p className="text-gray-600 mt-2">Description</p>
    </div>
  )
}
```

### 3. Form Handling

Example with useState:

```tsx
import { useState } from 'react'

function MyForm() {
  const [formData, setFormData] = useState({
    name: '',
    email: '',
  })

  const handleChange = (e) => {
    const { name, value } = e.target
    setFormData(prev => ({ ...prev, [name]: value }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    try {
      await client.post('/endpoint', formData)
    } catch (error) {
      console.error('Error:', error)
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <input
        name="name"
        value={formData.name}
        onChange={handleChange}
      />
      <button type="submit">Submit</button>
    </form>
  )
}
```

### 4. Using React Router

```tsx
import { useNavigate, useParams } from 'react-router-dom'

function MyPage() {
  const navigate = useNavigate()
  const { id } = useParams()

  return (
    <button onClick={() => navigate('/jobs')}>
      Go to Jobs
    </button>
  )
}
```

## Key Development Concepts

### State Management

Using React hooks (no Redux needed):
- `useState` - Local component state
- `useEffect` - Side effects
- Custom hooks for shared logic

Example:
```tsx
const [jobs, setJobs] = useState([])
const [loading, setLoading] = useState(true)

useEffect(() => {
  fetchJobs()
}, [])

async function fetchJobs() {
  setLoading(true)
  try {
    const res = await client.get('/jobs')
    setJobs(res.data)
  } finally {
    setLoading(false)
  }
}
```

### API Integration

Using Axios client with proxy:

```tsx
// All requests use relative URLs
import client from '../api/client'

// GET request
const data = await client.get('/jobs')

// POST request
const result = await client.post('/jobs', payload)

// Error handling
try {
  await client.post('/jobs', data)
} catch (error) {
  if (error.response?.status === 400) {
    // Handle validation error
  }
}
```

### Proxy Configuration

Vite proxy routes frontend calls to backend:

```typescript
// vite.config.ts
proxy: {
  '/jobs': {
    target: 'http://localhost:8080',
    changeOrigin: true,
  },
  '/stats': {
    target: 'http://localhost:8080',
    changeOrigin: true,
  },
  // etc...
}
```

This allows:
- Frontend on 3000
- Backend on 8080
- No CORS issues
- Transparent routing

## Building & Bundling

### Development Build
```bash
npm run dev
# Starts dev server with hot reload
```

### Production Build
```bash
npm run build
# Output: dist/
```

### Preview Production Build
```bash
npm run preview
# Test production build locally
```

## Code Quality

### TypeScript

All code should be TypeScript. Key patterns:

```tsx
// Type function parameters
function fetchData(id: string): Promise<Data> {
  return client.get(`/data/${id}`)
}

// Type component props
interface MyComponentProps {
  title: string
  onClick: (id: string) => void
  isLoading?: boolean
}

function MyComponent({ title, onClick, isLoading }: MyComponentProps) {
  return <div>{title}</div>
}
```

### Naming Conventions

- **Components**: PascalCase (`MyComponent.tsx`)
- **Files**: Match component name
- **Variables**: camelCase (`myVariable`)
- **Constants**: UPPER_SNAKE_CASE (`MAX_RETRIES`)
- **CSS Classes**: kebab-case (via Tailwind)

### Code Organization

- One component per file
- Keep files under 300 lines
- Extract reusable logic to hooks
- Group related functionality together

## Debugging

### Browser DevTools
- **F12** - Open DevTools
- **Console** - See logs and errors
- **Network** - Check API calls
- **React DevTools** - Inspect components

### Common Issues

**CORS Errors in Frontend**:
- Ensure backend is running
- Check Vite proxy config
- Verify API_BASE_URL is empty string

**API Calls Not Working**:
- Check Network tab in DevTools
- Verify endpoint path is correct
- Check authentication token
- See backend logs

**State Not Updating**:
- Check useState setter is called
- Ensure useEffect dependencies are correct
- Verify no accidental state mutations

## Testing

### Unit Tests (Not Implemented Yet)
```bash
npm run test
```

Future implementation:
- Component unit tests with Vitest
- Integration tests with Testing Library
- E2E tests with Playwright

### Manual Testing Checklist

Before submitting PR:
- [ ] All pages load without errors
- [ ] Forms validate correctly
- [ ] API calls work (backend must be running)
- [ ] Mobile responsive (test at 320px, 768px, 1200px)
- [ ] Keyboard navigation works
- [ ] Error handling is graceful
- [ ] Loading states are shown
- [ ] No console errors or warnings

## Performance Optimization

### Code Splitting
React Router enables automatic route-based code splitting:
```tsx
// Lazy load components
const Analytics = lazy(() => import('./pages/Analytics'))
```

### Image Optimization
- Use appropriate image formats
- Lazy load images
- Optimize before adding to repo

### Bundle Analysis
```bash
npm run build -- --analyze
```

## Git Workflow

### Branch Naming
- `feature/feature-name` - New feature
- `bugfix/bug-name` - Bug fix
- `docs/doc-name` - Documentation

### Commit Messages
```
feat: add visual form builder
fix: resolve CORS issues with proxy
docs: update API documentation
```

### Pull Request Process
1. Create feature branch
2. Make changes with clear commits
3. Test thoroughly
4. Create PR with description
5. Address review comments
6. Merge when approved

## Useful Commands

```bash
# Development
npm run dev              # Start dev server
npm run build            # Production build
npm run preview          # Preview build

# Code Quality
npm run lint             # Check code style (if configured)
npm run format           # Format code (if configured)

# Dependencies
npm list                 # Show all dependencies
npm outdated             # Check for outdated packages
npm update               # Update dependencies
npm audit                # Check for vulnerabilities
```

## Environment Variables

### Development (.env.local)
```
VITE_API_URL=
```

Leave empty to use Vite proxy.

### Production
Set real API URL:
```
VITE_API_URL=https://api.example.com
```

## Troubleshooting Development

### Port 3000 Already in Use
```bash
# Windows (PowerShell)
Get-Process node -ErrorAction SilentlyContinue | Stop-Process -Force

# Change port in vite.config.ts
server: {
  port: 3001
}
```

### Node Modules Issues
```bash
# Clear cache and reinstall
rm -rf node_modules package-lock.json
npm install
```

### Hot Reload Not Working
- Check Vite version is latest
- Restart dev server
- Clear browser cache

### Backend Connection Issues
- Verify backend is running on 8080
- Check proxy config in vite.config.ts
- Look at network calls in DevTools

## Resources

- [Vite Documentation](https://vitejs.dev)
- [React Documentation](https://react.dev)
- [TypeScript Handbook](https://www.typescriptlang.org/docs/)
- [Tailwind CSS Docs](https://tailwindcss.com/docs)
- [React Router Docs](https://reactrouter.com)

## Getting Help

- Check existing code for examples
- Review documentation in `/docs`
- Search GitHub issues
- Ask in team chat/discussions
