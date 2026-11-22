# PCC Hostinger Ubuntu Deployment Runbook

This prepares a single-host release. It does **not** deploy or contain live
credentials. Keep all JVM services and MySQL on loopback; expose only Nginx
ports 80/443. Review `DEVELOPMENT_STATUS.md`'s release gate before proceeding.

## 0. Release blockers

Before touching production:

1. Rotate the signing key, all employee passwords, and all database account
   passwords. `password.txt` was committed historically; its credentials must
   be treated as compromised. No history rewrite has been performed.
2. Disable/lock demo accounts, including `admin`, `po1`, `lo1`, `acc1`, and
   `mgr1`, or replace credentials through Identity's secured workflow.
3. Create a unique DB account for each service, plus separate backup/restore
   accounts. Never share an account across services.
4. Provision secrets through a protected password manager/secret store, never
   paste them in chat or shell commands. Restrict `/etc/pcc` to root and `pcc`.
5. Have a separate operator verify encrypted backups and a restore drill.

## 1. Prepare Ubuntu

```bash
sudo apt update
sudo apt install -y openjdk-21-jre mysql-server nginx certbot age
sudo useradd --system --home /opt/pcc --shell /usr/sbin/nologin pcc
sudo install -d -o pcc -g pcc -m 750 /opt/pcc /var/log/pcc
sudo install -d -o pcc -g pcc -m 700 /var/lib/pcc/documents
sudo install -d -o root -g pcc -m 750 /etc/pcc
sudo install -d -o root -g root -m 700 /var/backups/pcc
```

## 2. MySQL databases and least-privilege users

Configure MySQL `bind-address=127.0.0.1`; do not open 3306 in the firewall.
Create the seven databases and separate users in an interactive MySQL admin
session. Obtain each unique password from the approved secret manager; do not
put it in a SQL file or command-line argument.

For each service, create its DB then grant access only to that schema:

```sql
CREATE DATABASE pcc_identity_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'pcc_identity'@'127.0.0.1' IDENTIFIED BY '<secret from vault>';
GRANT ALL PRIVILEGES ON pcc_identity_db.* TO 'pcc_identity'@'127.0.0.1';
```

Repeat for `property`, `land`, `finance`, `document`, `workflow`, and
`reporting`, using a distinct database and user each time. These schema-owner
accounts need only the service schema's CRUD and Flyway DDL rights:
`SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES`. They have
no access to peer schemas.

Create backup accounts separately with read-only dump privileges. Restore
accounts must be limited to explicitly provisioned `pcc_*_restore_test`
schemas. Never grant the application users cross-schema permissions.

## 3. Secrets and per-instance configuration

Place a common file at `/etc/pcc/pcc.env` (mode `0640`, owner `root:pcc`).
Populate it from `.env.production.example` with secure secret-manager values.
The eight `application-prod.yml` files require these placeholders (no
localhost or blank fallbacks):

- `JWT_SECRET`: at least 64 random bytes; one coordinated value shared by all
  JWT issuers/verifiers and the Gateway.
- `SPRING_PROFILES_ACTIVE=prod`
- `SERVER_ADDRESS=127.0.0.1`
- `CORS_ALLOWED_ORIGINS=https://<actual-domain>`
- All seven service URL variables set to `http://127.0.0.1:8081` through `:8087`.
- `DOC_STORAGE_ROOT=/var/lib/pcc/documents`
- `COOKIE_SECURE=true`

Generate a fresh key inside the protected secret store or write it directly
to the root-owned environment file; do not let it print to a terminal or CI
log. For initial file creation:

```bash
sudo install -o root -g pcc -m 0640 /dev/null /etc/pcc/pcc.env
sudo bash -c 'umask 027; { printf "JWT_SECRET="; openssl rand -hex 48; } >> /etc/pcc/pcc.env'
```

Then set issuer, audience, profile, bind address, CORS, service URLs and cookie
settings in the same protected file. Generate distinct high-entropy
passwords for every app/backup/restore DB user in the secret manager; do not
reuse the JWT key or a shared database password.

