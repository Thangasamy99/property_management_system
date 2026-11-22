#!/usr/bin/env bash
# PCC backup — all service databases + document store. Cron nightly.
set -euo pipefail
source /etc/pcc/pcc.env
DEST=${BACKUP_DIR:-/var/backups/pcc}/$(date +%Y%m%d-%H%M)
mkdir -p "$DEST"

for db in pcc_identity_db pcc_property_db pcc_land_db pcc_finance_db \
          pcc_document_db pcc_workflow_db pcc_reporting_db; do
  mysqldump -h127.0.0.1 -u"$DB_USERNAME" -p"$DB_PASSWORD" \
    --single-transaction --routines "$db" | gzip > "$DEST/$db.sql.gz"
done

tar -czf "$DEST/documents.tar.gz" -C "${DOC_STORAGE_ROOT:-/var/lib/pcc/documents}" .
echo "Backup complete: $DEST"
# keep 14 days
find "${BACKUP_DIR:-/var/backups/pcc}" -maxdepth 1 -type d -mtime +14 -exec rm -rf {} +
