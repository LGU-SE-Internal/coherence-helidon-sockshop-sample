# Load Generator Design Document

## Overview

This document explains the design decisions, architecture, and rationale behind the Coherence Helidon Sock Shop load generator implementation.

## Problem Statement

The goal was to design a load generator that:
1. Simulates realistic e-commerce user behavior
2. Provides easy configuration of load patterns and API call ratios
3. Matches actual application usage scenarios
4. Supports multiple deployment modes
5. Provides comprehensive metrics and reporting

## Design Decision: Python + Locust

### Why Locust?

After analyzing three reference implementations:
- **Original Sock Shop** (Python Locust)
- **TrainTicket** (Go-based)
- **DeathStarBench** (Lua + wrk)

We chose to build on the original Locust approach with significant enhancements.

### Advantages

1. **User-Friendly Web UI**
   - Real-time metrics visualization
   - Interactive load control
   - No need to restart tests to adjust parameters
   - Charts and graphs built-in

2. **Realistic Behavior Modeling**
   - Task-based approach naturally models user journeys
   - Weighted tasks reflect actual usage patterns
   - Easy to define complex user flows
   - Supports stateful user sessions

3. **Configuration-Driven**
   - YAML configuration for load patterns
   - No code changes needed for common adjustments
   - Easy to create custom scenarios
   - Version control friendly

4. **Python Ecosystem**
   - Easy to read and modify
   - Large community support
   - Extensive documentation
   - Rich library ecosystem

5. **Deployment Flexibility**
   - Run locally for development
   - Docker for consistent environments
   - Kubernetes for distributed load
   - CI/CD integration ready

### Trade-offs

1. **Performance at Extreme Scale**
   - Go or Lua might achieve higher raw throughput
   - **Mitigation**: Locust supports distributed mode for extreme loads
   - **Reality**: For most use cases, Python is sufficient

2. **Learning Curve**
   - Requires Python knowledge for customization
   - **Mitigation**: Comprehensive documentation and examples
   - **Reality**: Python is widely known and easy to learn

## Architecture

### Component Structure

```
loadgen/
├── locustfile.py          # Core load generator logic
├── config.yaml            # Configuration file
├── requirements.txt       # Python dependencies
├── Dockerfile            # Container definition
├── docker-compose.yaml   # Local testing setup
├── k8s-deployment.yaml   # Kubernetes manifests
├── run-loadgen.sh        # CLI helper script
├── run-loadgen-ui.sh     # Web UI helper script
├── build.sh              # Docker build script
└── examples/             # Example scenarios
    ├── browsing-heavy.yaml
    ├── high-conversion.yaml
    └── black-friday.yaml
```

### User Behavior Model

The load generator simulates five key user scenarios:

1. **Browse Catalogue (40%)**
   - Most common action
   - Users exploring products
   - Various filters and pagination
   - Multiple tags (blue, green, formal, sport)

2. **View Product Details (30%)**
   - Users interested in specific items
   - Product information viewing
   - Image loading (50% probability)
   - Second most common action

3. **User Login/Registration (10%)**
   - Authentication flow
   - New user registration
   - Returning user login
   - Moderate frequency

4. **Shopping Cart Operations (15%)**
   - Add items to cart (70% probability)
   - Update quantities
   - Remove items
   - View cart contents

5. **Place Order (5%)**
   - Complete purchase flow
   - Address management
   - Payment card handling
   - Lowest frequency (realistic conversion rate)

### Why These Weights?

These weights are based on typical e-commerce conversion funnels:

- **Traffic Distribution**: Most users browse but don't buy
- **Conversion Rates**: Typical e-commerce conversion is 2-5%
- **Engagement Patterns**: Users view multiple products before deciding
- **Cart Abandonment**: Many add to cart but don't complete purchase

### Configuration Model

The YAML configuration allows easy customization:

```yaml
scenarios:
  my_scenario:
    users: 100          # Concurrent users
    spawn_rate: 10      # Users added per second
    run_time: "15m"     # Test duration
    description: "..."  # Human-readable description

api_weights:
  browse_catalogue: 40
  view_product_details: 30
  user_login_flow: 10
  shopping_cart: 15
  place_order: 5

user_behavior:
  min_wait: 1.0         # Think time between actions
  max_wait: 3.0
  probabilities:
    view_image: 0.5
    add_to_cart: 0.7
    # ... more settings
```

## Comparison with Alternatives

### vs. Original Sock Shop Load Test

**Improvements:**
- More realistic user behavior with stateful sessions
- Configuration-driven (no code changes for scenarios)
- Better error handling and retry logic
- Comprehensive documentation
- Multiple deployment options
- Example scenarios included

**Maintained:**
- Python + Locust foundation
- REST API testing focus
- Docker support
- **Original API structure compatibility**:
  - Uses `/cart` endpoint (singular)
  - Includes frontend page requests (`/`, `/category.html`, `/detail.html`, `/basket.html`)
  - Proper authentication flow
  - No requests to non-existent resources

### vs. TrainTicket Go Load Generator

**Our Advantages:**
- Easier to understand and modify (Python vs Go)
- Built-in web UI for interactive testing
- Rich reporting and visualization
- More accessible for non-Go developers

