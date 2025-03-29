#!/bin/bash
# Backup PostgreSQL database to local storage or cloud (S3/GCS)
# Run via cron: 0 2 * * * /path/to/backup-postgres.sh

set -e

BACKUP_DIR="${BACKUP_DIR:-./../backups}"
DB_HOST="${DB_HOST:-postgres}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-orchestrator_db}"
DB_USER="${DB_USER:-orchestrator}"
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-30}"
S3_BUCKET="${S3_BUCKET:-}"  # Optional: s3://my-backup-bucket

mkdir -p "$BACKUP_DIR"

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="postgres_${DB_NAME}_${TIMESTAMP}.sql.gz"
BACKUP_PATH="$BACKUP_DIR/$BACKUP_FILE"

echo "[$(date)] Starting PostgreSQL backup..."

# Create backup
if PGPASSWORD="$DB_PASSWORD" pg_dump \
  -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" "$DB_NAME" | \
  gzip > "$BACKUP_PATH"; then
  echo "[$(date)] ✓ Backup created: $BACKUP_FILE"
else
  echo "[$(date)] ✗ Backup failed" >&2
  exit 1
fi

# Verify backup
if gunzip -t "$BACKUP_PATH" >/dev/null 2>&1; then
  echo "[$(date)] ✓ Backup integrity verified"
else
  echo "[$(date)] ✗ Backup corrupted" >&2
  rm "$BACKUP_PATH"
  exit 1
fi

# Upload to S3 (optional)
if [ -n "$S3_BUCKET" ]; then
  if aws s3 cp "$BACKUP_PATH" "$S3_BUCKET/$BACKUP_FILE"; then
    echo "[$(date)] ✓ Backup uploaded to S3"
  else
    echo "[$(date)] ⚠ S3 upload failed, keeping local copy" >&2
  fi
fi

# Cleanup old backups (local)
echo "[$(date)] Cleaning up backups older than $BACKUP_RETENTION_DAYS days..."
find "$BACKUP_DIR" -name "postgres_*.sql.gz" -mtime "+$BACKUP_RETENTION_DAYS" -delete

echo "[$(date)] Backup completed successfully"
