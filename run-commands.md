# Run Commands Cheat Sheet

Quick reference for spinning everything up. Three independent pieces: **MySQL**, **Spring Boot backend**, **React frontend**, plus the optional **network-analysis** Python batch job.

---

## 0. Prerequisites (one-time)

- Local MySQL 8 running on `localhost:3306`, user `root` / password `n3u3da!` (see `application.properties`). DB `sentinel` auto-creates on first backend run — no manual setup needed.
  - **Not** the `compose.yaml` MySQL service — `spring.docker.compose.enabled=false`, that file is unused.
- Java 21 JDK, Node.js (for `sentinel-ui`), Python 3.13 (for `network-analysis`, optional).

---

## 1. Backend — `spring-Sentinel/`

```powershell
cd spring-Sentinel

# run (dev, with devtools hot-restart)
.\mvnw spring-boot:run

# just compile (triggers devtools auto-restart if the app is already running)
.\mvnw compile

# clean rebuild (use if you get stale/weird class errors)
.\mvnw clean compile

# full clean build + tests (no tests currently exist in this project)
.\mvnw clean install
```

- Runs on `http://localhost:8080`.
- `GROQ_API_KEY` env var needed only for the `/api/chatbot/ask` endpoint — everything else works without it.
- Optional overrides via env vars: `NETWORK_PYTHON_EXECUTABLE`, `NETWORK_PYTHON_SCRIPT_DIR` (for the "Run Analysis Now" button subprocess launch).

---

## 2. Frontend — `sentinel-ui/`

```powershell
cd sentinel-ui

# first time only (or after pulling dependency changes)
npm install

# dev server (hot reload, proxies /api/* to localhost:8080)
npm run dev

# lint
npm run lint

# production build
npm run build

# preview the production build
npm run preview
```

- Dev server prints its own URL (Vite default `http://localhost:5173`).

---

## 3. Network Analysis batch job — `network-analysis/` (optional, Python)

```powershell
cd network-analysis

# first time only: create venv + install deps
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt

# one-off manual run
python run_analysis.py --lookback-days 30 --trigger MANUAL

# scheduled/looping mode (polls for pending run requests + runs periodically)
python scheduler.py
```

- Normally triggered automatically by the backend's "Run Analysis Now" button (`POST /api/network/analysis/run`), which launches `run_analysis.py` as a subprocess — you don't need to run it manually unless testing standalone.
- Requires `.env` in `network-analysis/` with DB creds (copy from `.env.example`).
- `seed_network_data.py` is a **one-off, non-idempotent** data-seeding script — don't run more than once (adds a fresh batch of accounts/transactions every time, no dedup).

---

## 4. Typical full startup order

1. Make sure local MySQL is running.
2. `cd spring-Sentinel && .\mvnw spring-boot:run` (leave running).
3. `cd sentinel-ui && npm run dev` (leave running).
4. Open the Vite dev URL in the browser.
5. (Optional) Trigger network analysis via the UI's "Run Analysis Now" button, or manually with `python run_analysis.py --lookback-days 30 --trigger MANUAL` from `network-analysis/`.

---

## 5. Useful endpoints once backend is up

Base URL: `http://localhost:8080`

- `GET /api/transactions`, `POST /api/transactions`
- `GET /api/alerts`
- `GET /api/cases`, `PATCH /api/cases/{id}/{acknowledge|investigate|close|dismiss}`
- `GET /api/rules`, full CRUD
- `GET/PUT /api/alert-settings`
- `POST /api/simulator/start` · `POST /api/simulator/stop` · `GET /api/simulator/status` · `POST /api/simulator/trigger/{velocity|high-value|new-payee}`
- `POST /api/chatbot/ask`
- `GET /api/network/scores`, `GET /api/network/accounts/{id}`, `GET /api/network/accounts/{id}/graph`, `GET /api/network/runs`, `POST /api/network/analysis/run`

Full details: [BACKEND_API_REFERENCE.md](BACKEND_API_REFERENCE.md)
