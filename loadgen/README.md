# Load Generator for Coherence Helidon Sock Shop

A flexible, configuration-driven load generator for testing the Coherence Helidon Sock Shop microservices application. Built with Python and [Locust](https://locust.io/), it simulates realistic e-commerce user behavior with configurable load patterns.

**Compatible with original WeaveSocks front-end API structure**, using the correct `/cart` (singular) endpoint and frontend page requests (`/`, `/category.html`, `/detail.html`, `/basket.html`).

## Features

- **Realistic User Behavior**: Simulates actual e-commerce shopping patterns
  - Browse catalogue (40% of traffic)
  - View product details (30%)
  - User login/registration (10%)
  - Shopping cart operations (15%)
  - Order placement (5%)

- **API Compatibility**: Follows original sockshop load test pattern
  - Uses `/cart` endpoint (not `/carts/{id}`)
  - Includes frontend page requests
  - Proper authentication flow
  - Avoids requests to non-existent resources

- **Flexible Configuration**: YAML-based configuration for easy customization
  - Multiple predefined scenarios (light, medium, heavy, spike)
  - Adjustable API call ratios
  - Configurable user behavior parameters
  - No code changes needed for most adjustments

- **Multiple Deployment Options**:
  - Command-line (headless mode)
  - Web UI for interactive testing
  - Docker container
  - Kubernetes deployment

- **Comprehensive Reporting**:
  - Real-time metrics in web UI
  - CSV statistics export
  - HTML test reports
  - Response time percentiles

## Quick Start

### Prerequisites

- Python 3.11+
- pip

### Local Installation

1. Install dependencies:
```bash
pip install -r requirements.txt
```

2. Run with web UI (recommended for getting started):
```bash
./run-loadgen-ui.sh http://your-sockshop-host:8079
```

3. Open http://localhost:8089 in your browser to configure and start the load test.

### Headless Mode (Command Line)

Run a predefined scenario without web UI:

```bash
# Light load (10 users)
./run-loadgen.sh light http://your-sockshop-host:8079

# Medium load (50 users)
./run-loadgen.sh medium http://your-sockshop-host:8079

# Heavy load (200 users)
./run-loadgen.sh heavy http://your-sockshop-host:8079

# Spike test (500 users)
./run-loadgen.sh spike http://your-sockshop-host:8079
```

Results are saved as CSV files and an HTML report.

## Configuration

The `config.yaml` file allows you to customize load patterns without modifying Python code.

### Predefined Scenarios

| Scenario | Users | Spawn Rate | Duration | Use Case |
|----------|-------|------------|----------|----------|
| light    | 10    | 2/sec      | 5m       | Basic testing, validation |
| medium   | 50    | 5/sec      | 15m      | Normal production traffic |
| heavy    | 200   | 10/sec     | 30m      | Peak periods, stress testing |
| spike    | 500   | 50/sec     | 10m      | Sudden traffic surge testing |

### API Weight Distribution

Reflects realistic e-commerce user behavior:

```yaml
api_weights:
  browse_catalogue: 40      # Most users just browse
  view_product_details: 30  # Interested users view details
  user_login_flow: 10       # Some users register/login
  shopping_cart: 15         # Fewer users add to cart
  place_order: 5            # Only ~5% complete purchase
```

### Customizing Scenarios

Edit `config.yaml` to add your own scenarios:

```yaml
scenarios:
  my_custom_scenario:
    users: 100
    spawn_rate: 10
    run_time: "20m"
    description: "My custom load pattern"
```

Then run it:
```bash
./run-loadgen.sh my_custom_scenario http://target-host
```

## Docker Deployment

### Build Docker Image

```bash
docker build -t loadgen:latest .
```

### Run with Docker

**Web UI Mode:**
```bash
docker run -p 8089:8089 loadgen:latest \
  -f locustfile.py \
  --host=http://your-sockshop-host:8079
```

**Headless Mode:**
```bash
docker run loadgen:latest \
  -f locustfile.py \
  --host=http://your-sockshop-host:8079 \
  --users=50 \
  --spawn-rate=5 \
  --run-time=10m \
  --headless
```

## Kubernetes Deployment

### Prerequisites

- Kubernetes cluster
- Sock Shop application deployed in `sockshop` namespace

### Deploy Load Generator

```bash
kubectl apply -f k8s-deployment.yaml -n sockshop
```

This creates:
- ConfigMap with load generator configuration
- Service exposing the web UI
- Deployment running the load generator **continuously in headless mode**

**Note**: The deployment automatically starts generating load (50 users) as soon as it's deployed. The load generator will run indefinitely until you delete the deployment or scale it to 0 replicas.

### Control the Load Generator

**To adjust load levels:**
```bash
# Edit the deployment to change --users and --spawn-rate
kubectl edit deployment loadgen -n sockshop
```

**To stop load generation:**
```bash
# Scale to 0 replicas (keeps configuration)
kubectl scale deployment loadgen -n sockshop --replicas=0

# Or delete the deployment entirely
kubectl delete deployment loadgen -n sockshop
```

**To resume load generation:**
```bash
kubectl scale deployment loadgen -n sockshop --replicas=1
```

### View Load Generator Logs

Monitor the load generator output:
```bash
kubectl logs -n sockshop -l app=loadgen -f
```

### Run One-Time Load Test

For temporary load testing (not continuous):
```bash
kubectl run loadgen-test -n sockshop \
  --image=loadgen:latest \
  --restart=Never \
  --rm -it \
  -- \
  -f locustfile.py \
  --host=http://front-end.sockshop.svc.cluster.local \
  --users=50 \
  --spawn-rate=5 \
  --run-time=10m \
  --headless
```

## Advanced Usage

### Custom Locust Options

You can pass any Locust command-line options directly:

```bash
locust -f locustfile.py \
  --host=http://localhost:8079 \
  --users=100 \
  --spawn-rate=10 \
  --run-time=30m \
  --headless \
  --csv=results \
  --html=report.html \
  --only-summary
```

### Environment Variables

Configure behavior via environment variables:

```bash
export LOCUST_MIN_WAIT=1.0
export LOCUST_MAX_WAIT=5.0
locust -f locustfile.py --host=http://localhost:8079
```

### Distributed Load Testing

Run Locust in distributed mode for higher load:

**Master:**
```bash
locust -f locustfile.py --master --host=http://target-host
```

**Workers (run multiple instances):**
```bash
locust -f locustfile.py --worker --master-host=localhost
```

## User Scenarios

The load generator simulates the following user journey (based on original sockshop pattern):

1. **Browse Catalogue** (40% of requests)
   - Get product catalogue via `/catalogue` API
   - View category page (`/category.html`)
   - Realistic browsing behavior

2. **View Product Details** (30% of requests)
   - View product detail page (`/detail.html?id={id}`)
   - Fetch product details from API
   - Load product images
   - Read descriptions and prices

3. **User Management** (10% of requests)
   - Login with credentials via `/login`
   - Register new accounts via `/register`
   - Handle authentication (Basic Auth)

4. **Shopping Cart** (15% of requests)
   - View basket page (`/basket.html`)
   - Add items via `/cart` POST
   - Clear cart via `/cart` DELETE
   - Proper cart management

5. **Place Order** (5% of requests)
   - Complete purchase flow from home to checkout
   - Visit home page (`/`)
   - Browse and select products
   - Login/register if needed
   - Add to cart
   - Submit order via `/orders`

6. **Direct Payment Service Testing** (3% of requests)
   - Test payment authorization endpoint directly via `/payments` POST
   - Ensures payment service is exercised and traced
   - Simulates what orders service does internally
   - Provides independent verification of payment service health

7. **Direct Shipping Service Testing** (3% of requests)
   - Test shipping endpoint directly via `/shipping` POST
   - Ensures shipping service is exercised and traced
   - Simulates what orders service does internally
   - Provides independent verification of shipping service health

## Monitoring and Metrics

### Web UI Metrics

The Locust web UI (http://localhost:8089) provides:
- Real-time request statistics
- Response time charts
- Failure rates
- Requests per second
- Current number of users

### CSV Output

When running in headless mode, statistics are saved to CSV files:
- `loadtest_results_stats.csv` - Request statistics
- `loadtest_results_failures.csv` - Failed requests
- `loadtest_results_stats_history.csv` - Time-series data

### HTML Report

A comprehensive HTML report is generated at the end of each test:
- `loadtest_report.html`

## Troubleshooting

### Connection Refused

If you get "Connection refused" errors:
- Verify the target host is correct and accessible
- Check if Sock Shop is running: `kubectl get pods -n sockshop`
- Ensure services are exposed correctly

### High Failure Rate

If you see many failed requests:
- Check if backend services are healthy
- Verify resource limits (CPU/memory) on backend services
- Reduce load (decrease users or spawn rate)
- Check application logs for errors

### Locust Not Found

If `locust` command is not found:
```bash
pip install -r requirements.txt
```

Or use the full path:
```bash
python -m locust -f locustfile.py --host=http://target-host
```

## Comparison with Other Load Generators

### vs. Original Sock Shop Load Test (Python Locust)

**Advantages:**
- More realistic user behavior with weighted tasks
- Configuration-driven (no code changes for scenarios)
- Better error handling and retry logic
- Comprehensive documentation
- Kubernetes-ready deployment

### vs. TrainTicket Go Load Generator

**Advantages:**
- Easier to read and modify (Python vs Go)
- Built-in web UI for interactive testing
- Rich reporting and visualization
- Extensive Locust ecosystem and documentation

**Trade-offs:**
- Go may have slightly better performance at extreme scale
- Python is more accessible for quick modifications

### vs. DeathStarBench Lua Load Generator

**Advantages:**
- More user-friendly configuration (YAML vs Lua)
- Better suited for HTTP-based APIs
- Easier to understand for non-programmers
- Rich metrics and reporting

**Trade-offs:**
- Lua with wrk can achieve higher raw throughput
- DeathStarBench approach may be better for very low-latency testing

## Design Rationale

This load generator uses Python with Locust because:

1. **Realistic Behavior**: Task-based approach naturally models user journeys
2. **Flexibility**: YAML configuration allows non-programmers to adjust load
3. **Observability**: Web UI provides real-time insights
4. **Maintainability**: Python code is readable and well-documented
5. **Ecosystem**: Large community, extensive plugins, good documentation
6. **Deployment**: Works locally, in Docker, and in Kubernetes
7. **API Testing**: Perfect for REST API testing with complex workflows

## Contributing

To add new user scenarios:

1. Edit `locustfile.py` and add a new `@task` method
2. Adjust the task weight to reflect the desired frequency
3. Update `config.yaml` if new configuration is needed
4. Document the new scenario in this README

## References

- [Locust Documentation](https://docs.locust.io/)
- [Original Sock Shop](https://microservices-demo.github.io/)
- [Coherence Helidon Sock Shop](../README.md)

## License

Universal Permissive License (UPL), Version 1.0
