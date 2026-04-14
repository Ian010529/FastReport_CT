#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
BUILD_DIR="${FASTREPORT_LAMBDA_BUILD_DIR:-/tmp/fastreport-lambda}"
ZIP_PATH="${FASTREPORT_LAMBDA_ZIP_PATH:-/tmp/fastreport-lambda.zip}"

cd "$BACKEND_DIR"
mvn -q -DskipTests package dependency:copy-dependencies

rm -rf "$BUILD_DIR" "$ZIP_PATH"
mkdir -p "$BUILD_DIR/lib"

cp -R "$BACKEND_DIR/target/classes/"* "$BUILD_DIR/"
cp "$BACKEND_DIR"/target/dependency/*.jar "$BUILD_DIR/lib/"

(
  cd "$BUILD_DIR"
  zip -qr "$ZIP_PATH" .
)

echo "Lambda zip created: $ZIP_PATH"
du -h "$ZIP_PATH"