Create `/etc/pcc/<instance>.env` for each `identity`, `property`, `land`,
`finance`, `document`, `workflow`, `reporting`, and `gateway` instance. Each
file must be mode `0640`, owner `root:pcc` and contain only that instance's
configuration. Every data service file has its own `DB_URL`,
`DB_USERNAME`, and `DB_PASSWORD`; never use a generic shared DB account.
Reporting also needs its six upstream service URL variables. Gateway needs the
same seven upstream variables. Identity's instance file must set
`PCC_ADMIN_PASSWORD` before a fresh, empty Identity database is first started;
the production bootstrap now refuses to generate/log that initial password.
The forgot-password endpoint has no mail provider and logs no reset tokens;
until delivery is integrated, use Identity's authenticated admin reset flow.
Do not rotate JWT_SECRET while services are serving traffic: coordinated
rotation requires stopping the stack, replacing the key everywhere, deleting
all Identity `refresh_tokens` rows through an approved database change, and
restarting all services. Existing access JWTs then fail signature validation.

Verify permissions without printing file contents:

```bash
sudo chown root:pcc /etc/pcc/*.env
sudo chmod 0640 /etc/pcc/*.env
sudo chmod 0700 /var/lib/pcc/documents /var/backups/pcc
```

## 4. Artifacts and frontend

Build on the release workstation, test, then transfer artifacts over SSH:

```bash
cd frontend
VITE_GATEWAY_URL=https://<actual-domain>/api npm run build
```

Copy the seven service jars to `/opt/pcc/pcc-identity.jar`,
`pcc-property.jar`, `pcc-land.jar`, `pcc-finance.jar`, `pcc-document.jar`,
`pcc-workflow.jar`, `pcc-reporting.jar`; copy the gateway jar as
`pcc-gateway.jar`. Copy `frontend/dist` to `/var/www/pcc` and `deploy/` to
`/opt/pcc/deploy/`. Ensure the `pcc` user owns service jars and private
document storage. Never put document storage beneath `/var/www`.

## 5. systemd startup

Install `deploy/systemd/pcc@.service` at
`/etc/systemd/system/pcc@.service`. The unit loads common and per-instance
environment files, binds according to `SERVER_ADDRESS`, restarts on failure,
and restricts filesystem writes. Check `ReadWritePaths` if document storage
is changed.

Start in dependency order:

```bash
sudo systemctl enable --now mysql
sudo systemctl enable --now pcc@identity
sudo systemctl enable --now pcc@property pcc@land pcc@finance pcc@document
sudo systemctl enable --now pcc@workflow pcc@reporting
sudo systemctl enable --now pcc@gateway
```

Run schema migrations during controlled first startup; verify each service
logs Flyway success and does not log credentials. Identity seeds roles and
permissions; Reporting depends on the other service APIs at request time, not
at startup. Gateway is last so routes become available only after backends.

Check each unit with `systemctl status pcc@<instance>` and logs with
`journalctl -u pcc@<instance>`. Health endpoints are local only:
`http://127.0.0.1:8080/actuator/health`; service `/api/v1/health` routes may
return 401 without a JWT, which still proves HTTP reachability.

## 6. Nginx and TLS

Obtain the first certificate through a temporary HTTP-only bootstrap site,
then install the final TLS configuration:

```bash
sudo install -d -o www-data -g www-data -m 755 /var/www/letsencrypt
sudo cp /opt/pcc/deploy/nginx-pcc-bootstrap.conf /etc/nginx/sites-available/pcc
sudo sed -i 's/pcc.example.com/<actual-domain>/g' /etc/nginx/sites-available/pcc
sudo ln -s /etc/nginx/sites-available/pcc /etc/nginx/sites-enabled/pcc
sudo nginx -t && sudo systemctl enable --now nginx
sudo certbot certonly --webroot -w /var/www/letsencrypt   --agree-tos --non-interactive --email <ops-email> -d <actual-domain>
sudo cp /opt/pcc/deploy/nginx-pcc.conf /etc/nginx/sites-available/pcc
sudo sed -i 's/pcc.example.com/<actual-domain>/g' /etc/nginx/sites-available/pcc
sudo nginx -t && sudo systemctl reload nginx
sudo systemctl enable --now certbot.timer
sudo certbot renew --dry-run
```

The final config serves the SPA, proxies `/api/` to loopback Gateway :8080,
forwards HTTPS, allows 30MB multipart requests, and retains the ACME path for
webroot renewal. Other HTTP requests redirect to HTTPS. Nginx exposes no
service ports or actuator endpoints.

