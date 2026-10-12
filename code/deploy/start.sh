#!/usr/bin/env bash
# Builds everything and starts reconArk with docker compose. Run from anywhere.
set -euo pipefail
cd "$(dirname "$0")/.."            # the code/ folder

echo "1/3 Building the Java services (Gradle)…"
./gradlew bootJar --quiet

echo "2/3 Building the frontend (npm)…"
(cd frontend && npm ci --no-audit --no-fund --silent && npm run build --silent)

echo "3/3 Starting containers…"
docker compose -f deploy/docker-compose.yml --profile app up -d --build

cat <<'TXT'

reconArk is starting (give it ~30 seconds):
  UI          http://localhost:8080
  Grafana     http://localhost:3000   (dashboard "reconArk")
  Prometheus  http://localhost:9090
  Kafka UI    http://localhost:8085
Try it end to end:  deploy/smoke-test.sh
Stop:               docker compose -f deploy/docker-compose.yml --profile app down
TXT
