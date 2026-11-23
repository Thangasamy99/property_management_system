# PCC Cameroon – Development Status

## Phase 1–2: Identity Service + Admin Panel — COMPLETE

- `pcc-identity-service` (:8081, `pcc_identity_db`): auth, refresh tokens, users, roles, permissions, login history, audit logs, stats.
- Admin Panel (React/Vite :5173): dashboard, employees, roles & permissions, login history, audit logs, profile.
- Tests: 60/60 passing.

## Phase 4: Property Service — COMPLETE

`pcc-property-service` (:8082, `pcc_property_db`) — independent microservice validating identity-issued JWTs.

### Migrations
- `V1__create_property_schema.sql` — 12 tables: properties, property_addresses, property_status_history, buildings, apartments, tenants, rental_agreements, rental_agreement_terms, rent_schedules, rental_agreement_history, maintenance_requests, maintenance_history.
- Identity `V3` — seeds BUILDING_*/APARTMENT_* permissions; grants to ADMIN, MANAGER (view), ACCOUNTANT (view), PROPERTY_OFFICER (full).

### APIs (`/api/v1`)
- `/properties` — CRUD, search/type/status filters, status transitions (recorded), soft delete.
- `/buildings` — CRUD, filter by property, `by-property/{id}`.
- `/apartments` — CRUD, filter by building/status, unique number per building, `by-building/{id}`.
- `/tenants` — CRUD, search by name/code/email.
- `/rental-agreements` — create (auto AGR-number, overlap rejection, apartment→OCCUPIED), renew, terminate (apartment→AVAILABLE, future schedules cancelled), `/schedules`, `/history`.
- `/maintenance` — create (apartment or building target), status/assignee updates, `/history`.

Code sequences: PROP-*, BLD-*, TNT-*, AGR-*, MNT-*.

### Authorization
`@PreAuthorize` on every endpoint; authorities read from JWT claims — no identity DB access.
PROPERTY_VIEW/CREATE/UPDATE/DELETE · BUILDING_VIEW/CREATE/UPDATE · APARTMENT_VIEW/CREATE/UPDATE · TENANT_VIEW/MANAGE · RENTAL_VIEW/MANAGE · MAINTENANCE_VIEW/MANAGE.

### Tests
`PropertyServiceIntegrationTest` — 13/13 passing: auth rejections, permission 403s, code generation, duplicate checks, validation errors, schedule generation (12 monthly rows), overlap rejection, termination flow, maintenance lifecycle.

### Frontend
New `src/pages/property/` module (6 screens) + shared `components/DataTable.jsx`:
- Properties (search/filter, create/edit, details, status, archive)
- Buildings, Apartments, Tenants (list + create/edit modals)
- Rentals (create agreement, details modal with rent schedule + history tabs, renew, terminate)
- Maintenance (create request, status transitions, history)
- `services/api.js` — `propertyFetch` against `VITE_PROPERTY_API_URL` (default :8082), same in-memory token + refresh flow.
- Nav/routes permission-gated; sidebar "Add New" and "+" actions deep-link to `?new=1` dialogs.

## Phase 5: Land Service — COMPLETE

`pcc-land-service` (:8083, `pcc_land_db`) — independent microservice validating identity-issued JWTs (same contract as Property Service).

### Migrations
- Land `V1` — 10 tables: land_parcels, land_status_history, land_surveys (N/S/E/W boundary columns), land_coordinates, land_ownership, land_ownership_history (event log), land_acquisitions, land_sales, land_transfers, land_transactions (unified ledger). Spec's land_boundaries/sale_details/transfer_details consolidated to avoid duplication.
- Identity `V4` — seeds LAND_SURVEY_VIEW/CREATE/UPDATE, LAND_OWNERSHIP_VIEW/CREATE, LAND_ACQUISITION_CREATE/APPROVE, LAND_REPORT_VIEW. Reuses existing LAND_SALE_REQUEST (sale+transfer create) and LAND_SALE_APPROVE (approvals).

