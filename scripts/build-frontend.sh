#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONTEND_DIR="$ROOT_DIR/frontend"
STATIC_APP_DIR="$ROOT_DIR/src/main/resources/static/app"

cd "$FRONTEND_DIR"
npm install
npm run build

rm -rf "$STATIC_APP_DIR"
mkdir -p "$STATIC_APP_DIR"
cp -R "$FRONTEND_DIR/dist/." "$STATIC_APP_DIR/"
