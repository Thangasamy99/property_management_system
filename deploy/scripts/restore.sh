#!/usr/bin/env bash
# Non-destructive recovery test: restore only to pre-created *_restore_test
# databases and an isolated document directory. Never writes live DBs.
set -euo pipefail
umask 077
source /etc/pcc/pcc.env
source /etc/pcc/restore.env

DIR=${1:?usage: restore.sh <encrypted-backup-directory>}
: "${AGE_IDENTITY_FILE:?Set AGE_IDENTITY_FILE to a protected age identity file}"
RESTORE_ROOT=${RESTORE_ROOT:-/var/lib/pcc/restore-test}
DOC_DEST=${RESTORE_DOC_ROOT:-$RESTORE_ROOT/documents}
mkdir -m 700 -p "$DOC_DEST"
if find "$DOC_DEST" -mindepth 1 -print -quit | grep -q .; then
  echo "Document restore destination is not empty; refusing overwrite" >&2
  exit 1
fi

command -v age >/dev/null || { echo "age is required" >&2; exit 1; }
for service in identity property land finance document workflow reporting; do
  db="pcc_${service}_restore_test"
  archive="$DIR/pcc_${service}_db.sql.gz.age"
  test -r "$archive" || { echo "Missing archive for $service" >&2; exit 1; }
  # Restore credentials are separate and must have access only to the test DBs.
  user_var="RESTORE_DB_${service^^}_USER"
  pass_var="RESTORE_DB_${service^^}_PASSWORD"
  user=${!user_var:?Missing $user_var}
  pass=${!pass_var:?Missing $pass_var}
  MYSQL_PWD="$pass" mysql -h127.0.0.1 -u"$user" -Nse \
    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$db'" \
    | grep -qx 0 || { echo "$db is not empty; refusing overwrite" >&2; exit 1; }
  age -d -i "$AGE_IDENTITY_FILE" "$archive" | gzip -dc \
    | MYSQL_PWD="$pass" mysql -h127.0.0.1 -u"$user" "$db"
  echo "restored $service into isolated $db"
done

age -d -i "$AGE_IDENTITY_FILE" "$DIR/documents.tar.gz.age" \
  | tar -xz -C "$DOC_DEST"
echo "Document store restored to isolated path: $DOC_DEST"
