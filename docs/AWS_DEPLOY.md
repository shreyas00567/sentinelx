# Deploy SentinelX to AWS (free-tier, no AWS CLI required)

This guide deploys the **whole platform on a single EC2 instance** with Docker
Compose: PostgreSQL + ML service + Spring Boot backend + nginx-served frontend,
all reachable via **`http://<public-ip>`** on port 80.

No AWS CLI, no domain, no IAM keys needed — everything is done through the AWS
web console and a browser-based terminal.

---

## 1. Create an AWS account (one time)

1. Go to https://aws.amazon.com → **Create an AWS Account**.
2. Follow sign-up (email, password, contact + billing info, credit card).
   A card is required even for free tier (you won't be charged for usage within
   the free tier).
3. Choose the **Basic plan** (free) support tier.
4. Sign in to the console: https://console.aws.amazon.com

> Recommended: enable Multi-Factor Authentication and consider a
> [billing alarm](https://console.aws.amazon.com/billing/home#/budgets)
> ($1, $5, $10) so you never get surprised by a bill.

---

## 2. Launch a free-tier EC2 instance

1. Console → search **EC2** → **Launch instance**.
2. **Name**: `sentinelx`.
3. **AMI**: Amazon Linux 2023 (default, free tier eligible).
4. **Instance type**: `t3.micro` (free tier eligible, 1 GB RAM).
5. **Key pair**: skip creating one — you can connect via the browser later.
6. **Network settings** — click **Edit** and add these **Security group rules**:

   | Type | Protocol | Port | Source |
   |---|---|---|---|
   | SSH | TCP | 22 | My IP (your home IP) |
   | HTTP | TCP | 80 | 0.0.0.0/0 (anyone can see the site) |
   | HTTPS | TCP | 443 | 0.0.0.0/0 *(optional, for later)* |

   > Security tip: only allow port 80 publicly. Ports `8080` / `8000` are
   > **not** needed publicly — nginx proxies internally. Consider leaving them closed.

7. **Storage**: default 8 GiB gp2/gp3 is fine (free tier gives 30 GiB).
8. Click **Launch instance**, then go to the **Instances** list.

---

## 3. Upload the project and deploy

### 3a. Get a project zip

From a machine with the finished source (or extract the
`SentinelX.zip` you already have from the Desktop), open a terminal **inside the
project folder** and zip it:

```bash
zip -r sentinelx-deploy.zip . -x "frontend/node_modules/*" "backend/target/*" "frontend/dist/*" "*__pycache__*" "*.log"
```

### 3b. Copy the zip to the instance (in the EC2 console)

1. Select your `sentinelx` instance → **Connect**.
2. On the **EC2 Instance Connect** tab, click **Connect**.
   This opens a **browser terminal** on your instance — no SSH keys needed.
3. In that terminal, run these steps to upload via the console's upload dialog:

   ```
   sudo yum install -y zip unzip
   ```
   - Click the **Upload file** button (top-right of the Instance Connect window),
     upload `sentinelx-deploy.zip` to `/home/ec2-user/`.

4. Extract and deploy:

   ```bash
   cd ~
   mkdir -p sentinelx && cd sentinelx
   cp ../sentinelx-deploy.zip .
   unzip -o sentinelx-deploy.zip
   sudo bash deploy/install.sh
   ```

The script installs Docker, creates gen a random JWT secret, enables swap (for
the low-memory t3.micro), builds all images, and starts the stack. Wait a few
minutes — the first build downloads Maven + Node images.

---

## 4. Open the website

The install script prints the public IP. To get it manually:

```
curl -s http://checkip.amazonaws.com
```

Open **`http://<that-ip>`** in your browser → you should see the SentinelX login
page.

| Account  | Login                  |
|----------|------------------------|
| Admin    | `admin` / `Admin@123` |
| Analyst  | `analyst` / `Analyst@123` |

### Verify everything works

```bash
# from the instance
curl -s http://localhost:8000/health      # ML model loaded?
curl -s -o /dev/null -w "%{http_code}\n" \
  -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' -d '{"username":"admin","password":"Admin@123"}'  # 200?
```

Open the **Simulator** page in the UI and run `brute-force` / `insider-anomaly`
to generate traffic, then check Dashboard → Alerts.

---

## 5. Securing it (do this after testing)

1. **Change seeded credentials** — edit `.env` on the instance:
   ```bash
   cd ~/sentinelx
   sudo sed -i 's/SEED_ADMIN_PASS=Admin@123/SEED_ADMIN_PASS=YourStrongPass!/' .env
   sudo bash deploy/install.sh --rebuild
   ```
   (Data is lost on rebuild since this demo uses the Postgres volume — rebuild
   only when you want a clean state.)
2. **Add HTTPS (free)** — point a domain at the instance and add an
   [Elastic IP](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/elastic-ip-addresses-eip.html)
   then use certbot. Update the nginx config to listen on 443 and proxy
   `X-Forwarded-Proto`.
3. Close SSH (port 22) once you're done unless you still need it.

---

## Costs & cleanup

- **Free tier**: t3.micro + 30 GiB EBS + data transfer within limits → the
  standard stack fits comfortably in the free tier for the first 12 months.
- **Stop billing**: on the EC2 **Instances** page: *Instance state → Stop* (keeps
  the disk) or *Terminate* (deletes everything). A stopped instance costs
  ~nothing extra beyond EBS storage; a *terminated* instance costs nothing.

## Troubleshooting

| Symptom | Fix |
|---|---|
| Page loads but login fails | Backend still starting — run `sudo bash deploy/install.sh --logs` and wait, or check `docker compose ps` |
| `docker: command not found` | Install script didn't finish. Check it completed, then `sudo bash deploy/install.sh` again (it's idempotent). |
| Port 80 unreachable from browser | Security group missing HTTP 80 rule — add it and allow incoming HTTP. |
| Very slow first deploy | Expected: it builds Spring Boot + pulls images. Add swap (script does this) if it OOMs. |