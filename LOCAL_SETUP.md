# PCC Local Development Setup (Windows)

Local ports only; production profiles/configuration are unchanged.

| Component | URL |
|---|---|
| API Gateway | http://localhost:9000 |
| Property | http://localhost:9001 |
| Identity | http://localhost:9002 |
| Land | http://localhost:9003 |
| Finance | http://localhost:9004 |
| Document | http://localhost:9005 |
| Workflow | http://localhost:9006 |
| Reporting | http://localhost:9007 |
| React/Vite | http://localhost:3000 |

Property Management specifically listens on `9001`. Spring profiles default to
the previous ports outside `local`; the `local` profile uses environment-based
`SERVER_PORT` and service URL values. The `prod` profile is not changed.

## Prerequisites

- Java 21, Node.js/npm, MySQL 8, and PowerShell.
- Existing PCC local databases; setup scripts never create, reset, or delete
  them. MySQL is started if needed, but intentionally left running on stop.
- Create an ignored root `.env.local` containing your local `JWT_SECRET`
  (minimum 64 characters) and `DB_PASSWORD`. Do not copy production secrets.
  Values are read silently by `start-local.ps1`; the file is gitignored.
- Run `npm ci` in `frontend` once. Optionally copy
  `frontend/local.env.example` to `frontend/.env.local`.

## Start, inspect, stop

From the repository root:

```powershell
.\scripts\start-local.ps1
.\scripts\check-local.ps1
.\scripts\stop-local.ps1
```

Start builds all backend executable jars (without running tests), starts
MySQL first, then Identity, Property, Land, Finance, Document, Workflow,
Reporting, Gateway, and Vite. It refuses occupied ports and an already-live
PID manifest rather than starting duplicate processes. It waits for each
`/actuator/health` check and Vite HTTP response. Logs and the process manifest
are stored under ignored `.local/`. Stop terminates only PIDs it recorded and
whose command lines still match the recorded jar/Vite command; it does not
stop MySQL or unrelated Java/Node processes.

`-SkipBuild` may be used after building all modules:
`.\scripts\start-local.ps1 -SkipBuild`.
If a port is occupied, inspect the owner yourself; the scripts never kill a
process just because it is on a PCC port.

## API and CORS

Frontend default: `http://localhost:9000/api`. Direct-service troubleshooting
remains available with `VITE_DIRECT_SERVICES=1` or explicit `VITE_*_API_URL`
values in `frontend/.env.local`. Local backend CORS allows
`http://localhost:3000`. Gateway route paths and `StripPrefix=2` behavior are
unchanged; refresh-cookie rewriting and downstream JWT/permission checks stay
enabled.

Swagger UI when enabled:

- Gateway: `http://localhost:9000/swagger-ui.html`
- Property: `http://localhost:9001/swagger-ui.html`
- Identity: `http://localhost:9002/swagger-ui.html`
- Land/Finance/Document/Workflow/Reporting: corresponding port `9003`–`9007`.

Health checks use `/actuator/health` on each backend and Gateway. Gateway
health can also be checked at `http://localhost:9000/actuator/health`.

## Verification

Run each backend suite with its module's `mvnw.cmd test`, then
`npm run build` in `frontend`. Login/refresh and permissions should be tested
through `http://localhost:9000`; never put credentials in command-line
arguments or checked-in files. Port changes can be made with `SERVER_PORT`,
`*_SERVICE_URL`, `CORS_ALLOWED_ORIGINS`, or `VITE_DEV_PORT` environment values.
