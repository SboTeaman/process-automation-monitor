# UI Features Guide

## Dashboard

The main landing page showing system overview.

### Key Metrics
- **Total Jobs** - Count of all jobs
- **Success Rate** - Percentage of successful executions
- **Recent Errors** - Count of failed jobs in last 24h
- **Active Alerts** - Number of active alerts

### Charts
- **Daily Executions** - Line chart showing execution trend
- **Top Failing Jobs** - List of jobs with most failures
- **Job Performance** - Execution time and success metrics

### Quick Actions
- Click metric card for details
- Click job name to edit
- Use refresh button to update manually

## Navigation

### Main Menu (Left Sidebar)
- **Dashboard** - Home page with metrics
- **Jobs** - Job management (list, create, edit)
- **Analytics** - Detailed performance analysis
- **Alerts** - Alert configuration and history
- **Execution History** - View all past executions

### Top Bar
- Logo/Home - Click to go to dashboard
- User profile - Account settings
- Logout - Sign out

## Jobs Page

### Job List View

**Columns**:
- Job Name
- Type - HTTP_CALL, CSV_PROCESS, etc.
- Status - ACTIVE or INACTIVE
- Next Run - When job will run next
- Last Run - When job last executed
- Actions

**Actions Available**:
-¶ **Play Icon** - Execute job immediately
- **Edit Icon** - Edit job configuration
- **Delete Icon** - Remove job (with confirmation)
- **Toggle Icon** - Enable/disable job

### Pagination
- Shows 10 jobs per page
- Navigate with Previous/Next buttons
- Shows current page and total pages

### Search & Filter
- Filter by job type
- Search by job name
- Sort by different columns

## Job Creation (Visual Form Builder)

The most important feature - creating jobs without JSON!

### Step 1: Basic Information

**Fields**:
- **Job Name** *(required)* - User-friendly name
- **Description** - What the job does
- **Job Type** *(required)* - Select from: HTTP_CALL, CSV_PROCESS, DATA_VALIDATE, REPORT_GENERATE
- **Schedule** *(required)* - Cron expression (with crontab.guru link)
- **Timeout** - Job timeout in seconds (1-300)
- **Max Retries** - Number of retries on failure (0-10)
- **Enable Job** - Checkbox to start job immediately

**Validation**:
- All required fields must be filled
- Cron expression format is validated
- Timeout and retries are in valid ranges

### Step 2: Configuration

Configuration form changes based on job type.

#### CSV_PROCESS Example

** Source File Section**:
- Radio buttons for source type (File, S3, Database)
- Path input field
- Helper text with example

** Filter Section**:
- Toggle to enable/disable filtering
- Column name input
- Value input
- Example hint

** Column Selection**:
- Checkboxes for all available columns
- Shows selected columns in real-time
- Grid layout (2-3 columns)

** Sort Section**:
- Toggle to enable/disable sorting
- Column dropdown
- Direction (Ascending/Descending)

** Output Section**:
- Format dropdown (CSV, JSON, Excel)
- Output path input

** JSON Preview**:
- Live update as you fill form
- Shows generated JSON below form
- Read-only (use Step 3 to edit)

### Step 3: Review & Create

**Summary Sections**:
- Basic Information summary
- Configuration summary
- Generated JSON preview

**JSON Editor** *(Optional)*:
- Click ** Edit JSON** to enable
- Full JSON editor with validation
- Red border if invalid
- Click ** Done Editing** when done

**Status Indicator**:
- Green: Configuration looks good
- Red: Invalid JSON

**Actions**:
- ** Previous** - Go back to Step 2
- **Create Job** - Submit the form
- **Cancel** - Abandon and go back to jobs list

## Job Edit Page

Similar to creation but with pre-filled values.

**Features**:
- All form fields editable
- Can change any configuration
- Save changes or cancel
- Delete job button at bottom

## Analytics Page

Detailed performance metrics and insights.

### Metrics Displayed
- Total jobs executed
- Success rate (%)
- Average execution time
- Peak execution times
- Top performing jobs
- Most failing jobs

### Charts
- **Execution Timeline** - Line chart over time
- **Success vs Failure** - Pie chart
- **Job Performance** - Bar chart
- **Execution Times** - Histogram

### Filters
- Date range picker
- Filter by job type
- Filter by status

### Export
- Export data to CSV
- Export charts as images

## Alerts Page

Manage job alerts and notifications.

### Alert List
- Alert name
- Trigger condition (job fails, timeout, etc.)
- Notification method (email, webhook)
- Status (enabled/disabled)
- Actions (edit, delete)

### Create Alert
- Job selection
- Trigger type
- Notification method
- Recipients/webhooks
- Enable/disable

### Alert History
- View past triggered alerts
- When alert was sent
- What triggered it
- Notification status

## Execution History Page

Complete history of job executions.

### History Table

**Columns**:
- Job Name - Link to job
- Start Time - When execution started
- End Time - When execution finished
- Duration - How long it took
- Status - SUCCESS or FAILED
- Error Message - If failed

### Filters
- Date range
- Job name search
- Status filter (success/failed)
- Job type filter

### Details View
Click execution row to see:
- Full execution logs
- Input/output details
- Performance metrics
- Error trace (if failed)

### Export
- Export history to CSV
- Print execution details

## Profile & Account

### User Settings
- Username display
- Email address
- Last login time
- Password change option

### Preferences
- Theme (light/dark)
- Timezone
- Date format
- Email notifications

## Responsive Design

All pages are fully responsive:
- **Desktop** (1200px+) - Full layout
- **Tablet** (768px-1199px) - Adjusted layout
- **Mobile** (< 768px) - Stacked layout, mobile menu

## Accessibility

- All inputs have labels
- Error messages are clear
- Color not used alone to convey information
- Keyboard navigation supported
- Screen reader friendly

## Dark Mode

Toggle dark mode in:
- User profile menu
- Settings page
- Automatic detection from system

## Loading States

- Loading spinners on data fetch
- Skeleton screens on first load
- Disabled buttons during submit
- Clear error messages

## Notifications

### Toast Messages
- Success (green) - Job created successfully
- Error (red) - Failed to create job
- Warning (yellow) - Warning messages
- Info (blue) - Information messages

### In-Page Alerts
- Highlighted alert boxes
- Contextual information
- Links to more details

## Keyboard Shortcuts

- `Ctrl/Cmd + K` - Quick search/command palette
- `Ctrl/Cmd + S` - Save (on edit pages)
- `Esc` - Close dialogs/modals
- `Enter` - Submit forms
- `Tab` - Navigate form fields

## Tooltips & Help

- Hover over question marks for help
- Field-level explanations
- Example values in placeholders
- Links to external resources (crontab.guru)

## Empty States

- When no jobs exist - Prompt to create first job
- When no executions - Prompt to run a job
- When no alerts - Option to create first alert
- Helpful messages and action buttons

## Error Handling

### Error Messages
- Clear problem description
- Suggested solution
- Link to relevant documentation
- Retry button if applicable

### Validation Errors
- Inline validation messages
- Red highlights on invalid fields
- Summary of all errors above form
- Clear error descriptions

## Performance UI Indicators

- Page load times
- API response times
- Job execution times
- Resource usage

## Browser Compatibility

- Chrome/Edge 90+
- Firefox 88+
- Safari 14+
- Mobile browsers (iOS Safari, Chrome Mobile)
