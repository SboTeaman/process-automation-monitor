# Troubleshooting Guide

## Common Issues & Solutions

### Frontend Issues

#### 1. Port 3000 Already in Use

**Error**: `Port 3000 is already in use`

**Solutions**:
```bash
# Option 1: Kill existing Node processes
# Windows (PowerShell)
Get-Process node -ErrorAction SilentlyContinue | Stop-Process -Force

# Option 2: Use different port in vite.config.ts
server: {
  port: 3001
}
```

#### 2. Module Not Found Errors

**Error**: `Cannot find module 'xyz'`

**Solutions**:
```bash
# Reinstall dependencies
rm -rf node_modules package-lock.json
npm install

# Clear Vite cache
rm -rf node_modules/.vite
npm run dev
```

#### 3. CORS Errors

**Error**: `Access to XMLHttpRequest blocked by CORS policy`

**Causes & Solutions**:
- Backend not running on 8080
  - Start backend: Check `process-automation-monitor/` docs
- Vite proxy not configured
  - Check `frontend/vite.config.ts` has proxy rules
- API client using absolute URL
  - Ensure `API_BASE_URL = ''` in `frontend/src/api/client.ts`

**Check**:
```bash
# Verify backend is running
curl http://localhost:8080/jobs
# Should return data or 401 (not CORS error)
```

#### 4. Hot Reload Not Working

**Issue**: Changes don't appear in browser

**Solutions**:
- Hard refresh browser (Ctrl+Shift+R or Cmd+Shift+R)
- Restart dev server
- Check no syntax errors in changed file
- Clear browser cache

#### 5. Blank Page or 404

**Issue**: Page shows nothing or 404 error

**Solutions**:
- Check console for errors (F12)
- Verify route exists in `App.tsx`
- Ensure backend is running
- Check network requests in DevTools

### Authentication Issues

#### 1. Login Fails

**Error**: Invalid credentials or network error

**Solutions**:
- Verify backend is running
- Check email/password are correct
- Ensure account is created (try Register first)
- Check network tab for API errors (F12)

#### 2. 403 Forbidden After Login

**Error**: API returns 403 status

**This is expected behavior**:
- User needs to be logged in
- After login, 403 should resolve to 200 OK
- Check you're actually logged in (check localStorage)

**Debug**:
```javascript
// In browser console
localStorage.getItem('accessToken')
// Should show token, not null
```

#### 3. Session Expires Too Quickly

**Issue**: Getting logged out after short time

**Solutions**:
- Check backend token TTL (time-to-live)
- Ensure refresh token endpoint works
- Clear browser storage and login again

### Data & API Issues

#### 1. Jobs List is Empty

**Issue**: No jobs shown even after creating one

**Solutions**:
- Refresh page (F5)
- Check you're logged in
- Verify job was created successfully
- Check network tab for API errors

**Debug**:
```javascript
// In browser console, Network tab
// Check GET /jobs response
// Should show array of jobs
```

#### 2. API Returns 404

**Error**: Endpoint not found

**Causes**:
- Endpoint path is wrong
- Backend doesn't have this endpoint
- Typo in API call

**Solutions**:
- Check path matches backend API
- Verify backend has endpoint
- Look at backend documentation

#### 3. Job Won't Create

**Error**: Submit button doesn't work or shows error

**Solutions**:
- Check all required fields are filled
- Check for validation errors in form
- Look at console for error details (F12)
- Check network tab for API error response
- Try editing JSON directly

#### 4. Timeout Errors When Creating Job

**Error**: "Request timeout" or request hangs

