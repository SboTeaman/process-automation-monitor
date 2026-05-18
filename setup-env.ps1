# ============================================================================
# Setup Environment Variables for Process Automation Monitor (Windows)
# ============================================================================
# This script generates secure random secrets and creates .env file
# Usage: .\setup-env.ps1
# ============================================================================

Write-Host "🚀 Process Automation Monitor - Environment Setup" -ForegroundColor Green
Write-Host "==================================================" -ForegroundColor Green

# Check if .env already exists
if (Test-Path ".env") {
    Write-Host "⚠️  .env file already exists. Backing up to .env.backup" -ForegroundColor Yellow
    Move-Item -Path ".env" -Destination ".env.backup" -Force
}

# Function to generate random base64 string
function Generate-Secret {
    param([int]$length = 32)
    $bytes = New-Object byte[] $length
    [Security.Cryptography.RNGCryptoServiceProvider]::new().GetBytes($bytes)
    return [Convert]::ToBase64String($bytes)
}

# Generate random secrets
Write-Host ""
Write-Host "🔐 Generating random secrets..." -ForegroundColor Cyan
$JWT_SECRET = Generate-Secret 32
$WORKER_API_KEY = Generate-Secret 32
$ANALYTICS_API_KEY = Generate-Secret 32
$POSTGRES_PASSWORD = Generate-Secret 16
$MYSQL_PASSWORD = Generate-Secret 16
$MYSQL_ROOT_PASSWORD = Generate-Secret 16
$MYSQL_ANALYTICS_PASSWORD = Generate-Secret 16
$MYSQL_ANALYTICS_ROOT_PASSWORD = Generate-Secret 16

# Create .env file
$envContent = @"
# ============================================================================
# Process Automation Monitor - Generated Environment Configuration
# ============================================================================
# Generated at: $(Get-Date)
# DO NOT commit this file to git!
# ============================================================================

# ─── PostgreSQL (Orchestrator Database) ──────────────────────────────────
POSTGRES_DB=orchestrator_db
POSTGRES_USER=orchestrator
POSTGRES_PASSWORD=$POSTGRES_PASSWORD

# ─── MySQL (Worker Database) ─────────────────────────────────────────────
MYSQL_DATABASE=worker_db
MYSQL_USER=worker
MYSQL_PASSWORD=$MYSQL_PASSWORD
MYSQL_ROOT_PASSWORD=$MYSQL_ROOT_PASSWORD

# ─── MySQL (Analytics Database) ──────────────────────────────────────────
MYSQL_ANALYTICS_DATABASE=analytics_db
MYSQL_ANALYTICS_USER=analytics
MYSQL_ANALYTICS_PASSWORD=$MYSQL_ANALYTICS_PASSWORD
MYSQL_ANALYTICS_ROOT_PASSWORD=$MYSQL_ANALYTICS_ROOT_PASSWORD

# ─── JWT & Authentication ───────────────────────────────────────────────
JWT_SECRET=$JWT_SECRET
JWT_EXPIRY_MINUTES=15
JWT_REFRESH_EXPIRY_DAYS=7

# ─── Service-to-Service Authentication ──────────────────────────────────
WORKER_API_KEY=$WORKER_API_KEY
ANALYTICS_API_KEY=$ANALYTICS_API_KEY

# ─── Frontend Configuration ──────────────────────────────────────────────
ALLOWED_ORIGINS=http://localhost:3000

# ─── Email / SMTP Configuration (Optional) ──────────────────────────────
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=

# ─── Application Profiles ───────────────────────────────────────────────
SPRING_PROFILES_ACTIVE=docker

# ─── Swagger / OpenAPI Documentation ───────────────────────────────────
SWAGGER_ENABLED=false

# ─── Service URLs ──────────────────────────────────────────────────────
WORKER_SERVICE_URL=http://worker-service:8000
"@

Set-Content -Path ".env" -Value $envContent -Encoding UTF8

Write-Host ""
Write-Host "✅ .env file created successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "📋 Configuration Summary:" -ForegroundColor Cyan
Write-Host "   - PostgreSQL: orchestrator / $($POSTGRES_PASSWORD.Substring(0, 8))..."
Write-Host "   - MySQL Worker: worker / $($MYSQL_PASSWORD.Substring(0, 8))..."
Write-Host "   - MySQL Analytics: analytics / $($MYSQL_ANALYTICS_PASSWORD.Substring(0, 8))..."
Write-Host "   - JWT Secret: $($JWT_SECRET.Substring(0, 8))..."
Write-Host ""
Write-Host "🚀 Next steps:" -ForegroundColor Cyan
Write-Host "   1. docker-compose up -d     # Start all services"
Write-Host "   2. http://localhost:3000    # Open frontend"
Write-Host "   3. Register new account     # Create user account"
Write-Host ""
Write-Host "ℹ️  Service URLs:" -ForegroundColor Cyan
Write-Host "   - Frontend:   http://localhost:3000"
Write-Host "   - Orchestrator API: http://localhost:8080/api/"
Write-Host "   - Worker Health:    http://localhost:8000/health"
Write-Host "   - Analytics Health: http://localhost:8001/health"
Write-Host ""
Write-Host "✨ Setup complete!" -ForegroundColor Green
