# Deploy TransitOps Backend to Railway

## Prerequisites

- Railway account linked to GitHub
- Neon (or Railway Postgres) credentials
- Rotated `JWT_SECRET` (do not reuse any secret that appeared in chat)

## Steps

1. Push this repository to GitHub (`Steve-create99/TransitOps-Backend`).
2. In Railway: **New Project → Deploy from GitHub** → select the repo.
3. Add variables:

```text
DATABASE_URL=postgres://USER:PASSWORD@HOST/DB?sslmode=require
JWT_SECRET=<base64-secret>
JWT_ACCESS_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=604800000
CORS_ORIGINS=https://your-frontend-domain,http://localhost:5173
BOOTSTRAP_ADMIN_EMAIL=admin@yourdomain.com
BOOTSTRAP_ADMIN_PASSWORD=<strong-password>
JPA_DDL_AUTO=update
SPRING_PROFILES_ACTIVE=prod
```

Notes:
- `DATABASE_URL` may be `postgres://...` (Railway style). The app converts it to JDBC automatically.
- Do **not** set `SPRING_PROFILES_ACTIVE=h2` in production.
- If you omit `SPRING_PROFILES_ACTIVE` but set `DATABASE_URL`, the app switches to `prod` automatically.

4. Railway will build with Maven and start via `railway.toml` / `Procfile`.
5. Verify: `GET https://<railway-host>/api/health` → `{ "status": "UP" }`
6. Login: `POST /api/auth/login` with bootstrap admin.
7. Demo driver (mobile app): `kwame.mensah@transitops.local` / `Driver@12345` (seeded & linked on boot).
8. Point frontend production build:

```text
VITE_API_URL=https://<railway-host>/api
```

Then `npm run build` in the TransitOps frontend and deploy the `dist/` folder.
