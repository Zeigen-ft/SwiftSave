#!/usr/bin/env sh
set -eu
PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
OUTPUT_DIR=$(mktemp -d "${TMPDIR:-/tmp}/swiftsave-tests.XXXXXX")
trap 'rm -rf "$OUTPUT_DIR"' EXIT HUP INT TERM
if command -v javac >/dev/null 2>&1; then
    COMPILER=javac
else
    COMPILER="java -m jdk.compiler/com.sun.tools.javac.Main"
fi
$COMPILER -encoding UTF-8 -d "$OUTPUT_DIR" \
    "$PROJECT_DIR/app/src/main/java/app/swiftsave/UrlPolicy.java" \
    "$PROJECT_DIR/app/src/main/java/app/swiftsave/EngineErrors.java" \
    "$PROJECT_DIR/app/src/main/java/app/swiftsave/XCookieData.java" \
    "$PROJECT_DIR/tests/RegressionTests.java" \
    "$PROJECT_DIR/tests/XCookieDataTests.java" \
    "$PROJECT_DIR/tests/CookieExportFixture.java"
java -cp "$OUTPUT_DIR" app.swiftsave.RegressionTests
java -cp "$OUTPUT_DIR" app.swiftsave.XCookieDataTests
if [ -n "${SWIFTSAVE_COOKIE_FIXTURE_PATH:-}" ]; then
    java -cp "$OUTPUT_DIR" app.swiftsave.CookieExportFixture > "$SWIFTSAVE_COOKIE_FIXTURE_PATH"
fi