## 7. Firewall

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw deny 3306/tcp
sudo ufw deny 8080:8087/tcp
sudo ufw enable
sudo ufw status verbose
```

Confirm externally that only SSH (if required) and 80/443 are reachable.
Also verify all Java listeners bind to 127.0.0.1 and MySQL is loopback-only.

## 8. Encrypted backup and isolated restore drill

Install `pcc-backup.service` and `pcc-backup.timer` from `deploy/systemd/`:

```bash
sudo cp /opt/pcc/deploy/systemd/pcc-backup.service /etc/systemd/system/
sudo cp /opt/pcc/deploy/systemd/pcc-backup.timer /etc/systemd/system/
```

copy `deploy/backup.env.example` to `/etc/pcc/backup.env` and populate unique,
least-privilege dump accounts plus the approved offsite age recipient. Keep
the age private identity off the backup host.
Enable the scheduled backup and check failure status through systemd:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now pcc-backup.timer
sudo systemctl start pcc-backup.service   # one manual verification run
systemctl is-failed pcc-backup.service
journalctl -u pcc-backup.service
```

`deploy/scripts/backup.sh` expects `/etc/pcc/backup.env` with separate,
read-only `BACKUP_DB_<SERVICE>_USER/PASSWORD` values and an `AGE_RECIPIENT`.
It sets restrictive permissions, dumps seven databases with transactional
consistency, encrypts each database archive and the document archive with
`age`, and returns nonzero on any failure. Configure a monitored systemd timer
which records failed runs in the system journal and alert on a failed unit;
transfer encrypted artifacts to an
offsite account/bucket with versioning and access logging.

Before a restore drill, provision empty schemas named
`pcc_identity_restore_test` through `pcc_reporting_restore_test`; provide
least-privilege test restore credentials in `/etc/pcc/restore.env` (copy
`deploy/restore.env.example`; root-owned, group `pcc`, mode `0640`). Protect
the age identity file similarly. Run:

```bash
sudo -u pcc /opt/pcc/deploy/scripts/restore.sh /var/backups/pcc/<timestamp>
```

The script refuses a non-empty restore DB and writes documents only beneath
an isolated restore-test path. It never stops services or writes live schemas.
Verify table counts and document hashes in the isolated environment before
approving any separate production recovery procedure. Retain at least one
encrypted offsite copy; test recovery quarterly.

## 9. Rollback

Keep the previously approved jars and frontend build in a versioned,
access-controlled release directory. Roll back by restoring those artifacts,
then restarting the affected service. Do not roll back Flyway migrations by
editing `flyway_schema_history`; database rollback requires a reviewed backup
restore into isolated databases first and a separately approved recovery plan.
For a bad frontend release, restore the previous `/var/www/pcc` build and reload
Nginx.

## 10. Release verification

- Gateway login, refresh cookie, all seven route prefixes, permissions,
  upload/download and CSV/XLSX/PDF exports.
- Test roles and business flows using non-demo accounts.
- Verify CORS only allows the production HTTPS origin.
- Verify MySQL/service ports from an external network.
- Verify log access and backup success alerts.
- Rotate secrets/passwords and disable all demo accounts before public launch.

## Release gate status — review required

Current gate is **NO-GO**; nothing in this runbook has been deployed. The
historical Git tree contains a development `password.txt` and a duplicate
demo-password test fixture. Those values are removed from the current tracked
tree but remain in Git history. The local ignored `password.txt` is still on
the workstation. Do not rewrite remote history until the owner approves the
coordinated mirror/filter/force-push/re-clone plan in `DEVELOPMENT_STATUS.md`.

Four development role accounts were reset for the gateway role check; the
known development `admin` account was not reset and remains a blocker. Raw
password-reset tokens are no longer logged. Self-service reset delivery is
unavailable until an email provider is configured.

Regression results: 172 backend tests passed across seven services and
Gateway; frontend production build passed. Gateway route, JWT, permissions,
document download/upload and three export formats were exercised live.
Nginx syntax/external firewall/TLS checks and encrypted backup/isolated
restore were not run on this Windows workstation. The full mutation-heavy
refund/reversal, expense-payment and ownership-transfer scenarios remain
staging gates; see `DEVELOPMENT_STATUS.md` for exact coverage.

Do not begin production deployment until credential rotation, approved
history-cleanup decision, staging restore drill, native Nginx/systemd
validation and the remaining business-flow checks are signed off.
