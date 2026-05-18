#!/bin/bash
# ============================================================================
# Setup Environment Variables for Process Automation Monitor
# ============================================================================
# This script generates secure random secrets and creates .env file
# Usage: ./setup-env.sh
# ============================================================================

set -e

echo "🚀 Process Automation Monitor - Environment Setup"
echo "=================================================="

# Check if .env already exists
if [ -f ".env" ]; then
    echo "⚠️  .env file already exists. Backing up to .env.backup"
    mv .env .env.backup
fi

# Generate random secrets
echo ""
echo "🔐 Generating random secrets..."
JWT_SECRET=$(openssl rand -base64 32)
WORKER_API_KEY=$(openssl rand -base64 32)
ANALYTICS_API_KEY=$(openssl rand -base64 32)
POSTGRES_PASSWORD=$(openssl rand -base64 16)
MYSQL_PASSWORD=$(openssl rand -base64 16)
MYSQL_ROOT_PASSWORD=$(openssl rand -base64 16)
MYSQL_ANALYTICS_PASSWORD=$(openssl rand -base64 16)
MYSQL_ANALYTICS_ROOT_PASSWORD=$(openssl rand -base64 16)

# Create .env file
cat > .env << EOF
# ============================================================================
# Process Automation Monitor - Generated Environment Configuration
# ============================================================================
# Generated at: $(date)
# DO NOT commit this file to git!
# ============================================================================

# ─── PostgreSQL (Orchestrator Database) ──────────────────────────────────
POSTGRES_DB=orchestrator_db
POSTGRES_USER=orchestrator
POSTGRES_PASSWORD=${POSTGRES_PASSWORD}

# ─── MySQL (Worker Database) ─────────────────────────────────────────────
MYSQL_DATABASE=worker_db
MYSQL_USER=worker
MYSQL_PASSWORD=${MYSQL_PASSWORD}
MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}

# ─── MySQL (Analytics Database) ──────────────────────────────────────────
MYSQL_ANALYTICS_DATABASE=analytics_db
MYSQL_ANALYTICS_USER=analytics
MYSQL_ANALYTICS_PASSWORD=${MYSQL_ANALYTICS_PASSWORD}
MYSQL_ANALYTICS_ROOT_PASSWORD=${MYSQL_ANALYTICS_ROOT_PASSWORD}

# ─── JWT & Authentication ───────────────────────────────────────────────
JWT_SECRET=${JWT_SECRET}
JWT_EXPIRY_MINUTES=15
JWT_REFRESH_EXPIRY_DAYS=7

# ─── Service-to-Service Authentication ──────────────────────────────────
WORKER_API_KEY=${WORKER_API_KEY}
ANALYTICS_API_KEY=${ANALYTICS_API_KEY}

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
EOF

echo ""
echo "✅ .env file created successfully!"
echo ""
echo "📋 Configuration Summary:"
echo "   - PostgreSQL: orchestrator / $(echo ${POSTGRES_PASSWORD} | cut -c1-8)..."
echo "   - MySQL Worker: worker / $(echo ${MYSQL_PASSWORD} | cut -c1-8)..."
echo "   - MySQL Analytics: analytics / $(echo ${MYSQL_ANALYTICS_PASSWORD} | cut -c1-8)..."
echo "   - JWT Secret: $(echo ${JWT_SECRET} | cut -c1-8)..."
echo ""
echo "🚀 Next steps:"
echo "   1. docker-compose up -d     # Start all services"
echo "   2. http://localhost:3000    # Open frontend"
echo "   3. Register new account     # Create user account"
echo ""
echo "ℹ️  Service URLs:"
echo "   - Frontend:   http://localhost:3000"
echo "   - Orchestrator API: http://localhost:8080/api/"
echo "   - Worker Health:    http://localhost:8000/health"
echo "   - Analytics Health: http://localhost:8001/health"
echo ""
echo "✨ Setup complete!"
