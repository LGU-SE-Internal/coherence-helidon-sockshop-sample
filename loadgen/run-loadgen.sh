#!/bin/bash
#
# Run Load Generator for Coherence Helidon Sock Shop
#
# Usage: ./run-loadgen.sh [scenario] [target_host]
#   scenario: light, medium, heavy, spike (default: light)
#   target_host: Target application URL (default: http://localhost:8079)
#

set -e

# Default values
SCENARIO=${1:-light}
TARGET_HOST=${2:-http://localhost:8079}
CONFIG_FILE="config.yaml"

# Check if config file exists
if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Configuration file $CONFIG_FILE not found!"
    exit 1
fi

# Parse configuration using Python
read_config() {
    python3 - <<EOF
import yaml
import sys

with open('$CONFIG_FILE', 'r') as f:
    config = yaml.safe_load(f)

scenario = config['scenarios'].get('$SCENARIO')
if not scenario:
    print(f"Error: Scenario '$SCENARIO' not found in configuration!", file=sys.stderr)
    sys.exit(1)

print(f"{scenario['users']}")
print(f"{scenario['spawn_rate']}")
print(f"{scenario['run_time']}")
print(f"{scenario['description']}")
EOF
}

# Read scenario configuration
CONFIG_VALUES=$(read_config)
if [ $? -ne 0 ]; then
    echo "$CONFIG_VALUES"
    exit 1
fi

USERS=$(echo "$CONFIG_VALUES" | sed -n '1p')
SPAWN_RATE=$(echo "$CONFIG_VALUES" | sed -n '2p')
RUN_TIME=$(echo "$CONFIG_VALUES" | sed -n '3p')
DESCRIPTION=$(echo "$CONFIG_VALUES" | sed -n '4p')

echo "=========================================="
echo "Sock Shop Load Generator"
echo "=========================================="
echo "Scenario: $SCENARIO"
echo "Description: $DESCRIPTION"
echo "Target: $TARGET_HOST"
echo "Users: $USERS"
echo "Spawn Rate: $SPAWN_RATE users/sec"
echo "Duration: $RUN_TIME"
echo "=========================================="
echo ""

# Check if locust is installed
if ! command -v locust &> /dev/null; then
    echo "Error: locust is not installed!"
    echo "Install it with: pip install -r requirements.txt"
    exit 1
fi

# Run locust with configuration
locust \
    -f locustfile.py \
    --host="$TARGET_HOST" \
    --users="$USERS" \
    --spawn-rate="$SPAWN_RATE" \
    --run-time="$RUN_TIME" \
    --headless \
    --csv=loadtest_results \
    --html=loadtest_report.html

echo ""
echo "Load test completed!"
echo "Results saved to:"
echo "  - loadtest_results_*.csv"
echo "  - loadtest_report.html"
