#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$ROOT_DIR/.env"

if [[ ! -f "$ENV_FILE" ]]; then
    echo "缺少 $ENV_FILE，请先基于 .env.example 创建一份 .env。" >&2
    exit 1
fi

set -a
source "$ENV_FILE"
set +a