**Solutions**:
- Check backend is responding (open http://localhost:8080)
- Increase request timeout in client.ts
- Check network tab to see request is being made
- Verify backend isn't overloaded

### UI & Display Issues

#### 1. Styling Looks Wrong

**Issue**: CSS not applied or broken layout

**Solutions**:
- Hard refresh browser (Ctrl+Shift+R)
- Clear browser cache
- Check Tailwind CSS is loaded (check Network tab)
- Restart dev server

#### 2. Icons Not Showing

**Issue**: Lucide icons appear as empty boxes

**Solutions**:
- Restart dev server
- Ensure lucide-react is installed: `npm list lucide-react`
- Reinstall if needed: `npm install lucide-react`

#### 3. Form Fields Disabled/Grayed Out

**Issue**: Cannot type in input fields

**Solutions**:
- Check form state in React DevTools
- Verify no CSS `pointer-events: none`
- Look for disabled attribute on element
- Check for loading state

#### 4. Mobile View Broken

**Issue**: Layout breaks on small screens

**Solutions**:
- Check viewport meta tag in index.html
- Verify Tailwind responsive classes are correct
- Test in DevTools mobile view (F12)
- Check for overflow issues

### Performance Issues

#### 1. Slow Page Load

**Issue**: Dashboard/pages take long to load

**Solutions**:
- Check Network tab for slow requests
- Verify backend performance
- Clear browser cache
- Check for console errors that might block rendering

#### 2. Memory Leak or Slow Over Time

**Issue**: App gets slower the longer it runs

**Solutions**:
- Check for unremoved event listeners
- Verify useEffect cleanup functions
- Restart dev server
- Check Task Manager for memory usage

#### 3. Large Bundle Size

**Solutions**:
```bash
npm run build -- --analyze
# Review output to find large dependencies
```

### Database/Backend Issues

#### 1. Backend Not Responding

**Error**: Cannot connect to http://localhost:8080

**Solutions**:
- Verify backend is running
- Check it's running on port 8080
- Check logs in backend terminal
- Restart backend

#### 2. 500 Internal Server Error

**Error**: Backend returns 500 status

**Solutions**:
- Check backend logs for error
- Verify data format is correct
- Try simpler test case
- Restart backend service

## Diagnostic Checklist

### When Something Doesn't Work

Follow this checklist:

1. **Check Browser Console** (F12)
   - [ ] Any JavaScript errors?
   - [ ] Any warnings?
   - [ ] API call visible in Network tab?

2. **Check Network Tab** (F12 Network)
   - [ ] Request being made to right URL?
   - [ ] Correct HTTP method?
   - [ ] Correct headers?
   - [ ] Response status (200, 400, 401, 403, 500)?
   - [ ] Response body contains error message?

3. **Verify Backend** 
   - [ ] Backend is running? (http://localhost:8080)
   - [ ] No errors in backend logs?
   - [ ] Database is accessible?
   - [ ] Port 8080 is not blocked?

4. **Verify Frontend**
   - [ ] Dev server running on 3000?
   - [ ] No TypeScript errors?
   - [ ] No build errors?
   - [ ] Browser showing correct URL?

5. **Clear Cache & Restart**
   - [ ] Clear browser cache (F12 Storage Clear)
   - [ ] Restart dev server
   - [ ] Hard refresh browser (Ctrl+Shift+R)
   - [ ] Clear node_modules and reinstall

## Getting More Help

### View Logs

**Frontend**:
```bash
# Browser console shows logs
F12 Console
```

**Backend**:
```bash
# Check backend terminal
# See process-automation-monitor/ docs
```

### Debug with React DevTools

1. Install [React DevTools extension](https://react.dev/link/react-devtools)
2. Open DevTools (F12)
3. Go to Components tab
4. Inspect component structure
5. View component state and props

### Network Debugging

1. Open DevTools (F12)
2. Go to Network tab
3. Perform action that fails
4. Check request/response:
   - URL
   - Headers
   - Status code
   - Response body

### API Testing

Test API directly without frontend:

```bash
# Install curl or use tool like Postman

# Test backend
curl -X GET http://localhost:8080/jobs

# Test with auth header (replace TOKEN)
curl -X GET http://localhost:8080/jobs \
  -H "Authorization: Bearer TOKEN"
```

## Still Having Issues?

1. Check [DEVELOPMENT.md](DEVELOPMENT.md) for setup
2. Review [GETTING_STARTED.md](GETTING_STARTED.md) for basics
3. Check backend [README](../process-automation-monitor/README.md)
4. Search existing GitHub issues
5. Create new issue with:
   - Error message
   - Steps to reproduce
   - Console output
   - Browser/OS version
   - Screenshot if relevant
