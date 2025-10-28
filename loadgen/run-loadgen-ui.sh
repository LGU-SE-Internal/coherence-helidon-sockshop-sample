#!/bin/bash
#
# Run Load Generator with Web UI for Coherence Helidon Sock Shop
#
# Usage: ./run-loadgen-ui.sh [target_host] [port]
#   target_host: Target application URL (default: http://localhost:8079)
#   port: Web UI port (default: 8089)
#

set -e

TARGET_HOST=${1:-http://localhost:8079}
WEB_PORT=${2:-8089}

echo "=========================================="
echo "Sock Shop Load Generator - Web UI Mode"
echo "=========================================="
echo "Target: $TARGET_HOST"
echo "Web UI: http://localhost:$WEB_PORT"
echo "=========================================="
echo ""
echo "Starting Locust web interface..."
echo "Open http://localhost:$WEB_PORT in your browser to control the load test"
echo ""

# Check if locust is installed
if ! command -v locust &> /dev/null; then
    echo "Error: locust is not installed!"
    echo "Install it with: pip install -r requirements.txt"
    exit 1
fi

# Run locust with web UI
locust \
    -f locustfile.py \
    --host="$TARGET_HOST" \
    --web-port="$WEB_PORT"
