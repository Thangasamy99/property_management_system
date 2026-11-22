#!/usr/bin/env bash
# PCC restore — ./restore.sh /var/backups/pcc/YYYYMMDD-HHMM
set -euo pipefail
source /etc/pcc/pcc.env
DIR=${1:?usage: restore.sh <backup-dir>}

systemctl stop 'pcc@*' 2>/dev/null || true
for f in "$DIR"/*.sql.gz; do
  db=$(basename "$f" .sql.gz)
  gunzip -c "$f" | mysql -h127.0.0.1 -u"$DB_USERNAME" -p"$DB_PASSWORD" "$db"
  echo "restored $db"
done
tar -xzf "$DIR/documents.tar.gz" -C "${DOC_STORAGE_ROOT:-/var/lib/pcc/documents}"
systemctl start pcc@identity pcc@property pcc@land pcc@finance pcc@document pcc@workflow pcc@reporting pcc@gateway
echo "Restore complete from $DIR"