### APIs (`/api/v1`)
- `/land-parcels` — register (auto LND-#### ref, unit→m² conversion), search/classification/status/region filters, status transitions w/ history, `/summary`, soft delete.
- `/land-surveys` — record (SRV-####), list by parcel or global search, update.
- `/land-coordinates` — per-parcel list, atomic replace with lat/lng validation + duplicate-sequence rejection.
- `/land-ownership` — register (OWN-####), current/ended tracking, `/history/{parcelId}` event log.
- `/land-acquisitions` — create (ACQ-####, PENDING), approve/complete (approver only), cancel.
- `/land-sales` — create (SAL-####, one active sale per parcel enforced), submit→review→approve/reject→progress (PAYMENT_PENDING→LEGAL_PROCESSING→COMPLETED), cancel; parcel status driven (UNDER_SALE→SOLD/AVAILABLE).
- `/land-transfers` — create (TRF-####, requires CURRENT ownership, optional sale link gated to LEGAL_PROCESSING/COMPLETED), submit→approve/reject→complete; completion atomically ends current ownership + opens new record + writes TRANSFERRED_OUT/IN events.
- `/land-transactions` — immutable ledger of every business event (type, ref, amount, actor).

### Authorization
`@PreAuthorize` on every endpoint. LAND_OFFICER registers/requests; MANAGER approves (LAND_SALE_APPROVE, LAND_ACQUISITION_APPROVE); ACCOUNTANT read-only; ADMIN full.

### Tests
`LandServiceIntegrationTest` — 12/12 passing: 401/403 rejections, unit conversion (2.5 ha→25 000 m²), invalid area/coordinate 400s, survey + ownership refs, acquisition approve 403 for officer + lifecycle, conflicting sale 422, sale workflow driving parcel status, transfer completion preserving 2 ownership records (CURRENT/ENDED) + history events, ledger + summary.

### Frontend (`src/pages/land/` — 8 screens + ParcelSelect + landShared)
- Land Overview (summary cards, parcels-by-status, recent ledger activity)
- Land Parcels (register/edit/status/archive + detail modal with tabs: details, status history, surveys, coordinate editor, ownership, transactions)
- Surveys, Ownership (+history), Acquisitions (approve/complete/cancel), Land Sales (full workflow actions), Transfers (+legal completion modal), Transactions ledger
- `landFetch` (:8083, VITE_LAND_API_URL); permission-gated nav/routes; Add-New deep links for parcel + sale forms.
- Verified live with `lo1` (LAND_OFFICER): LND-0002, SRV-0001, OWN-0001, ACQ-0001, SAL-0001; approve 403, conflict 422.

## Phase 6: Finance Service — COMPLETE

`pcc-finance-service` (:8084, `pcc_finance_db`) — independent microservice validating identity-issued JWTs (same contract). XAF currency. Finance owns payments, allocations, receipts, refunds, ledger and accounts; Property/Land remain source of truth for their own records — integration is HTTP-only.

### Migrations
- Finance `V1` — 11 tables: finance_accounts, payment_methods, payment_targets, payments, payment_allocations, receipts, other_income, expenses, refunds, ledger_entries, audit_logs.
- Identity `V5` — seeds finance permissions: FINANCE_DASHBOARD_VIEW, RENT_COLLECTION_VIEW/CREATE/APPROVE, LAND_PAYMENT_VIEW/CREATE/APPROVE/SYNC, INCOME_VIEW/CREATE/APPROVE, EXPENSE_PAY, RECEIPT_VIEW/PRINT, REFUND_VIEW/CREATE/APPROVE, LEDGER_VIEW/ADJUST, CASH_ACCOUNT_VIEW/MANAGE + cross-module view grants (RENTAL_VIEW, TENANT_VIEW, LAND_VIEW, LAND_OWNERSHIP_VIEW, LAND_REPORT_VIEW for ACCOUNTANT; PROPERTY_VIEW for officers).
- Property `V2` — `rent_schedules.paid_amount` column for Finance sync.
- Land `V2` — `paid_amount` + `finance_status` columns on `land_acquisitions` and `land_sales`.

### APIs (`/api/v1`) — 13 controllers
- `/dashboard/summary` — real totals: collections/expenses today+month, outstanding rent, land paid/receipts, cash/bank balances, 6-month income-vs-expense series, recent transactions.
- `/rent-collections` — `GET /payments` (search/status/date-range, paged), `GET /targets/{scheduleId}` (outstanding), `POST /payments` (full/partial, idempotent), `POST /payments/{id}/post` (approver).
- `/land-acquisition-payments`, `/land-sale-receipts` — same payment pattern against Land records.
- `/other-income` — create (RECORDED) → approve&post / reject.
- `/expenses` — create (PENDING_APPROVAL) → approve/reject → `/pay` (blocked until APPROVED) / cancel.
- `/payment-methods`, `/receipts` (list/get/by-payment, unique RCP-####, amount-in-words), `/refunds` (REQUESTED→approve posts, refundable-amount validated), `/reversals` (balancing entry, original preserved).
- `/ledger` — paged/filtered entries (LED-####, CREDIT/DEBIT, type, source ref, method, posting status); `/ledger/adjustments`.
- `/cash-accounts`, `/bank-accounts`, `/accounts/{id}` (+create/update/toggle).
- `/reports` — daily-collection, monthly-collection, collections, rent-arrears, expenses, income-vs-expense, cashbook, payment-method-summary (all real data, date-range filters).

### Integration contracts (HTTP only, token relayed)
- Finance → Property `GET /rental-agreements/{id}/schedules` (verify at record time) and `POST /rent-schedules/{id}/payment` `{paidAmount}` (sync at post time; Property recomputes PENDING/PARTIAL/PAID).
- Finance → Land `PATCH /land-acquisitions/{id}/finance-status` and `PATCH /land-sales/{id}/finance-status` `{paidAmount, financeStatus}` (sync at post time).
- Sync endpoints require `RENT_SCHEDULE_PAYMENT_SYNC` / `LAND_PAYMENT_SYNC`; caller's JWT is forwarded. External-service failure → 503 `ExternalServiceException`.

### Authorization
ACCOUNTANT: create/record + post-payment methods, expenses, income, refunds, ledger/reports views. MANAGER: approval permissions (RENT_COLLECTION/LAND_PAYMENT/EXPENSE/INCOME/REFUND_APPROVE, LEDGER_ADJUST). Officers: view + create in their domain. ADMIN: all. Verified live: acc1 post→403, expense self-approve→403, unapproved pay→422.

### Tests
`FinanceServiceIntegrationTest` — 17/17: 401/403, partial+full allocation, overpayment 422, idempotency, concurrent payment, expense approval/reject/unapproved-pay, refund validation, reversal preserves original, receipt uniqueness, ledger entries, invalid external refs, service-unavailable 503, dashboard totals, date-range reports.

### Frontend (`src/pages/finance/` — 11 screens + financeShared)
- Dashboard (stat cards, income-vs-expense bars, recent payments), Rent Collection (agreement search via Property API → schedule cards → record/post), Land Acquisition Payments + Land Sale Receipts (target cards w/ finance status → record/post), Other Income, Expenses (approve/reject/pay workflow), Receipts (printable detail), Refunds (payment-ref→receipt lookup, approve/reject), Financial Ledger (filters), Cashbook (per-account movements + balance), Finance Reports (5 real reports + CSV export).
- `financeFetch` (:8084, `VITE_FINANCE_API_URL`), `RecordPaymentModal`, XAF formatting, permission-gated nav/routes, `?new=1` deep links.

### Live E2E (real `acc1` ACCOUNTANT JWT)
- Rent: AGR-0001 schedule 1 → partial 60,000 (idempotent replay → same PAY-0001) + 90,000 → posted by admin → Property synced paidAmount=150,000 status PAID; overpayment 422.
- Land: ACQ-0001 approved → 5,000,000 posted → Land synced PARTIALLY_PAID; SAL-0001 approved → 10,000,000 receipt → synced PARTIALLY_PAID.
- Expense EXP-0001 → approve → paid (PAY-0005). Income INC-0001 → approved → posted. Refund RFD-0001 (30,000) validated + posted. PAY-0002 reversed → balancing DEBIT LED-0008, original preserved.
- Dashboard/reports/ledger/receipts all return real persisted data.

### Known limitations
- ~~Refund reduces Finance's target paidAmount, but the refunded amount is not pushed back to Property schedule paidAmount~~ — FIXED (commit `3ce0ca4`): Finance V2 `payment_allocations.allocation_ref`/`reversed_amount` + `sync_outbox` (persisted with posting tx, confirmed-success marking, 60s retry, manual retry/reconcile endpoints); Property V3 `rent_schedule_payments`/`_payment_reversals` (`POST /rent-schedules/{id}/payments` + `/payment-reversals`); Land V3 `land_finance_payments`/`_payment_reversals` (same contract on acquisitions + sales). Scheduled/startup dispatch without a relayable JWT stays PENDING until a request-driven or manual retry.
- Account balances only move when a payment specifies `accountId`; payments posted without an account keep balances at 0 (explicit, by design).
- Advance-rent overpayment workflow not implemented (rejected 422, per spec default).
- Receipts/print is browser `window.print()`; PDF generation deferred to Document Service.

## Phase 7: Document Service — COMPLETE
`pcc-document-service` (:8085, `pcc_document_db`) — independent microservice, same JWT contract. Multipart upload with magic-byte validation, private local filesystem storage behind an abstraction (S3-compatible replaceable), external-ref associations over relayed-JWT HTTP — no cross-service DB access.

### Migrations
- Document `V1` — `document_categories` (21 seeded across PROPERTY/LAND/FINANCE/ADMINISTRATION), `documents` (unique DOC-###### ref), `document_versions` (UUID storage_key, unique per doc+version), `document_associations` (unique doc+type+ref), `document_audit_logs`.
- Identity `V6` — 14 `DOCUMENT_*` permissions; ADMIN all; MANAGER operational + archive/restore + audit; ACCOUNTANT finance-domain ops; PROPERTY/LAND_OFFICER domain-scoped upload/view/version/associate (no archive, no category manage, no audit).

### APIs (`/api/v1`)
- `/documents` — multipart upload (category, description, documentDate, expiryDate, optional `associations` JSON param); GET search (status/categoryId/domain/expired/q/date-range, paging+sort); `GET|PUT /{id}`.
- `/documents/{id}/versions` (POST new, GET history), `/download?version=n`, `/preview?version=n` (attachment vs inline; PDF+images previewable), `/archive`, `/restore`.
- `/documents/{id}/associations` (GET/POST/DELETE), `/audit-history`, `/for-entity?type&ref`, `/expiry?bucket=expired|30|60|none`, `/expiry/summary`, `/categories` (GET; POST/PUT = CATEGORY_MANAGE), `/dashboard`.

### Storage + validation
- `DocumentStorage` abstraction; `LocalDocumentStorage` writes `root/<2-char prefix>/<uuid>` via tmp + ATOMIC_MOVE, cleans partial files, whitelists keys (path-traversal proof). Magic bytes verified: PDF/JPEG/PNG directly; OLE2 (.doc/.xls) and ZIP (.docx/.xlsx) disambiguated by extension. Stored file verified before DB commit; deleted on failed insert. `DOC_STORAGE_ROOT`, `DOC_MAX_FILE_SIZE`, `DOC_ALLOWED_MIME` env-configurable.
- `ExternalEntityValidator` validates external refs over relayed-JWT GETs to Property/Land/Finance where a lookup exists; `OTHER`/`LAND_TRANSFER` are free-form. Associations store external refs only — no cross-DB FKs.

### Tests
`DocumentServiceIntegrationTest` — 21/21: seeded categories, 401/403 perms, upload success, invalid type, content mismatch, oversize, search/pagination, version upload/history/v1-download, download/preview perms, association create/dup/unknown-ref/for-entity, archive+restore (archived-version blocked), expiry summary+buckets, audit actions, path-traversal filename, dashboard. Regression: Identity 60/60, Property 23/23, Land 16/16, Finance 22/22.

### Frontend (`src/pages/documents/` — 9 screens)
Dashboard, All Documents, Property/Land/Finance Documents (domain-filtered `DocumentsPage`), Categories (admin manage), Expiring (bucket cards), Archived (restore), Document Detail (Details/Versions/Associations/Audit tabs). `UploadModal` (drag-drop + category + metadata + inline association), `EntityDocumentsPanel` embedded in Properties view modal, Rentals agreement details, Land parcel detail. `docFetch`/`docUpload` share the JWT + refresh contract; nav permission-gated.

### Live E2E (real admin + po1 PROPERTY_OFFICER JWTs)
- DOC-000001 uploaded with a PROPERTY association validated live against :8082; download byte-identical; v2 uploaded → v1 download preserved.
- Unknown PROPERTY/LAND_PARCEL refs → 400 (relayed-JWT lookup); unauthenticated download → 401; fake-PDF content and .exe → 422; po1 archive/category-manage → 403 while upload/view → 200; expiry-required category → 422; expiry buckets + dashboard verified.

### Known limitations
- Local filesystem only (S3 adapter designed-for, not implemented); preview inline only for PDF/images; expiry list endpoints unpaginated.
- `LAND_TRANSFER`/`OTHER` refs unvalidated (no lookup endpoint yet); receipt PDF generation still browser print.

### Run
`SERVER_PORT=8085 DB_URL=... DB_USERNAME=... DB_PASSWORD=... JWT_SECRET=... DOC_STORAGE_ROOT=/data/pcc-docs DOC_MAX_FILE_SIZE=26214400 PROPERTY_SERVICE_URL=... LAND_SERVICE_URL=... FINANCE_SERVICE_URL=... java -jar pcc-document-service.jar` — storage root must be private (outside static/public dirs).

## Phase 8: Workflow & Approval Management Service — COMPLETE
`pcc-workflow-service` (:8086, `pcc_workflow_db`) — independent microservice, same JWT contract. Configurable multi-level approval workflows across Property/Land/Finance/Document, sequential levels, delegation, notifications, durable integration outbox — owning services remain the source of truth; `COMPLETED` only after confirmed downstream success.

### Migrations
- Workflow `V1` — `workflow_definitions` (9 seeded) + `workflow_levels`, `workflow_instances` (unique WF-###### ref, optimistic `@Version`), `workflow_approval_tasks` (unique instance+level+cycle; cycle supports return→resubmit), `workflow_actions`, `workflow_delegations`, `workflow_audit_logs`, `notifications` (recipient_id or recipient_role), `workflow_integration_outbox`.
- Identity `V7` — 13 `WORKFLOW_*` + `NOTIFICATION_VIEW` permissions. ADMIN all; MANAGER approve/reject/return/delegate/report; ACCOUNTANT approve only (finance scopes); PROPERTY/LAND_OFFICER submit/cancel/view — no approve/reject/return/configure/delegate.

### APIs (`/api/v1`)
- `/workflows` — submit (definitionId, entityRef, amount, priority, description, documentRefs); GET search w/ status/module/type/submitter/priority/date/q + paging/sort; `/mine`, `/pending-approvals`, `/overdue`, `/dashboard`.
- `/workflows/{id}/approve|reject|return|comment|resubmit|cancel` — reject/return require reason; only current-level assignee (direct, role, or in-scope delegate); no skipping; no duplicates; no self-approval; pessimistic state checks + `@Version`.
- `/workflows/{id}/tasks|history|audit|integrations`; `POST /workflows/integrations/{outboxId}/retry`.
- `/workflow-definitions` — GET (module filter) for WORKFLOW_VIEW; POST/PUT for WORKFLOW_CONFIGURE; levels: order, name, approverRole or approverId, minAmount threshold, mandatory.
- `/delegations` — scoped temp delegation (module/definitionId, start/end dates, reason); `/notifications` (+ `/unread-count`, `/{id}/read`, `/read-all`) — user + role-targeted visibility.

### Integration outbox
- On final approval: outbox row persisted in the same tx (integration_ref = WF-######-I1), then best-effort dispatch relays the approver's JWT to the owning service (`{id}` path substitution; JSON payload: workflowRef/entityRef/approvedBy/source).
- Success → instance COMPLETED; 4xx (except 408/429) → PERMANENT → INTEGRATION_FAILED; transient/5xx → FAILED + retry_count → scheduled sweep re-queues, manual retry relays the retrier's JWT (overrides PERMANENT). SUCCESS rows short-circuit (idempotent).
- Seeded definitions wire real endpoints: EXPENSE/REFUND → Finance `/expenses/{id}/approve` `/refunds/{id}/approve`; LAND_ACQ/SALE → Land `/land-acquisitions|land-sales/{id}/approve`; PROP_REGISTRATION → Property `PATCH /properties/{id}/status`. RENTAL_*, LAND_TRANSFER, DOC_VERIFICATION have no integration (approval record only).

### Tests
`WorkflowServiceIntegrationTest` — 17/17: seeded defs, configure 403/200, submit+task, self/unauthorized/wrong-role approver denied, sequential multi-level (no skipping, duplicate blocked), reject-reason required, return→resubmit (task cycle), notifications, integration dispatch + failure→manual-retry→COMPLETED, delegation + date validation, history/audit, search/pagination.

### Frontend (`src/pages/workflows/` — 9 screens + SubmitModal)
Dashboard (stat cards + pending list), All/Mine/Pending/Overdue/Returned/History (`RequestsPage` modes), Definitions (level editor modal), Detail (timeline, actions w/ mandatory reasons, integrations + retry, action history), Delegations, Notifications (unread, mark-read). `wfFetch` (:8086, `VITE_WORKFLOW_API_URL`) shares the auth contract; nav/routes permission-gated.

### Live E2E (real po1/lo1/acc1/mgr1/admin JWTs)
- po1 submits WF-000001 (expense EXP-0002) → MANAGER task + mgr1 unread notification; lo1 approve → 403; mgr1 approves → Finance `/expenses/2/approve` invoked w/ mgr1's JWT → expense APPROVED (approvedBy mgr1) → workflow COMPLETED; outbox SUCCESS, history preserved.
- Land: acquisition ACQ-0002 → WF-000002 → mgr1 approve → dispatch failed (500: text/plain body bug, fixed to application/json) → manual retry → acquisition APPROVED → COMPLETED.
- Failure: expense 999 → approve → downstream 404 → PERMANENT → INTEGRATION_FAILED w/ error; manual retry path verified.
- Delegation: mgr1→acc1 (FINANCE scope) — acc1 approved po1's MANAGER-level task (actedBy=acc1, delegated_from tracked); acc1's own submission still blocked by self-approval guard (403). Delegate's downstream call 403 (owns no EXPENSE_APPROVE) → PERMANENT → mgr1 retry → SUCCESS → COMPLETED.
- ADMIN created INCOME_APPROVAL live; po1 definition-create → 403.

### Known limitations
- Integration payload is generic (workflowRef/entityRef/approvedBy); endpoints needing business-specific bodies (e.g. Property `PATCH /properties/{id}/status` wants {status,reason}) will reject — add per-definition payload templates if those paths are activated.
- Scheduled retry cannot dispatch without a relayable JWT — offline failures wait for request-driven/manual retry (no service-to-service client credential yet).
- Delegation on role-based levels can't verify the delegator actually held the role (no cross-service user lookup) — scope is enforced by module/definition + delegate's own WORKFLOW_APPROVE permission.
- Escalation config = due-date derived from escalationDays; auto-reassignment to a higher level not implemented.

### Run
`SERVER_PORT=8086 DB_URL=... DB_USERNAME=... DB_PASSWORD=... JWT_SECRET=... PROPERTY_SERVICE_URL=... LAND_SERVICE_URL=... FINANCE_SERVICE_URL=... DOCUMENT_SERVICE_URL=... java -jar pcc-workflow-service.jar`

## Outstanding / next phases
- Phase 3 API Gateway (:8080) — deferred; direct service URLs in dev.
- Phase 9+ Reporting service — not started.
- `building_land_parcels` link table — deferred until Property↔Land integration requirements are defined.
- GIS map visualization — coordinates stored and validated; map UI deferred.

---

## Phase 7 — Reporting & Analytics Service (`pcc-reporting-service` :8087)

Independent aggregator: relays the caller JWT to owning services over HTTP; never touches another service's database. Own DB `pcc_reporting_db` (Flyway V1: `report_definitions`, `report_cache_entries`, `report_execution_logs`, `report_export_logs`, `scheduled_report_configs`).

**Aggregation layer** (`SourceGateway`): configurable per-service base URLs, connect/read timeouts, bounded parallelism (4 workers), JWT relay captured on the request thread, auto-pagination capped at 1000 rows, every result carries service/status/fetchedAt/error/cached. Down or failed sources are reported as `DOWN`/`PARTIAL` — never silently zero-filled.

**Dashboards** `/api/v1/dashboard/{executive,property,land,finance,workflow,documents}`: consolidated KPIs + per-source health block; period selector (today/week/month/quarter/year/custom) passed through to Finance endpoints. Executive = 10 parallel calls + paged property lists; portfolio totals are lifetime, periodCollections is period-scoped.

**Reports**: `ReportRegistry` defines ~30 report types across PROPERTY/LAND/FINANCE/WORKFLOW/DOCUMENT/IDENTITY, each mapped to a real owning-service endpoint (reuses Finance's own report endpoints — no duplicate computation) with column projection + automatic numeric/money totals. `/reports/{domain}/{key}` runs them; `/reports/definitions` returns only what the caller's permissions allow; `/reports/{key}/export?format=CSV|XLSX|PDF` (Apache POI + OpenPDF, PCC header, filters, totals, row cap noted); `/reports/refresh` (REPORT_REFRESH) clears cache; `/reports/execution-history` pages the audit log.

**Permissions** — Identity V8: new `REPORTING_VIEW, DASHBOARD_VIEW, PROPERTY_REPORT_VIEW, DOCUMENT_REPORT_VIEW, EMPLOYEE_REPORT_VIEW, AUDIT_REPORT_VIEW, REPORT_EXPORT, REPORT_REFRESH, REPORT_CONFIGURE`; reuses `LAND_REPORT_VIEW` (V4), `WORKFLOW_REPORT_VIEW` (V7), `FINANCIAL_REPORT_VIEW` (V2). Officers get their domain only; ACCOUNTANT finance/workflow/document; MANAGER all operational + export/refresh; ADMIN everything. Per-report permission enforced server-side in addition to menu filtering.

**Caching**: DB-backed `report_cache_entries` w/ configurable TTL (`pcc.reporting.cache-ttl-seconds`, default 120s); freshness timestamps always surfaced; POST /reports/refresh invalidates.

**Frontend** (`src/pages/reporting/`): `rpFetch`/`rpDownload` on :8087, 13 permission-gated screens via 3 reusable components (KPI cards, pure-SVG bar charts, period picker, source-health banner, generic report runner with filters+exports+paging, execution history) + 14 nav entries.

**Tests**: 7/7 — definitions seeding + permission filtering, rows+totals, least-privilege 403/401, DOWN-source health, CSV/XLSX/PDF generation, refresh permission, execution logging. Vite build clean.

**Live verification**: executive dashboard pulled real data from all 5 services (1 property/1 apt 100% occupied/1 active agreement; 2 parcels 8700sqm; collections 15,250,000 XAF; 1 pending/3 completed workflow; 3 docs). PROPERTY_OFFICER live: property dashboard 200, finance 403, refresh 403, definitions filtered. Exports verified as valid CSV/XLSX/PDF files. Downstream outage confirmed via visible DOWN source entries.

**Limitations**: employee/audit reports depend on what Identity exposes (audit-logs endpoint only — login trends limited); page-size cap 1000 rows; cache applies to dashboards not individual report runs; PDF covers full row set w/ truncation notice.

Run: `SERVER_PORT=8087 DB_URL=... DB_PASSWORD=... JWT_SECRET=... java -jar pcc-reporting-service.jar` (all six service URLs configurable via `pcc.reporting.*-url`).

---

## Phase 8 — API Gateway & Production Readiness

**`pcc-api-gateway`** (:8080) — Spring Cloud Gateway (Boot 4.1.2, SC 2025.1.0 w/ compatibility-verifier disabled; SC renamed config namespace to `spring.cloud.gateway.server.webflux.*`).

- Routes: `/api/{identity,property,land,finance,documents,workflows,reporting}/**` → `StripPrefix=2` → owning service. Env-configurable upstreams.
- `JwtValidationFilter` (global, order -100): validates HS512 signature + issuer + audience + exp + `type=access`; public paths = identity login/refresh + `/actuator`. Authorization header forwarded untouched — downstream permission checks still apply.
- `RewriteResponseHeader` rescopes the refresh cookie `Path=/api/v1/auth` → `/api/identity/api/v1/auth` so HttpOnly refresh works through the prefix.
- CORS (env-configurable origin, credentials allowed, Content-Disposition exposed for downloads), 30MB upload cap, 5s connect/60s response timeouts, JSON 401 errors, `/actuator/health`.

**Frontend**: single entry point — `VITE_GATEWAY_URL` (default `http://localhost:8080/api`); `VITE_DIRECT_SERVICES=1` or per-service `VITE_*_API_URL` bypass for troubleshooting. Auth + refresh + uploads + exports all preserved.

**Gateway E2E (live)**: login+refresh via gateway 200; all 7 route prefixes 200; unauth 401; po1 finance 403/property 200; POST property 201; multipart upload → DOC-000004; PDF export valid; workflow definitions 200; audit-logs 200; unknown path 404.

**Security review**: `password.txt` (contained dev admin/officer credentials) removed from tracking + gitignore'd (`*.env`, `*.pem`, `*.key`, `secrets/`); still present in history → **all dev passwords + JWT_SECRET + DB creds must be rotated before go-live**. Remaining known risks documented in deploy/DEPLOYMENT.md §9.

**Deployment** (`deploy/`): production env template, `pcc@.service` systemd template (hardened: NoNewPrivileges, ProtectSystem), Nginx SPA+TLS config, backup/restore scripts (7 DBs + document store, 14-day retention), full runbook in `deploy/DEPLOYMENT.md`. Loopback-only MySQL + services; only Nginx public.

## Final release checklist

- [x] 7 backend services + gateway running
- [x] Frontend → gateway (build clean)
- [x] Auth/refresh/authorization correct through gateway
- [x] Upload/download + exports through gateway
- [x] Reporting live-aggregates real data w/ source health
- [x] Regression suites on all services post-gateway (172/172; isolated test databases reset before rerun)
- [ ] Production secrets rotated (see §9)
- [ ] VPS deployment executed (pending review)

## Phase 9 — Final Pre-Production Security Gate (2026-11-22)

**Decision: NO-GO for production deployment.** This pass did not deploy to or modify production. Secret rotation, external Nginx validation, and a real encrypted backup/restore drill remain release blockers.

### Repository and credentials

- `password.txt` contained known development admin and PROPERTY_OFFICER credentials. It was removed from the tracked tree in `440ac14` and remains ignored by `.gitignore`; the local ignored file is still present and was not deleted.
- Git history still contains `password.txt` and the former test fixture using the PROPERTY_OFFICER password (historical commits `467d3f7`, `3aa41ce`, and `440ac14`). Treat both credentials as compromised. A separate commit removed the test fixture from the current working tree. No remote history rewrite or force-push was performed.
- The development DB credential and development JWT value were not found in tracked Git history/current tracked files. Local `.env` files remain ignored and contain development configuration; rotate those values before reuse outside this workstation.
- The tracked tree no longer contains the exposed `password.txt` values. `.gitignore` covers local `.env`, secret/key files and credentials, while explicitly allowing the non-secret `deploy/.env.production.example`.
- Identity bootstrap no longer writes generated passwords to logs; `prod` profile refuses initial bootstrap without `PCC_ADMIN_PASSWORD`. Local logs checked for an initial-admin bootstrap block; none was present.
- The security review found Identity's forgot-password path logged raw reset tokens. That log was removed; current behavior logs only a generic delivery-not-configured warning. Local application logs contained neither an initial-admin bootstrap block nor the old raw-reset-token log signature. Self-service reset email is not configured; use the authorized admin reset flow until mail delivery is integrated.
- Development `po1`, `lo1`, `acc1`, and `mgr1` accounts were reset through Identity; the generated temporary values were not printed or saved. Live logins and permissions were checked. The known development `admin` credential remains unchanged and is a blocker; it must be rotated before release.
- Do not rewrite history without approval. Safe cleanup plan: rotate credentials first; freeze pushes; create an access-restricted mirror backup; use `git-filter-repo` to remove `password.txt` and replace the historical test fixture on every branch/tag; inspect the rewritten mirror and run secret scanning; coordinate a one-time force-push only after explicit approval; require all collaborators to re-clone and review forks/caches. History cleanup was not performed.

### Production hardening

- Added `application-prod.yml` for all seven services and Gateway. Production requires explicit DB URL/user/password, JWT secret/issuer/audience, CORS origin, service URLs and `SERVER_ADDRESS`; there are no production localhost fallbacks. Local profiles retain their dev defaults.
- Production binds services to `127.0.0.1` via `SERVER_ADDRESS`; Gateway is behind Nginx. `deploy/.env.production.example` contains placeholders only. Each app receives a unique database account with privileges scoped to its schema; backup and restore accounts are separate.
- Gateway allows only exact Identity login/refresh paths and `/actuator/health` without a JWT. `/actuator/gateway/**` now requires auth; route metadata is not public. Authorization is forwarded and downstream authorization remains enforced.
- `deploy/nginx-pcc.conf` adds TLS 1.2/1.3, security headers, explicit Authorization forwarding, ACME webroot challenge handling, 30MB request limit, SPA fallback and Gateway-only proxy. Added HTTP bootstrap site to obtain the first certificate.
- systemd uses a shared protected environment file plus a per-instance file, loopback binding, restricted writes, automatic restart. Added encrypted backup timer/service and isolated, non-overwriting restore script. Scripts pass `bash -n`.
- Production profile YAML for all eight JVM applications parsed successfully. Local Nginx/systemd are unavailable on this Windows workstation, so their native config validators were not run. `age` and production backup/restore credentials are unavailable; the encrypted backup script and separate-schema restore drill were not executed. No production DB or document data was touched.

### Regression results

| Component | Tests | Failures / errors | Result |
|---|---:|---:|---|
| Identity | 60 | 0 / 0 | PASS |
| Property | 23 | 0 / 0 | PASS |
| Land | 16 | 0 / 0 | PASS |
| Finance | 22 | 0 / 0 | PASS |
| Document | 21 | 0 / 0 | PASS |
| Workflow | 17 | 0 / 0 | PASS |
| Reporting | 7 | 0 / 0 | PASS |
| API Gateway | 6 | 0 / 0 | PASS |
| Frontend production build | — | — | PASS |

**Automated total: 172 tests, 0 failures, 0 errors.** Existing test databases had accumulated state; only isolated `*_test_db` schemas were dropped/recreated before successful regression reruns.

### Gateway live verification

- Login and refresh cookie through Gateway: 200/200. All seven service route prefixes returned successful responses; unauthenticated request 401.
- Role checks with real Identity JWTs: ADMIN all tested domain dashboards 200; MANAGER property/finance 200; ACCOUNTANT finance 200/property 403; PROPERTY_OFFICER property 200/finance 403; LAND_OFFICER land 200/finance 403.
- Property create returned 201. Agreement schedule, rent collection, refunds, Finance sync-record list, expenses, acquisition, sale, transfer, workflow history/definitions, documents and Reporting dashboard routes were exercised with 200 responses.
- Existing local Finance data: 12/12 sync records SUCCESS; 4 refunds POSTED; expenses included APPROVED and PAID records. This confirms existing synchronized state is readable, but this gate did not create a new reversal or re-run the full refund/reversal workflow through Gateway.
- Report-by-report probing found several Finance report definitions initially returned DOWN (a wrong payments path, required date filters, and non-Page DTO shapes). The registry/aggregation code was corrected and the Reporting suite passed again, but the updated service was not repackaged and live-retested; Finance report correctness remains a release blocker.
- Document multipart upload succeeded for a test PDF; download returned 200, 47 bytes, and Content-Disposition; version history returned 200. Unsupported text upload returned 422 as expected.
- CSV/XLSX/PDF export through Gateway returned 200, correct file signatures and Content-Disposition. CORS preflight echoed only the configured localhost development origin. Unknown entity returned 404.
- A test property and test document were created in local development data; these were not production records. Ownership-transfer list was empty, so a full transfer lifecycle was not verified. Full workflow submission/approval, rent collection/refund/reversal, expense-payment, and ownership-transfer mutations were not repeated in this gate; prior phase tests/live checks remain the evidence for those service-owned flows.
- Nginx itself is not installed locally, so multipart/download through Nginx and external firewall/TLS behavior remain unverified.

### Release blockers

1. Rotate JWT signing secret, DB credentials, and all employee credentials in the production secret manager. Revoke Identity refresh sessions and coordinate a full stop/start so old access JWTs are rejected. Reset/disable demo accounts; do not reuse the development `admin` credential.
2. Decide and approve Git history cleanup; do not force-push until the repository owner approves.
3. Rotate/disable the development `admin` account; the other four demo roles were reset for this gate. Self-service password-reset email is unavailable, so validate the administrator recovery path.
4. Install the release configuration on a staging VPS, validate with `nginx -t` and systemd, run encrypted backup, then restore into separate empty `*_restore_test` schemas and isolated document storage.
5. Run the remaining mutation-heavy Gateway scenarios in staging (refund/reversal reconciliation, expense approval+payment, ownership transfer) and verify the results against each owning service before production.
6. Validate public firewall exposure and certificate renewal from an external host.

**Deployment remains paused for final release/deployment review.**

## Local Development Port Standardization (2026-11-23)

Local application defaults and local-only profiles now use Gateway `9000`, Property `9001`, Identity `9002`,
Land `9003`, Finance `9004`, Document `9005`, Workflow `9006`, Reporting
`9007`, and Vite `3000`. Application endpoint paths and DB names are unchanged;
production `application-prod.yml` and `deploy/` ports were not changed.

- Added per-service `application-local.yml`, environment-based downstream URL
  overrides and localhost:3000 CORS. Property Management is explicitly 9001.
- Frontend clients use `http://localhost:9000/api` by default; document file
  preview/download now uses Gateway too. Direct-service troubleshooting remains
  configurable.
- Added `scripts/start-local.ps1`, `check-local.ps1`, `stop-local.ps1` and
  `LOCAL_SETUP.md`. The scripts check occupied ports, start MySQL first,
  refuse duplicate PCC processes, wait on health endpoints, save logs in
  ignored `.local/`, and stop only recorded matching processes. MySQL is left
  running; no databases are reset/deleted.
- The default local profile also uses the new ports, while `local` retains
  explicit bind-address and downstream defaults. Target ports were free during
  this change, but the startup script correctly refused to run because the
  ignored `.env.local` was not readable/absent here. Live 9000-9007/3000
  runtime checks therefore remain an operator run with a valid local secret;
  do not use production secrets.
