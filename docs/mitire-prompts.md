
---

**2026-09-03 11:42:15**

Hi Claude, how do I reastart MiTiRe? Could you create a shell script to start and restart it, similar to this script here? #!/usr/bin/env bash
set -euo pipefail

APP_NAME="octa-reporting-system"
PORT=8085
LOG_FILE="/tmp/${APP_NAME}.log"
PID_FILE="/tmp/${APP_NAME}.pid"
BUILD_JAR="build/libs/${APP_NAME}-0.0.1-SNAPSHOT.jar"
DEPLOY_JAR="/tmp/${APP_NAME}.jar"
PROFILE="postgres"

usage() {
  echo "Usage: $0 {start|stop|restart|build|logs} [--profile dev|postgres]"
  echo ""
  echo "  build                        Build the application JAR (skips tests)"
  echo "  start   [--profile <name>]   Build and start (default profile: postgres)"
  echo "  stop                         Stop the application"
  echo "  restart [--profile <name>]   Stop, build, and start"
  echo "  logs                         Tail the application log"
  echo ""
  echo "Examples:"
  echo "  $0 start"
  echo "  $0 start --profile postgres"
  echo "  $0 restart --profile postgres"
}

parse_profile() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --profile|-p)
        PROFILE="$2"
        shift 2
        ;;
      *) shift ;;
    esac
  done
}

build() {
  echo "Building ${APP_NAME} (production mode — bundles Vaadin frontend)..."
  ./gradlew bootJar -Pvaadin.productionMode=true -x test
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
  echo "Starting ${APP_NAME} on port ${PORT} with profile '${PROFILE}'..."
  java -jar "${DEPLOY_JAR}" --spring.profiles.active="${PROFILE}" > "$LOG_FILE" 2>&1 &
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
shift || true
parse_profile "$@"

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

---

**2026-09-03 11:46:00**

Please move the link from the left menu to the manual as the last item, separated from the other by a line, and add index and quick links in the manual
