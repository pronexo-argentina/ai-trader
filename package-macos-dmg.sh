#!/usr/bin/env bash
set -euo pipefail

APP_NAME="IA-TradeX"
VERSION="1.0.0"
MAIN_CLASS="com.iatradex.Launcher"
MAIN_JAR="ia-tradex-0.1.0-SNAPSHOT.jar"

if ! command -v jpackage >/dev/null 2>&1; then
  echo "ERROR: jpackage no está disponible. Usá un JDK 21 completo."
  exit 1
fi

mvn clean package

rm -rf target/package-input
mkdir -p target/package-input
cp "target/${MAIN_JAR}" target/package-input/

mvn -q dependency:copy-dependencies \
  -DincludeScope=runtime \
  -DoutputDirectory=target/package-input

rm -rf dist
mkdir -p dist

jpackage \
  --type dmg \
  --name "${APP_NAME}" \
  --app-version "${VERSION}" \
  --vendor "IA-TradeX" \
  --description "Análisis de mercados y backtesting 100% Java" \
  --input target/package-input \
  --main-jar "${MAIN_JAR}" \
  --main-class "${MAIN_CLASS}" \
  --icon packaging/IA-TradeX.icns \
  --dest dist \
  --java-options "-Dfile.encoding=UTF-8"

echo "DMG generado dentro de dist/"
