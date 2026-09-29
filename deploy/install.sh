#!/usr/bin/env bash
# SentinelX - one-shot EC2 (Amazon Linux 2023) deployment bootstrap.
# Upload the project zip to the instance, extract it, then run this script.
#
#   sudo bash install.sh            # install Docker + bring stack up
#   sudo bash install.sh --rebuild  # rebuild images from clean state
#   sudo bash install.sh --stop     # stop the stack
#   sudo bash install.sh --logs     # tail container logs
set -euo pipefail

cd "$(dirname "$0")"            # work in the directory this script lives in

case "${1:-}" in
  --stop)
    docker compose down
    exit 0
    ;;
  --logs)
    docker compose logs -f --tail=200
    exit 0
    ;;
esac

echo ">>> Installing Docker (Amazon Linux 2023)"
if ! command -v docker >/dev/null 2>&1; then
  sudo dnf install -y docker
  sudo systemctl enable --now docker
fi
sudo usermod -aG docker "$(whoami)" || true
sudo dnf install -y docker-compose-plugin || true

echo ">>> Creating .env from example (if missing)"
if [ ! -f .env ]; then
  cp .env.example .env
  # Use a random JWT secret so sessions are not guessable
  sed -i "s/^JWT_SECRET=.*/JWT_SECRET=$(openssl rand -hex 32)/" .env
fi

echo ">>> Adding swap (t3.micro has only 1GB RAM; Docker builds are memory-hungry)"
if [ ! -f /swapfile ]; then
  sudo fallocate -l 2G /swapfile
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile
  sudo swapon /swapfile
  echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
fi

echo ">>> Building + starting stack"
export DOCKER_BUILDKIT=1
if [ "${1:-}" = "--rebuild" ]; then
  sudo -E docker compose build --no-cache
fi
sudo docker compose up -d --build

echo ">>> Waiting for backend to become healthy"
for i in $(seq 1 60); do
  # any HTTP code (e.g. 401 from the auth endpoint) means Tomcat is up
  code=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST http://localhost:8080/api/auth/login \
    -H 'Content-Type: application/json' -d '{}' || true)
  if [ "$code" != "000" ] && [ "$code" != "" ]; then
    break
  fi
  sleep 5
done

IP=$(curl -s http://checkip.amazonaws.com || echo "?")
echo ""
echo "==================================================================="
echo " SentinelX is deployed."
echo "   Website  : http://$IP"
echo "   API      : http://$IP:8080"
echo "   ML health: http://$IP:8000/health"
echo "   Login    : admin / Admin@123   (change these in .env + restart!)"
echo " Logs: sudo bash install.sh --logs"
echo " Stop: sudo bash install.sh --stop"
echo "==================================================================="