**Their Advantages:**
- Potentially better performance at extreme scale
- Lower memory footprint
- Faster compilation

**Why We Chose Python:**
- Most teams can read/modify Python
- Web UI is invaluable for testing
- Performance is adequate for most needs
- Extensive Locust ecosystem

### vs. DeathStarBench Lua Load Generator

**Our Advantages:**
- More user-friendly configuration (YAML vs Lua)
- Better suited for complex HTTP workflows
- Easier to understand for non-programmers
- Stateful user sessions

**Their Advantages:**
- wrk + Lua can achieve very high throughput
- Lower latency overhead
- More suitable for low-level performance testing

**Why We Chose Locust:**
- Better for testing business logic
- More realistic user behavior modeling
- Configuration over scripting
- Better observability

## Predefined Scenarios

### Light (10 users, 5 min)
- **Use Case**: Basic functional testing, validation
- **Target**: Development environments
- **Expected**: All requests succeed, low latency

### Medium (50 users, 15 min)
- **Use Case**: Typical production traffic simulation
- **Target**: Staging environments
- **Expected**: Stable performance, minimal failures

### Heavy (200 users, 30 min)
- **Use Case**: Peak period simulation, stress testing
- **Target**: Performance testing environments
- **Expected**: System remains stable under sustained load

### Spike (500 users, 10 min)
- **Use Case**: Sudden traffic surge testing
- **Target**: Resilience testing
- **Expected**: System handles sudden load, graceful degradation if needed

## Example Scenarios

### Browsing-Heavy
- Simulates marketing campaign driving traffic
- High browsing (50%), low conversion (2%)
- Tests catalogue and search performance
- Validates caching effectiveness

### High-Conversion
- Simulates engaged, high-intent users
- Lower browsing (25%), higher conversion (15%)
- Tests order processing and payment systems
- Validates database transaction handling

### Black-Friday
- Simulates extreme shopping events
- 1000 users, balanced distribution
- Tests system capacity limits
- Validates auto-scaling and resilience

## Deployment Options

### Local Python
- **Pros**: Quick setup, no containers needed, easy debugging
- **Cons**: Environment dependencies, less portable
- **Use Case**: Development, quick tests

### Docker
- **Pros**: Consistent environment, easy to share, portable
- **Cons**: Requires Docker, slightly more setup
- **Use Case**: Local testing, CI/CD integration

### Kubernetes
- **Pros**: Production-like, scalable, persistent
- **Cons**: Requires cluster, more complex setup
- **Use Case**: Long-running tests, distributed load

## Metrics and Reporting

### Real-Time Metrics (Web UI)
- Request count
- Failures
- Response times (median, 95th, 99th percentile)
- Requests per second
- Current user count

### CSV Export
- Time-series data for analysis
- Per-endpoint statistics
- Failure details
- Custom percentiles

### HTML Report
- Summary statistics
- Response time charts
- Failure analysis
- Downloadable for sharing

## Security Considerations

1. **Test Data**: Uses generated test data, no real user information
2. **Authentication**: Supports Basic Auth for testing
3. **Rate Limiting**: Configurable spawn rate to avoid overwhelming systems
4. **Credentials**: No hardcoded credentials (generated per test)
5. **SSL**: Supports HTTPS endpoints

## Future Enhancements

Potential improvements for future versions:

1. **Additional Protocols**
   - gRPC support for backend services
   - WebSocket for real-time features

2. **Advanced Scenarios**
   - Multi-step user journeys
   - A/B testing simulation
   - Different user personas

3. **Enhanced Reporting**
   - Integration with Grafana/Prometheus
   - Custom dashboards
   - Automated performance regression detection

4. **Load Patterns**
   - Gradual ramp-up/ramp-down
   - Cyclic patterns (daily/weekly)
   - Random spikes

5. **Data-Driven**
   - CSV input for test data
   - Database-backed user profiles
   - Replay of production traffic

## Best Practices

### When to Use Each Scenario

1. **Development**: Light scenario for quick validation
2. **PR Testing**: Light or Medium for CI/CD pipelines
3. **Staging**: Medium or Heavy for release validation
4. **Pre-Production**: Heavy and Spike for capacity planning
5. **Production**: Monitor-only (consider separate monitoring solution)

### Interpreting Results

**Good Performance:**
- Response times < 500ms (P95)
- Failure rate < 1%
- Stable throughput
- Linear scaling with users

**Warning Signs:**
- Response times > 1s (P95)
- Failure rate 1-5%
- Increasing response times over test duration
- Database connection pool exhaustion

**Critical Issues:**
- Response times > 5s (P95)
- Failure rate > 10%
- Services crashing
- OOM errors

## Conclusion

This load generator provides a comprehensive, flexible, and user-friendly solution for testing the Coherence Helidon Sock Shop application. The design balances ease of use, realistic behavior modeling, and deployment flexibility, making it suitable for various testing scenarios from development to production readiness validation.

The Python + Locust foundation provides a solid base that can be easily extended and customized as testing needs evolve, while the configuration-driven approach ensures that common adjustments can be made without code changes.
