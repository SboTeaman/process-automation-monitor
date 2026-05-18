# Getting Started Guide

## Installation & Setup

### Prerequisites
- Node.js 18 or higher
- npm or yarn
- Git

### Frontend Setup

1. **Navigate to frontend directory**:
```bash
cd frontend
```

2. **Install dependencies**:
```bash
npm install
```

3. **Start development server**:
```bash
npm run dev
```

The frontend will start on `http://localhost:3000`

### Backend Setup

See the backend documentation in `process-automation-monitor/` for detailed setup instructions.

**Quick start** (assumes backend is set up):
- Backend should run on `http://localhost:8080`
- Frontend proxy automatically routes API calls to backend

## First Time Usage

### 1. Register
- Go to `http://localhost:3000/register`
- Create a new account with email and password

### 2. Login
- Use your credentials to login
- You'll be redirected to the dashboard

### 3. Create Your First Job

#### Option A: Visual Form Builder (Recommended)
1. Click **Jobs** **Create Job**
2. **Step 1 - Basic Information**:
   - Enter job name (e.g., "Daily Report Processing")
   - Select job type (e.g., CSV_PROCESS)
   - Set schedule using cron expression
   - Adjust timeout and retry settings

3. **Step 2 - Configure**:
   - For CSV_PROCESS:
     - Specify source file path
     - Add filters (optional)
     - Select columns to keep
     - Set sort order (optional)
     - Choose output format and path

4. **Step 3 - Review**:
   - Review all settings
   - Optionally edit JSON directly
   - Click **Create Job**

#### Option B: JSON Editor
If you prefer raw JSON:
1. Go to **Step 3: Review**
2. Click ** Edit JSON**
3. Paste or edit JSON configuration
4. Click ** Done Editing**
5. Click **Create Job**

### 4. Monitor Jobs

- **Jobs List**: View all created jobs
- **Dashboard**: See job statistics and recent executions
- **Analytics**: Detailed performance metrics
- **Execution History**: View past job runs

## Common Tasks

### Execute a Job Now
1. Go to **Jobs** list
2. Find your job
3. Click the **Play** icon
4. Job executes immediately

### Edit a Job
1. Go to **Jobs** list
2. Click **Edit** icon on the job
3. Modify configuration
4. Save changes

### Enable/Disable a Job
1. Go to **Jobs** list
2. Click the **Toggle** icon to enable/disable
3. Disabled jobs won't run on schedule

### View Job History
1. Click **Execution History**
2. Filter by date or job
3. Click on execution to see details

## Cron Expression Guide

Common patterns:
- `0 0 * * *` - Daily at midnight
- `0 9 * * *` - Every day at 9 AM
- `0 0 * * 0` - Every Sunday at midnight
- `0 */4 * * *` - Every 4 hours
- `0 9-17 * * *` - Every hour from 9 AM to 5 PM (weekdays)

**Tip**: Use [crontab.guru](https://crontab.guru) to create and verify cron expressions.

## Troubleshooting

### Login Issues
- Ensure backend is running
- Check email and password are correct
- Clear browser cache and try again

### Jobs Not Showing
- Refresh the page (F5)
- Verify you're logged in
- Check browser console for errors (F12)

### API Connection Issues
- Verify backend is running on port 8080
- Check that frontend can reach backend
- Look for CORS errors in browser console

### Port Already in Use
```bash
# Kill process on port 3000
# Windows (PowerShell)
Get-Process node -ErrorAction SilentlyContinue | Stop-Process -Force

# Or change Vite port in vite.config.ts
```

## Next Steps

- Read [Job Configuration Guide](JOB_CONFIGURATION.md) for detailed job setup
- Check [API Reference](API_REFERENCE.md) for backend API endpoints
- Explore [Architecture Overview](ARCHITECTURE.md) to understand the system

## Need Help?

- Check [Troubleshooting Guide](TROUBLESHOOTING.md)
- Review [UI Features](UI_FEATURES.md) for detailed feature documentation
- Open an issue on GitHub
