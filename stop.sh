#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

read -r -p "This removes containers and the Postgres volume. Continue? [y/N] " reply
case "$reply" in
  [yY]*) docker compose down -v ;;
  *) echo "Cancelled." ;;
esac
