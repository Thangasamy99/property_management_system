# PCC Production Deployment (Hostinger Ubuntu VPS)

Single-host layout: MySQL + 8 JVM services + Nginx serving the SPA.
No Docker. All services bind loopback; only Nginx 80/443 is public.

## 1. Prepare the host

```bash
sudo apt update && sudo apt install -y openjdk-21-jre mysql-server nginx certbot
sudo useradd -r -s /usr/sbin/nologin pcc
sudo mkdir -p /opt/pcc /etc/pcc /var/lib/pcc/documents /var/log/pcc /var/backups/pcc
sudo chown -R pcc:pcc /var/lib/pcc /var/log/pcc
```

## 2. MySQL (localhost only)

```sql
CREATE USER 'pcc_app'@'127.0.0.1' IDENTIFIED BY '<strong-password>';
CREATE DATABASE pcc_identity_db;   -- repeat for each:
-- pcc_property_db pcc_land_db pcc_finance_db pcc_document_db pcc_workflow_db pcc_reporting_db
GRANT ALL PRIVILEGES ON pcc_*_db.* TO 'pcc_app'@'127.0.0.1';
```
Confirm `bind-address = 127.0.0.1` in `mysqld.cnf`. Flyway migrates on each service's first boot (including Identity V1–V8 seeds + admin).

## 3. Deploy artifacts

```
scp backend/*/target/*.jar pcc@host:/tmp/
scp -r frontend/dist pcc@host:/tmp/pcc-dist
sudo cp /tmp/pcc-*-service-*.jar /opt/pcc/pcc-<name>.jar   # rename per service
sudo cp /tmp/pcc-api-gateway-*.jar /opt/pcc/pcc-gateway.jar
sudo rsync -a /tmp/pcc-dist/ /var/www/pcc/
```

## 4. Configuration

- `sudo cp deploy/.env.production.example /etc/pcc/pcc.env` → edit, `chmod 600`, `chown pcc:pcc`.
- Generate `JWT_SECRET` (≥64 chars): `openssl rand -hex 40`.
- Service DB URLs use `DB_URL_BASE` + per-service name; each service's
  `application.yml` already reads `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`/`JWT_SECRET`
  — for each service set `DB_URL=${DB_URL_BASE}/pcc_<name>_db?...` per service
  (see DEPLOYMENT checklist below for exact env per unit).

## 5. Frontend build

```bash
cd frontend
VITE_GATEWAY_URL=https://pcc.example.com/api npm run build
```
All modules call `${VITE_GATEWAY_URL}/<svc>/api/v1/...` through the gateway.
Dev fallback: `VITE_DIRECT_SERVICES=1` uses direct service ports.

## 6. systemd

```bash
sudo cp deploy/systemd/pcc@.service /etc/systemd/system/
sudo systemctl daemon-reload
for s in identity property land finance document workflow reporting gateway; do
  sudo systemctl enable --now pcc@$s
done
journalctl -u pcc@gateway -f   # logs
```

## 7. Nginx + TLS

```bash
sudo cp deploy/nginx-pcc.conf /etc/nginx/sites-available/pcc
sudo ln -s /etc/nginx/sites-available/pcc /etc/nginx/sites-enabled/
sudo certbot --nginx -d pcc.example.com
sudo nginx -t && sudo systemctl reload nginx
```

## 8. Firewall

```bash
sudo ufw allow 'Nginx Full' && sudo ufw allow OpenSSH && sudo ufw enable
# 8080–8087 and 3306 stay loopback-only — never open them.
```

## 9. Health checks / rollback

- Gateway: `GET /actuator/health`; each service: `GET :808x/api/v1/health` (401 = up).
- Rollback: keep previous jar as `pcc-<name>.jar.prev`, swap back + `systemctl restart pcc@<name>`.
- Backups: `deploy/scripts/backup.sh` via cron nightly; `restore.sh <dir>` for DR.

## Required BEFORE go-live

- Rotate the development `admin`/`po1`/etc. passwords — `password.txt` with
  dev credentials was committed historically (untracked in c5133fb-era commit;
  still in Git history). Rotate JWT_SECRET + all DB + all employee passwords.
- Disable/lock demo employee accounts.
- Set `CORS_ALLOWED_ORIGINS` to the real domain only.
