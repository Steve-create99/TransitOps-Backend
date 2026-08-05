# TransitOps RBAC + Driver Portal — QA Report

**Date:** 2026-08-05  
**Scope:** Backend (`TransitOps-backend`) + Web (`TransitOps`)  
**Mobile:** unchanged (already DRIVER-gated)  
**Verdict:** **Ready** for role-separated deploy (backend + web together)

---

## 1. Files changed

### Backend
- `src/main/java/com/transitops/backend/controller/DriverController.java` — class-level `ADMIN|DISPATCHER` (closes DRIVER list/IDOR)
- `src/main/java/com/transitops/backend/controller/VehicleController.java` — class-level `ADMIN|DISPATCHER` (closes DRIVER fleet/GPS IDOR)
- `src/main/java/com/transitops/backend/controller/DashboardController.java` — staff-only KPIs/charts
- `src/main/java/com/transitops/backend/controller/ReportController.java` — staff-only reports
- `src/main/java/com/transitops/backend/controller/RouteController.java` — explicit GET roles (`ADMIN|DISPATCHER|DRIVER` read)
- `src/main/java/com/transitops/backend/controller/StopController.java` — same
- `src/main/java/com/transitops/backend/controller/ScheduleController.java` — same
- `src/main/java/com/transitops/backend/entity/Driver.java` — `@JsonIgnore` on `user` (fix incident JSON 500)
- `src/main/java/com/transitops/backend/entity/Incident.java` — ignore lazy relations on serialize
- `RBAC_QA_REPORT.md` — this report

### Web
- `src/utils/roles.js` — `isStaff` / `isDriver` / `homeForRole`
- `src/routes/AppRouter.jsx` — `StaffRoute` + `DriverRoute` trees
- `src/layouts/DriverLayout.jsx` — driver-only shell/nav
- `src/pages/driver/Home.jsx`, `Trip.jsx`, `History.jsx`, `Incidents.jsx`, `Notifications.jsx`, `Profile.jsx`
- `src/pages/Login/index.jsx` — post-login `homeForRole`
- `src/pages/InviteAccept/index.jsx` — post-accept `homeForRole`
- `src/services/api.js` — `driverMeApi` + clearer 403 message
- `src/context/TransitContext.jsx` — DRIVER skips staff fleet/dashboard polls

---

## 2. Role matrix

### Web routes

| Route | ADMIN | DISPATCHER | DRIVER | Anonymous |
|-------|:-----:|:----------:|:------:|:---------:|
| `/login`, `/invite/accept` | redirect home | redirect home | redirect `/driver` | ok |
| `/dashboard`, `/routes`, `/stops`, `/schedules`, `/maps`, `/reports`, `/drivers`, `/vehicles`, `/notifications`, `/settings` | ok | ok | → `/driver` | → `/login` |
| `/driver`, `/driver/trip`, `/driver/history`, `/driver/incidents`, `/driver/notifications`, `/driver/profile` | → `/dashboard` | → `/dashboard` | ok | → `/login` |

### API (summary)

| Endpoint area | ADMIN | DISPATCHER | DRIVER | Anonymous |
|---------------|:-----:|:----------:|:------:|:---------:|
| `POST /api/auth/login\|refresh` | public | public | public | public |
| `GET /api/auth/me`, logout | ✓ | ✓ | ✓ | 401/403 |
| `/api/invites/**` | public (token) | public | public | public |
| `/api/settings/**` | ✓ | ✗ | ✗ | blocked |
| `/api/dashboard/**` | ✓ | ✓ | ✗ 403 | blocked |
| `/api/reports/**` | ✓ | ✓ | ✗ 403 | blocked |
| `/api/drivers` CRUD/list/`{id}/**` | ✓ (invite/delete ADMIN) | ✓ (no invite/delete) | ✗ 403 | blocked |
| `/api/drivers/me/**` | ✓* | ✓* | ✓ | blocked |
| `/api/vehicles/**` | ✓ (delete ADMIN) | ✓ | ✗ 403 | blocked |
| `/api/routes\|stops\|schedules` GET | ✓ | ✓ | ✓ (read) | blocked |
| `/api/routes\|stops\|schedules` mutate | ✓ | ✓ | ✗ 403 | blocked |
| `/api/notifications` list/read | ✓ | ✓ | ✓ (own) | blocked |
| `POST /api/notifications` | ✓ | ✓ | ✗ | blocked |

