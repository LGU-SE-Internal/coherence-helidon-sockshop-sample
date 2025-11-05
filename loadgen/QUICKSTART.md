# Load Generator Quick Start Guide

This guide will get you up and running with the Sock Shop load generator in under 5 minutes.

## Option 1: Docker (Recommended)

### Prerequisites
- Docker installed
- Sock Shop running and accessible

### Steps

1. **Build the image:**
   ```bash
   cd loadgen
   ./build.sh
   ```

2. **Run with web UI:**
   ```bash
   docker run -p 8089:8089 loadgen:latest \
     -f locustfile.py \
     --host=http://your-sockshop-host:8079
   ```

3. **Open browser:**
   - Navigate to http://localhost:8089
   - Enter number of users and spawn rate
   - Click "Start swarming"

That's it! You're now generating load.

## Option 2: Kubernetes

### Prerequisites
- Kubernetes cluster
- kubectl configured
- Sock Shop deployed in cluster

### Steps

1. **Build and push image:**
   ```bash
   cd loadgen
   docker build -t your-registry/loadgen:latest .
   docker push your-registry/loadgen:latest
   ```

2. **Update k8s-deployment.yaml:**
   - Change `image: loadgen:latest` to `image: your-registry/loadgen:latest`
   - Update `TARGET_HOST` environment variable if needed

3. **Deploy:**
   ```bash
   kubectl apply -f k8s-deployment.yaml -n sockshop
   ```
   
   **Important**: The load generator starts automatically and runs **continuously** (no time limit). It begins generating load with 50 users as soon as the pod is ready.

4. **Monitor load generation:**
   ```bash
   kubectl logs -n sockshop -l app=loadgen -f
   ```

5. **To stop load generation:**
   ```bash
   # Scale to 0 (or delete deployment)
   kubectl scale deployment loadgen -n sockshop --replicas=0
   ```

6. **To adjust load (optional):**
   ```bash
   # Edit deployment to change --users and --spawn-rate
   kubectl edit deployment loadgen -n sockshop
   ```

## Option 3: Local Python

### Prerequisites
- Python 3.11+
- pip
- Sock Shop running locally

### Steps

1. **Install dependencies:**
   ```bash
   cd loadgen
   pip install -r requirements.txt
   ```

2. **Run a test scenario:**
   ```bash
   ./run-loadgen.sh light http://localhost:8079
   ```

3. **View results:**
   - Check `loadtest_report.html` in your browser
   - Review CSV files for detailed metrics

## Common Scenarios

### Scenario 1: Quick Smoke Test
```bash
./run-loadgen.sh light http://localhost:8079
```
- 10 users, 5 minutes
- Validates basic functionality

### Scenario 2: Realistic Load
```bash
./run-loadgen.sh medium http://localhost:8079
```
- 50 users, 15 minutes
- Simulates typical traffic

### Scenario 3: Stress Test
```bash
./run-loadgen.sh heavy http://localhost:8079
```
- 200 users, 30 minutes
- Tests system limits

### Scenario 4: Interactive Testing
```bash
./run-loadgen-ui.sh http://localhost:8079
```
- Opens web UI for manual control
- Adjust users/spawn rate in real-time

## What to Expect

### Successful Test Indicators
- ✅ Response times < 500ms for most requests
- ✅ Failure rate < 1%
- ✅ Throughput scales with user count
- ✅ No 5xx errors from backend

### Warning Signs
- ⚠️ Response times > 2s
- ⚠️ Failure rate > 5%
- ⚠️ Many 503/504 errors
- ⚠️ Response times increasing over time

### Critical Issues
- ❌ Failure rate > 50%
- ❌ Backend services crashing
- ❌ Database connection errors
- ❌ Out of memory errors

## Troubleshooting

**Problem: Connection refused**
```bash
# Check if Sock Shop is running
kubectl get pods -n sockshop

# Verify service endpoints
kubectl get svc -n sockshop
```

**Problem: High failure rate**
```bash
# Check backend logs
kubectl logs -n sockshop -l app=carts --tail=50

# Check resource usage
kubectl top pods -n sockshop
```

**Problem: Import errors (Python)**
```bash
# Reinstall dependencies
pip install --force-reinstall -r requirements.txt
```

## Next Steps

- Read [README.md](README.md) for detailed documentation
- Explore [examples/](examples/) for custom scenarios
- Customize [config.yaml](config.yaml) for your needs
- Integrate with CI/CD pipeline

## Support

For issues or questions:
1. Check the main [README.md](README.md)
2. Review existing GitHub issues
3. Create a new issue with details

Happy load testing! 🚀
