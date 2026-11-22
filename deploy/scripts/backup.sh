#!/usr/bin/env bash
# PCC backup — seven databases + private document storage.
# Requires AGE_RECIPIENT to encrypt all artifacts before they leave this host.
set -euo pipefail
umask 077
source /etc/pcc/pcc.env
source /etc/pcc/backup.env

: "${AGE_RECIPIENT:?Set AGE_RECIPIENT to the offsite backup recipient public key}"
command -v age >/dev/null || { echo "age is required" >&2; exit 1; }
command -v mysqldump >/dev/null || { echo "mysqldump is required" >&2; exit 1; }

DEST=${BACKUP_DIR:-/var/backups/pcc}/$(date +%Y%m%d-%H%M)
mkdir -m 700 -p "$DEST"
TMP_CNF=$(mktemp)
trap 'rm -f "$TMP_CNF"' EXIT
chmod 600 "$TMP_CNF"

databases=(identity property land finance document workflow reporting)
for service in "${databases[@]}"; do
  # Per-service read-only backup credentials, distinct from application users.
  user_var="BACKUP_DB_${service^^}_USER"
  pass_var="BACKUP_DB_${service^^}_PASSWORD"
  db="pcc_${service}_db"
  user=${!user_var:?Missing $user_var}
  pass=${!pass_var:?Missing $pass_var}
  printf '[client]\nuser=%s\npassword=%s\nhost=127.0.0.1\n' "$user" "$pass" > "$TMP_CNF"
  mysqldump --defaults-extra-file="$TMP_CNF" --single-transaction \
    --routines --events "$db" | gzip -c | age -r "$AGE_RECIPIENT" \
    -o "$DEST/$db.sql.gz.age"
done

DOC_ROOT=${DOC_STORAGE_ROOT:-/var/lib/pcc/documents}
tar -czf - -C "$DOC_ROOT" . | age -r "$AGE_RECIPIENT" \
  -o "$DEST/documents.tar.gz.age"
chmod 600 "$DEST"/*

echo "Encrypted backup completed: $DEST"
# Retain 14 days; remove only this backup root's old dated directories.
find "${BACKUP_DIR:-/var/backups/pcc}" -mindepth 1 -maxdepth 1 \
  -type d -mtime +14 -exec rm -rf -- {} +
