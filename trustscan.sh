#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR="$(ls $SCRIPT_DIR/build/libs/trustscan-*.jar | head -n 1)"

if [ ! -f "$JAR" ]; then
  echo "Build nicht gefunden. Baue zuerst mit: ./gradlew build"
  exit 1
fi

java -jar "$JAR" "$@"