\* Staff on `/drivers/me` only if linked Driver row exists (else 404).

**Intentional read exception:** DRIVER may `GET` routes/stops/schedules so the Expo companion (and assigned-route views) keep working without a mobile change. Mutations remain staff-only.

---

## 3. Bugs found / fixed

| Bug | Fix |
|-----|-----|
| DRIVER saw full admin web shell (login → `/dashboard`) | Role-aware router + `/driver/*` portal |
| DRIVER could call admin GETs (drivers, vehicles, dashboard, reports, settings) | `@PreAuthorize` on controllers |
| DRIVER IDOR: `POST /api/drivers/{id}/incidents`, `POST /api/vehicles/{id}/gps` | Class-level staff gate; drivers use `/api/drivers/me/**` |
| `POST /api/drivers/me/incidents` returned 500 (lazy `User` proxy in JSON) | `@JsonIgnore` on `Driver.user` + ignore props on `Incident.driver` |
| TransitContext `Promise.all` failed for DRIVER after API 403s | Skip staff APIs when role is DRIVER |

---

## 4. Remaining blockers

1. **Deploy coupling:** Production Railway still serves pre-RBAC APIs until this backend is deployed. Deploy **backend before (or with) web**, or drivers using the new web build against old API may see inconsistent 403s.
2. **No self-service change-password API** — profile page documents invite/admin reset only (by design for this pass).
3. **Anonymous unauthenticated** requests to protected method-secured endpoints may return **403** instead of **401** (Spring Access Denied). Still denied; clients should treat both as “not allowed.”
4. **UI smoke** of every driver page against live data was API-validated; full click-through in a browser should be done after deploy.

---

## 5. Security notes

- Authorization uses **DB role via `UserDetails`**, not the JWT `role` claim alone.
- UI hiding is not trusted: staff endpoints return **403** for DRIVER JWTs.
- DRIVER write paths for trips/incidents/location/attendance are self-scoped under `/api/drivers/me/**`.
- Do not weaken CORS/JWT for convenience.
- Demo driver password remains hardcoded in `DataInitializer` (`Driver@12345`) — env-configurable bootstrap would be a follow-up hardening item.

---

## 6. Seed / demo accounts used

| Role | Email | Password | Source |
|------|-------|----------|--------|
| ADMIN | `admin@transitops.local` | `Admin@12345` | `BOOTSTRAP_ADMIN_*` / `.env.example` |
| DRIVER | `kwame.mensah@transitops.local` | `Driver@12345` | `DataInitializer` / `DEPLOY_RAILWAY.md` |

Local smoke ran against `http://localhost:8080` (Neon DB via local `.env`).

---

## 7. Smoke results (API)

| Flow | Admin | Driver |
|------|-------|--------|
| Login | ✓ | ✓ |
| Refresh token | ✓ | ✓ |
| Dashboard/reports/drivers/vehicles GETs | 200 | **403** |
| Settings | 200 | **403** |
| Invite driver | allowed (ADMIN) | **403** |
| `/api/drivers/me` + shift | n/a (404 if no driver row) | **200** |
| `POST /api/drivers/{id}/incidents` | staff | **403** |
| `POST /api/vehicles/{id}/gps` | staff | **403** |
| `POST /api/drivers/me/incidents` | — | **200** (after JSON fix) |
| Frontend build (`npm run build`) | ✓ | ✓ |
| Backend compile | ✓ | ✓ |

---

## 8. Production-readiness verdict

### **Ready**

**Justification:** DRIVER can no longer use the admin web shell or admin fleet/analytics APIs; self-scoped companion APIs work; IDOR write paths closed; admin ops unchanged for core GETs/mutations under `ADMIN|DISPATCHER`. Remaining items are deploy ordering and optional password UX — not open authorization holes in the updated codepaths.

**Deploy checklist**
1. Deploy backend with these `@PreAuthorize` changes  
2. Deploy web with `/driver/*` router  
3. Verify demo DRIVER lands on `/driver` and gets 403 on `/api/drivers`  
4. Verify admin still loads `/dashboard`
