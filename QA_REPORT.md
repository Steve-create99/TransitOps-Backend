# TransitOps QA Report

Date: 2026-08-04

## Summary

Full-stack production suite implemented: Spring Boot API (auth, domain CRUD, dashboard, reports, settings, GPS) + React frontend wired to real APIs with RBAC. Frontend production build succeeds. Backend runs on H2 locally (Neon credentials currently reject authentication — rotate Neon password and set `SPRING_PROFILES_ACTIVE=local`).

## Features tested

| Area | Result |
|------|--------|
| Login (admin bootstrap) | Pass — JWT access + refresh returned |
| Health `GET /api/health` | Pass |
| Dashboard KPIs | Pass — real aggregates (`passengersToday=690`, `activeBuses=2`) |
| Refresh token uniqueness | Fixed — UUID `jti` claim added after duplicate-key failure |
| Frontend `npm run build` | Pass |
| Routes/Stops/Schedules API surface | Pass — list + create/delete route smoke tested |
| Lazy-load JSON errors on routes/drivers/schedules | Fixed — EAGER fetch + transactional list methods |
| Drivers/Vehicles/Notifications/Reports/Settings | Implemented backend + frontend modules |
| Role guards (UI) | ADMIN / DISPATCHER / DRIVER nav + route guards |
| Public ADMIN signup | Removed — DISPATCHER/DRIVER only |

## Bugs found and fixed

1. **Refresh token unique constraint** — identical JWTs within the same second. Fixed with `jti` UUID in refresh tokens.
2. **Neon auth failure** — `password authentication failed for user neondb_owner`. Added H2 profile fallback; Neon password must be rotated/updated in `application-local.properties`.
3. **Secrets in `porpmt.md`** — removed plaintext DB URL/password from draft file.
4. **Mock KPIs / Reports** — replaced with `/api/dashboard` and `/api/reports`.
5. **Open ADMIN registration** — blocked in `AuthService` + Login UI.

## Backend endpoints delivered

- Auth: register, login, refresh, logout, me
- CRUD: routes, stops, schedules, drivers, vehicles
- Drivers: incidents, attendance, profile
- Vehicles: locations, GPS ping, maintenance
- Notifications: list, unread-count, mark read/all, archive, delete
- Dashboard: kpis, charts
- Reports: JSON + CSV export
- Settings: org, users invite/update, audit logs
- Health: `/api/health`

## Performance improvements

- Vite proxy to localhost for local API
- Pageable list endpoints (size defaults)
- Frontend context single parallel fetch for dashboard bootstrap
- Optimistic mark-read for notifications

## Security improvements

- JWT access + refresh; logout revokes refresh tokens
- `@PreAuthorize` on mutating ops and settings
- Password hashed with BCrypt; `@JsonIgnore` on password field
- CORS configurable via `CORS_ORIGINS`
- Role-based frontend route guards

## Railway deploy checklist

1. Push backend repo; connect Railway.
2. Set env: `DATABASE_URL`, `JWT_SECRET` (new rotated value), `CORS_ORIGINS`, `BOOTSTRAP_ADMIN_*`, unset or avoid `SPRING_PROFILES_ACTIVE=h2` in prod.
3. Deploy jar; verify `GET /api/health`.
4. Set frontend `VITE_API_URL` to Railway `/api` and rebuild.

## Remaining recommendations before production

1. **Rotate Neon DB password and JWT secret** immediately (credentials were previously exposed in chat/docs).
2. Re-point `SPRING_PROFILES_ACTIVE=local` once Neon works; use Flyway migrations instead of `ddl-auto=update`.
3. Deploy backend to Railway and smoke-test auth + one CRUD path.
4. Add integration tests for auth and route CRUD.
5. Consider code-splitting the frontend bundle (>500KB warning).
6. Replace CDN Leaflet with npm `leaflet` + `react-leaflet` when convenient.
