#!/bin/bash

# Absoluter Pfad zu diesem Skript (auch wenn über Symlink aufgerufen)
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do
  DIR="$(cd -P "$(dirname "$SOURCE")" >/dev/null 2>&1 && pwd)"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
done
SCRIPT_DIR="$(cd -P "$(dirname "$SOURCE")" >/dev/null 2>&1 && pwd)"

# Jar suchen im Projektverzeichnis
JAR="$(ls "$SCRIPT_DIR/build/libs/trustscan.jar" 2>/dev/null | head -n 1)"
LINK_PATH="/usr/local/bin/trustscan"

function run() {
  if [ ! -f "$JAR" ]; then
    echo "❌ Build not found. Run: ./gradlew build"
    exit 1
  fi
  java -jar "$JAR" "$@"
}

function install_link() {
  if [ ! -f "$JAR" ]; then
    echo "❌ Build not found. Run: ./gradlew build"
    exit 1
  fi
  echo "🔗 Installing symlink to $LINK_PATH"
  sudo ln -sf "$SCRIPT_DIR/trustscan.sh" "$LINK_PATH"
  sudo chmod +x "$SCRIPT_DIR/trustscan.sh"
  echo "✅ Installed. You can now run 'trustscan' from anywhere."
}

if [ "$1" == "install" ]; then
  install_link
  exit 0
fi

run "$@"
