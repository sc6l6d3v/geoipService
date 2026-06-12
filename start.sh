#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: $0 DEV|PROD [HOST_PORT]" >&2
  exit 1
}

ENV_IN="${1:-}"; [[ -z "${ENV_IN}" ]] && usage
ENV_UPPER="$(echo "${ENV_IN}" | tr '[:lower:]' '[:upper:]')"
case "${ENV_UPPER}" in DEV|PROD) ;; *) echo "ENV must be DEV or PROD"; exit 2;; esac

HOST_PORT="${2:-8083}"

# Load variables from .env if present
if [[ -f .env ]]; then
  set -a
  . ./.env
  set +a
fi

: "${HUBUSER:?HUBUSER must be set (e.g. export HUBUSER=yourdockerhubuser)}"

IMAGE="${HUBUSER}/geoip:rest-${ENV_UPPER}"
NETWORK="rs-net-${ENV_UPPER,,}"

docker run --net "${NETWORK}" \
  --env MONGOURI --env MONGORO --env REDISKEY --env REDISHOST \
  --env GEOIPKEY --env DBNAME --env PORT --env BINDHOST \
  --env CLIENTPOOL --env SERVERPOOL --env REDIS_TTL_MINUTES \
  --add-host=wengen.iscs-i.com:192.168.15.66 \
  --restart on-failure \
  --name geoip-svc \
  -d -p "${HOST_PORT}:8080" -p 5050:5050 "${IMAGE}"
