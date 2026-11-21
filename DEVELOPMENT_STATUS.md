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
- Refund reduces Finance's target paidAmount, but the refunded amount is not pushed back to Property schedule paidAmount (schedule stays PAID while Finance shows residual outstanding — manual adjustment or a future refund-sync endpoint).
- Account balances only move when a payment specifies `accountId`; payments posted without an account keep balances at 0 (explicit, by design).
- Advance-rent overpayment workflow not implemented (rejected 422, per spec default).
- Receipts/print is browser `window.print()`; PDF generation deferred to Document Service.

## Outstanding / next phases
- Phase 3 API Gateway (:8080) — deferred; direct service URLs in dev.
- Phase 7+ Document, Workflow, Reporting services — not started.
- Property images/documents and land document references — fields exist (`document_reference`/`documentReference`); actual storage deferred until Document Service.
- `building_land_parcels` link table — deferred until Property↔Land integration requirements are defined.
- GIS map visualization — coordinates stored and validated; map UI deferred.
