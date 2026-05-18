# Job Configuration Guide

## Job Types Overview

### 1. HTTP_CALL
Execute HTTP requests (GET, POST, PUT, DELETE)

**Configuration Fields**:
- URL - endpoint to call
- Method - HTTP method
- Headers - custom headers
- Body - request payload (for POST/PUT)
- Authentication - basic auth or token

**Use Cases**:
- Call external APIs
- Trigger webhooks
- Send data to third-party services

### 2. CSV_PROCESS
Process CSV files (filter, select columns, sort)

**Configuration Fields**:
- Source Type - file path, S3 bucket, database table
- Source Path - location of CSV
- Filter - optional row filtering (column = value)
- Columns - select which columns to keep
- Sort - optional sorting by column
- Output Format - CSV, JSON, or Excel
- Output Path - where to save result

**Use Cases**:
- Daily data transformation
- Report generation
- Data cleaning and normalization

**Example Configuration**:
```json
{
  "source": {
    "type": "file",
    "path": "/data/sales.csv"
  },
  "processing": {
    "filter": {
      "field": "status",
      "value": "completed"
    },
    "columns": ["date", "amount", "customer"],
    "sort": {
      "field": "amount",
      "direction": "desc"
    }
  },
  "output": {
    "format": "csv",
    "path": "/reports/processed.csv"
  }
}
```

### 3. DATA_VALIDATE
Validate data against rules

**Configuration Fields**:
- Data Source - input file/table
- Validation Rules - what to check
- Error Handling - fail or warn
- Output - report location

**Use Cases**:
- Data quality checks
- Pre-import validation
- Compliance verification

### 4. REPORT_GENERATE
Generate reports from data

**Configuration Fields**:
- Data Source - input data
- Template - report template
- Format - PDF, HTML, Excel
- Output - report location
- Recipients - email recipients (optional)

**Use Cases**:
- Automated reporting
- Executive summaries
- Scheduled email reports

## Common Configuration Patterns

### Daily CSV Processing at 9 AM
```
Name: Daily Sales Report
Type: CSV_PROCESS
Schedule: 0 9 * * *
Configuration:
  - Source: /data/sales-daily.csv
  - Filter: status = completed
  - Columns: date, amount, customer, region
  - Sort: amount DESC
  - Output: /reports/daily-sales.csv
```

### Hourly Data Validation
```
Name: Hourly Data Quality Check
Type: DATA_VALIDATE
Schedule: 0 * * * *
Configuration:
  - Data Source: production_database
  - Rules: Check for null values, valid dates, etc.
  - Error Handling: WARN
  - Output: /logs/validation-report.log
```

### Weekly Report Generation
```
Name: Weekly Executive Report
Type: REPORT_GENERATE
Schedule: 0 9 * * 1  (Every Monday at 9 AM)
Configuration:
  - Data Source: aggregated_metrics
  - Template: executive-summary
  - Format: PDF
  - Output: /reports/weekly-summary.pdf
  - Recipients: executives@company.com
```

## Scheduling Guide

### Cron Expression Basics

Format: `minute hour day-of-month month day-of-week`

**Common Examples**:
- `0 0 * * *` - Every day at midnight
- `0 9 * * *` - Every day at 9 AM
- `0 9 * * 1-5` - Weekdays at 9 AM
- `0 9 * * 0` - Every Sunday at 9 AM
- `0 */4 * * *` - Every 4 hours
- `0 9 * 1 *` - First day of month at 9 AM
- `0 18 * * 1,3,5` - Mon/Wed/Fri at 6 PM
- `*/30 * * * *` - Every 30 minutes

**Helper**: Use [crontab.guru](https://crontab.guru) to create expressions

### Business Hours Only
Run job only during business hours (9 AM - 5 PM, weekdays):
```
0 9-17 * * 1-5
```

### First Day of Month
Run on first day of month at 8 AM:
```
0 8 1 * *
```

### Twice a Day
Run at 9 AM and 3 PM:
```
0 9,15 * * *
```

## Timeout and Retry Settings

### Timeout
- **Default**: 30 seconds
- **Range**: 1 - 300 seconds
- **Use When**: Job might take a while (large file processing, external API call)

### Max Retries
- **Default**: 3 retries
- **Range**: 0 - 10
- **Use When**: Job might fail temporarily (network issues)

**Example**:
- Large CSV processing: timeout = 300s, retries = 3
- API call: timeout = 60s, retries = 5
- Quick validation: timeout = 10s, retries = 0

## Advanced Configuration

### Using JSON Editor

For advanced users, you can edit JSON directly:

1. Go to **Step 3: Review**
2. Click ** Edit JSON**
3. Modify configuration
4. Click ** Done Editing**

**JSON Editor Features**:
- Live syntax validation
- Red border if JSON is invalid
- "Create Job" button disabled until valid

### Custom Headers (HTTP_CALL)

Add authorization headers:
```json
{
  "url": "https://api.example.com/data",
  "method": "POST",
  "headers": {
    "Authorization": "Bearer YOUR_TOKEN",
    "Content-Type": "application/json"
  },
  "body": {
    "key": "value"
  }
}
```

### Dynamic Paths

Use variables in paths (backend dependent):
```json
{
  "source": {
    "path": "/data/{date}.csv"
  },
  "output": {
    "path": "/reports/{date}-processed.csv"
  }
}
```

**Available Variables** (if supported by backend):
- `{date}` - Today's date
- `{yesterday}` - Yesterday's date
- `{month}` - Current month
- `{year}` - Current year

## Validation & Error Handling

### Pre-Creation Validation

The form validates:
- Job name is required
- Cron expression is valid
- Timeout is 1-300 seconds
- Retries is 0-10

### Run-Time Validation

Backend validates:
- Source files/paths exist
- Output directory is writable
- External APIs are reachable
- Database connections work

### Error Handling

If job fails:
1. **Retry Logic**: If retries remaining, retry after delay
2. **Failure Logging**: Error logged with timestamp
3. **Alert** (if configured): Send notification
4. **Status**: Job marked as FAILED

View details in:
- **Execution History** - See what failed
- **Alerts** - Get notifications
- **Logs** - Detailed error messages

## Best Practices

1. **Start Simple**: Create simple jobs first, then add complexity
2. **Test First**: Run job manually before scheduling
3. **Use Descriptive Names**: "Daily_Sales_Report" not "Job1"
4. **Set Appropriate Timeouts**: Don't set too low or too high
5. **Monitor Results**: Check execution history regularly
6. **Version Your Configs**: Keep backups of working configurations
7. **Use Comments**: Add description explaining job purpose
8. **Schedule Off-Peak**: Large jobs during low-traffic times
9. **Set Realistic Retries**: Too many retries = delayed failures
10. **Test Cron Expressions**: Verify with crontab.guru before saving

## Troubleshooting Configuration

### Job Not Running
- Check if job is enabled (toggle in Jobs list)
- Verify cron expression with crontab.guru
- Check execution history for errors

### Jobs Running at Wrong Time
- Verify cron expression (use crontab.guru)
- Check server timezone settings
- Ensure job is enabled

### Source File Not Found
- Verify file path is correct
- Check file exists and is readable
- For S3: verify bucket and permissions

### Output File Not Created
- Check output path is writable
- Verify disk space available
- Check job executed successfully (no errors)

### Job Timing Out
- Increase timeout value
- Optimize the task (smaller dataset, faster API)
- Split into multiple jobs

## Configuration Examples

See [GETTING_STARTED.md](GETTING_STARTED.md) for step-by-step examples.
