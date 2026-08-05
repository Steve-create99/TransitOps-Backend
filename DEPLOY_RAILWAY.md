# Deploy TransitOps Backend to Railway

## Prerequisites

- Railway account linked to GitHub
- Neon (or Railway Postgres) credentials
- Rotated `JWT_SECRET` (do not reuse any secret that appeared in chat)
- Resend API key for driver invite emails
- VAPID key pair for web push (`npx web-push generate-vapid-keys`)

## Steps

1. Push this repository to GitHub (`Steve-create99/TransitOps-Backend`).
2. In Railway: **New Project → Deploy from GitHub** → select the repo.
3. Add variables:

```text
DATABASE_URL=postgresql://USER:PASSWORD@HOST/DB?sslmode=require
JWT_SECRET=<long-random-secret>
JWT_ACCESS_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=604800000
CORS_ORIGINS=https://transitops-frontend.pages.dev,http://localhost:5173
BOOTSTRAP_ADMIN_EMAIL=admin@yourdomain.com
BOOTSTRAP_ADMIN_PASSWORD=<strong-password>
JPA_DDL_AUTO=update
SPRING_PROFILES_ACTIVE=prod
FRONTEND_URL=https://transitops-frontend.pages.dev
INVITE_TTL_HOURS=72
RESEND_API_KEY=re_xxxxxxxx
RESEND_FROM_EMAIL=TransitOps <onboarding@resend.dev>
VAPID_PUBLIC_KEY=<base64url public key>
VAPID_PRIVATE_KEY=<base64url private key>
VAPID_SUBJECT=mailto:admin@yourdomain.com
```

Local reference copy (gitignored): see [`.env`](.env) and template [`.env.example`](.env.example).

**CORS tip:** use the site origin only (`https://transitops-frontend.pages.dev`), not `/dashboard`.

**JWT tip:** expirations are **milliseconds** (`900000` = 15m, `604800000` = 7d), not `15m` / `7d`.

**Resend tip:** the free test sender `onboarding@resend.dev` can only deliver to the email address of your Resend account. Verify a domain in Resend for production campus mail.

**Web push tip:** after deploy, open Settings → enable browser notifications while signed in as ADMIN. The public VAPID key is exposed at `GET /api/push/vapid-public-key`.

Notes:
- `DATABASE_URL` may be `postgres://...` or `postgresql://...`. The app converts it to JDBC automatically.
- Do **not** set `SPRING_PROFILES_ACTIVE=h2` in production.
- If you omit `SPRING_PROFILES_ACTIVE` but set `DATABASE_URL`, the app switches to `prod` automatically.
- **Self-registration is disabled** — invite drivers via Drivers → Invite (email) or Settings.
- Demo logins: Admin `admin@transitops.local` / `Admin@12345`; Driver `kwame.mensah@transitops.local` / `Driver@12345`.
- Current production host: `https://web-production-f8ec21.up.railway.app`
- Known debt: Hibernate `ddl-auto=update` (Flyway deferred).

4. Railway will build with Maven and start via `railway.toml` / `Procfile`.
5. Verify: `GET https://web-production-f8ec21.up.railway.app/api/health` → `{ "status": "UP" }`
6. Login: `POST /api/auth/login` with bootstrap admin.
7. Invite a driver: `POST /api/drivers/invite` (ADMIN JWT) with `{ "email", "firstName", "lastName" }`.
8. Accept: open `{FRONTEND_URL}/invite/accept?token=...`, set password, then sign in on web/mobile.
9. Demo driver (mobile app): `kwame.mensah@transitops.local` / `Driver@12345` (seeded & linked on boot).
10. Point frontend production build:

```text
VITE_API_URL=https://web-production-f8ec21.up.railway.app/api
```

Then `npm run build` in the TransitOps frontend and deploy the `dist/` folder.
