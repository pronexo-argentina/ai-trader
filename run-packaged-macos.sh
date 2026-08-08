#!/usr/bin/env bash
set -euo pipefail

APP="./dist/IA-TradeX.app"

if [ ! -d "$APP" ]; then
  echo "No existe $APP"
  echo "Primero ejecutá: ./package-macos.sh"
  exit 1
fi

echo "Iniciando IA-TradeX desde la terminal para mostrar cualquier error..."
"$APP/Contents/MacOS/IA-TradeX"
