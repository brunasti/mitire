#!/usr/bin/env bash
set -euo pipefail

APP_NAME="mitire"
PORT=8080
LOG_FILE="/tmp/${APP_NAME}.log"
PID_FILE="/tmp/${APP_NAME}.pid"
BUILD_JAR="ui/build/libs/ui-0.1.0-SNAPSHOT.jar"
DEPLOY_JAR="/tmp/${APP_NAME}.jar"

usage() {
  echo "Usage: $0 {start|stop|restart|build|logs}"
  echo ""
  echo "  build     Build the application JAR (skips tests)"
  echo "  start     Build and start"
  echo "  stop      Stop the application"
  echo "  restart   Stop, build, and start (kills anything on port ${PORT} first)"
  echo "  logs      Tail the application log"
}

build() {
  echo "Building ${APP_NAME} (production mode — bundles Vaadin frontend)..."
  ./gradlew :ui:bootJar -Pvaadin.productionMode=true -x test
  cp "${BUILD_JAR}" "${DEPLOY_JAR}"
  echo "Build complete: ${DEPLOY_JAR}"
}

stop() {
  echo "Stopping ${APP_NAME}..."
  if [ -f "$PID_FILE" ]; then
    kill "$(cat "$PID_FILE")" 2>/dev/null && echo "Stopped (pid $(cat "$PID_FILE"))" || echo "Process already gone"
    rm -f "$PID_FILE"
  fi
  lsof -ti:"$PORT" -sTCP:LISTEN | xargs kill -9 2>/dev/null || true
}

start() {
  build
  echo "Starting ${APP_NAME} on port ${PORT}..."
  java -jar "${DEPLOY_JAR}" > "$LOG_FILE" 2>&1 &
  echo $! > "$PID_FILE"
  echo "PID $(cat "$PID_FILE") — tailing log (Ctrl+C to detach, app keeps running):"
  echo "Log: ${LOG_FILE}"
  tail -f "$LOG_FILE"
}

restart() {
  stop
  sleep 1
  start
}

logs() {
  tail -f "$LOG_FILE"
}

COMMAND="${1:-start}"

case "$COMMAND" in
  build)   build ;;
  start)   start ;;
  stop)    stop ;;
  restart) restart ;;
  logs)    logs ;;
  help|--help|-h) usage ;;
  *)
    echo "Unknown command: $COMMAND"
    usage
    exit 1
    ;;
esac
