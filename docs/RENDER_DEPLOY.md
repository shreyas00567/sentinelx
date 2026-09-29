# Deploy SentinelX to Render (free, no credit card)

Everything runs on the **Render free plan** — no card required — with real,
public URLs like `https://sentinelx-web.onrender.com`. One click provisions all
four pieces from the `render.yaml` blueprint:

| Service | What it is | URL |
|---|---|---|
| `sentinelx-web` | React dashboard (static) | `https://sentinelx-web.onrender.com` |
| `sentinelx-api` | Spring Boot backend | `https://sentinelx-api.onrender.com` |
| `sentinelx-ml` | FastAPI + ML autoencoder | `https://sentinelx-ml.onrender.com/health` |
| `sentinelx` | PostgreSQL database | internal |

> Free-tier notes: web services sleep after ~15 min of inactivity and wake on
> the next visit (takes ~30–60 s). The free Postgres database expires after 30
> days — fine for a demo; upgrade or export before then if you want to keep data.

---

## 1. Put the project on GitHub

1. Create a repo: https://github.com/new — name it `sentinelx` (set it **Public**
   so Render can access it free; or **Private** and connect Render per step 3).
2. In a terminal on this PC, from the project folder:

   ```powershell
   cd C:\Users\shreyas khanore\Cybersecurity_project
   git init
   git add -A
   git commit -m "SentinelX - SIEM demo, Render-ready"
   git remote add origin https://github.com/<YOUR-USERNAME>/sentinelx.git
   git branch -M main
   git push -u origin main
   ```

   (PowerShell will ask for your GitHub username + a **Personal Access Token**,
   not your password — create one at GitHub → Settings → Developer settings →
   Personal access tokens → generate one with `repo` scope.)

---

## 2. Create a Render account

1. Go to https://render.com → **Get Started** → sign up **using GitHub** (this
   links the account — no card needed).
2. Confirm your email when Render prompts.

---

## 3. Deploy with the Blueprint

1. In the Render dashboard click **New +** → **Blueprint**.
2. Select your `sentinelx` GitHub repo.
3. Render reads `render.yaml` and proposes 4 services (database + 3 web).
4. Click **Apply**. Render builds and deploys them in order (database →
   ml → api → web). First build takes ~5–10 min (it compiles Spring Boot and
   installs npm deps).

---

## 4. Open your website

When `sentinelx-web` shows **Live**:

1. Open `https://sentinelx-web.onrender.com`
2. Log in: `admin` / `Admin@123` (or `analyst` / `Analyst@123`).
3. Run scenarios on the **Simulator** page, then check Dashboard / Alerts /
   Incidents.

### Verify the pieces

```bash
curl https://sentinelx-ml.onrender.com/health        # {"status":"ok",...}
curl -X POST https://sentinelx-api.onrender.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"Admin@123\"}"   # 200 + token
```

---

## 5. First login attempt shows "Session expired"/401?

First visit wakes the backend (~30 s). Wait for `sentinelx-api` to be **Live**
(green), then refresh the page. The frontend calls `VITE_API_URL` which the
blueprint wired automatically to the backend's URL.

---

## Updating after code changes

Push to GitHub → Render auto-rebuilds the affected service(s). To rebuild
manually, open the service → **Manual Deploy** → **Deploy latest commit**.

## Changing the seeded passwords

In the Render dashboard open `sentinelx-api` → **Environment** → set
`SEED_ADMIN_PASS` and `SEED_ANALYST_PASS` to new values, click **Save Changes**
(a redeploy happens automatically).

## Costs & cleanup

- All four services are on `plan: free` — no charges. Free limits: 750
  instance-hours/month per account (services sleep when idle, so this is ample).
- To delete everything: Render dashboard → remove the Blueprint or each service.

## Troubleshooting

| Symptom | Fix |
|---|---|
| `sentinelx-api` deploy fails at "dependency:go-offline" | Transient Maven network issue — click **Manual Deploy → Deploy latest commit** to retry. |
| Web shows blank / API errors | Make sure `sentinelx-api` is **Live** first, then refresh the site. |
| Events don't produce alerts | Run the Simulator scenarios after the backend is fully up; wait ~10 s for risk/incident processing. |
| Frontend CORS errors | The blueprint sets `CORS_ORIGINS` to the frontend URL automatically. If you renamed services, re-check web service env vars. |