#!/bin/bash
# Backup MySQL databases to local storage or cloud (S3/GCS)
# Run via cron: 0 3 * * * /path/to/backup-mysql.sh

set -e

BACKUP_DIR="${BACKUP_DIR:-./../backups}"
DB_HOST="${DB_HOST:-mysql}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-root}"
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-30}"
S3_BUCKET="${S3_BUCKET:-}"  # Optional: s3://my-backup-bucket

mkdir -p "$BACKUP_DIR"

TIMESTAMP=$(date +%Y%m%d_%H%M%S)

echo "[$(date)] Starting MySQL backup..."

# Backup worker DB
WORKER_BACKUP="mysql_worker_db_${TIMESTAMP}.sql.gz"
WORKER_PATH="$BACKUP_DIR/$WORKER_BACKUP"

if mysqldump -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" \
  --single-transaction --quick --lock-tables=false \
  worker_db | gzip > "$WORKER_PATH"; then
  echo "[$(date)] ✓ Worker DB backup: $WORKER_BACKUP"
else
  echo "[$(date)] ✗ Worker DB backup failed" >&2
  exit 1
fi

# Backup analytics DB
ANALYTICS_BACKUP="mysql_analytics_db_${TIMESTAMP}.sql.gz"
ANALYTICS_PATH="$BACKUP_DIR/$ANALYTICS_BACKUP"

if mysqldump -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" \
  --single-transaction --quick --lock-tables=false \
  analytics_db | gzip > "$ANALYTICS_PATH"; then
  echo "[$(date)] ✓ Analytics DB backup: $ANALYTICS_BACKUP"
else
  echo "[$(date)] ✗ Analytics DB backup failed" >&2
  exit 1
fi

# Verify backups
if gunzip -t "$WORKER_PATH" >/dev/null 2>&1 && gunzip -t "$ANALYTICS_PATH" >/dev/null 2>&1; then
  echo "[$(date)] ✓ Backup integrity verified"
else
  echo "[$(date)] ✗ Backup corrupted" >&2
  rm "$WORKER_PATH" "$ANALYTICS_PATH"
  exit 1
fi

# Upload to S3 (optional)
if [ -n "$S3_BUCKET" ]; then
  aws s3 cp "$WORKER_PATH" "$S3_BUCKET/$WORKER_BACKUP" && \
  aws s3 cp "$ANALYTICS_PATH" "$S3_BUCKET/$ANALYTICS_BACKUP" && \
  echo "[$(date)] ✓ Backups uploaded to S3" || \
  echo "[$(date)] ⚠ S3 upload failed, keeping local copies" >&2
fi

# Cleanup old backups (local)
echo "[$(date)] Cleaning up backups older than $BACKUP_RETENTION_DAYS days..."
find "$BACKUP_DIR" -name "mysql_*.sql.gz" -mtime "+$BACKUP_RETENTION_DAYS" -delete

echo "[$(date)] MySQL backup completed successfully"
