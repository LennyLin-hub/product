#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

source "$ROOT_DIR/scripts/dev-env.sh"

cd "$ROOT_DIR"
mvn spring-boot:run -pl product-server -Dspring-boot.run.profiles=local
