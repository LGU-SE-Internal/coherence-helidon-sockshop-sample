# Helm Chart Quick Start Guide

This guide provides quick commands to deploy and manage the Sock Shop application using Helm.

## Prerequisites Check

```bash
# Check if Helm is installed
helm version

# Check if kubectl is configured
kubectl cluster-info

# Check if Coherence Operator is installed
kubectl get crd coherence.coherence.oracle.com
```

## Install Coherence Operator

If the Coherence Operator is not installed:

```bash
kubectl apply -f https://github.com/oracle/coherence-operator/releases/download/v3.3.4/coherence-operator.yaml
```

Verify installation:
```bash
kubectl get pods -n coherence-operator-system
```

## Quick Installation

### 1. Create Namespace

```bash
kubectl create namespace sockshop
```

### 2. Install Sock Shop

**Full deployment (backend + frontend + load generator):**
```bash
helm install sockshop ./helm/sockshop -n sockshop
```

**Backend and frontend only:**
```bash
helm install sockshop ./helm/sockshop -n sockshop --set loadgen.enabled=false
```

**Backend only:**
```bash
helm install sockshop ./helm/sockshop -n sockshop \
  --set frontend.enabled=false \
  --set loadgen.enabled=false
```

### 3. Verify Installation

```bash
# Check Helm release
helm list -n sockshop

# Check Coherence services
kubectl get coherence -n sockshop

# Check all pods
kubectl get pods -n sockshop

# Watch pods until they're ready
kubectl get pods -n sockshop -w
```

### 4. Access the Application

```bash
# Port forward to frontend
kubectl port-forward -n sockshop service/front-end 8079:80
```

Open http://localhost:8079 in your browser.

## Common Operations

### Check Status

```bash
# Helm release status
helm status sockshop -n sockshop

# Pod status
kubectl get pods -n sockshop

# Service status
kubectl get svc -n sockshop

# Coherence cluster status
kubectl get coherence -n sockshop
```

### View Logs

```bash
# Frontend logs
kubectl logs -n sockshop -l app=front-end -f

# Load generator logs
kubectl logs -n sockshop -l app=loadgen -f

# Specific backend service logs (e.g., carts)
kubectl logs -n sockshop -l coherenceRole=Carts -f

# All logs from a specific pod
kubectl logs -n sockshop <pod-name> -f
```

### Scale Services

```bash
# Scale via Helm upgrade
helm upgrade sockshop ./helm/sockshop -n sockshop \
  --set carts.replicas=3

# Scale directly with kubectl
kubectl scale coherence/carts -n sockshop --replicas=3

# Scale all backend services
for svc in carts catalog orders payment shipping users; do
  kubectl scale coherence/$svc -n sockshop --replicas=3
done
```

### Update Configuration

```bash
# Update load generator settings
helm upgrade sockshop ./helm/sockshop -n sockshop \
  --set loadgen.load.users=100 \
  --set loadgen.load.spawnRate=10

# Update using custom values file
helm upgrade sockshop ./helm/sockshop -n sockshop -f my-values.yaml

# Update specific service image
helm upgrade sockshop ./helm/sockshop -n sockshop \
  --set carts.image.tag=2.11.0
```

## Load Generator Control

### View Load Generator Status

```bash
# Check logs
kubectl logs -n sockshop -l app=loadgen -f

# Check web UI (if enabled)
kubectl port-forward -n sockshop service/loadgen 8089:8089
# Open http://localhost:8089
```

### Stop Load Generation

```bash
kubectl scale deployment loadgen -n sockshop --replicas=0
```

### Resume Load Generation

```bash
kubectl scale deployment loadgen -n sockshop --replicas=1
```

### Change Load Parameters

```bash
# Edit deployment directly
kubectl edit deployment loadgen -n sockshop

# Or use Helm upgrade
helm upgrade sockshop ./helm/sockshop -n sockshop \
  --set loadgen.load.users=200 \
  --set loadgen.load.spawnRate=20 \
  --set loadgen.load.runTime=30m
```

## Troubleshooting

### Check Helm Chart

```bash
# Validate chart
helm lint ./helm/sockshop

# Test template rendering
helm template test ./helm/sockshop -n sockshop

# Dry run installation
helm install test ./helm/sockshop -n sockshop --dry-run --debug
```

### Debug Pod Issues

```bash
# Describe pod
kubectl describe pod <pod-name> -n sockshop

# Check events
kubectl get events -n sockshop --sort-by='.lastTimestamp'

# Check resource usage
kubectl top pods -n sockshop
kubectl top nodes
```

### View Configuration

```bash
# Get current values
helm get values sockshop -n sockshop

# Get all values (including defaults)
helm get values sockshop -n sockshop --all

# View manifest
helm get manifest sockshop -n sockshop
```

### Common Fixes

**Issue: Coherence CRD not found**
```bash
# Install Coherence Operator
kubectl apply -f https://github.com/oracle/coherence-operator/releases/download/v3.3.4/coherence-operator.yaml
```

**Issue: ImagePullBackOff**
```bash
# Check image names and registry
helm get values sockshop -n sockshop | grep image

# Update registry
helm upgrade sockshop ./helm/sockshop -n sockshop \
  --set global.imageRegistry=correct-registry.com
```

**Issue: Pods pending**
```bash
# Check node resources
kubectl describe nodes

# Check pod events
kubectl describe pod <pod-name> -n sockshop
```

## Rollback and History

### View Release History

```bash
helm history sockshop -n sockshop
```

### Rollback to Previous Version

```bash
helm rollback sockshop -n sockshop
```

### Rollback to Specific Revision

```bash
helm rollback sockshop 1 -n sockshop
```

## Upgrade

### Upgrade with New Values

```bash
helm upgrade sockshop ./helm/sockshop -n sockshop \
  --set carts.replicas=3 \
  --set loadgen.load.users=100
```

### Upgrade with Values File

```bash
helm upgrade sockshop ./helm/sockshop -n sockshop -f custom-values.yaml
```

### Upgrade and Reset Values

```bash
helm upgrade sockshop ./helm/sockshop -n sockshop --reset-values
```

## Uninstall

### Remove Sock Shop

```bash
helm uninstall sockshop -n sockshop
```

### Verify Removal

```bash
# Check Helm releases
helm list -n sockshop

# Check remaining resources
kubectl get all -n sockshop
```

### Delete Namespace

```bash
kubectl delete namespace sockshop
```

## Export and Package

### Export Chart as Package

```bash
helm package ./helm/sockshop
```

This creates a `sockshop-1.0.0.tgz` file.

### Install from Package

```bash
helm install sockshop sockshop-1.0.0.tgz -n sockshop
```

## Tips and Best Practices

1. **Use Custom Values Files**: Create environment-specific values files instead of passing many `--set` flags
   ```bash
   helm install sockshop ./helm/sockshop -n sockshop -f prod-values.yaml
   ```

2. **Test Before Deploying**: Always use `--dry-run` for important changes
   ```bash
   helm upgrade sockshop ./helm/sockshop -n sockshop --dry-run -f new-values.yaml
   ```

3. **Monitor Resources**: Keep an eye on resource usage
   ```bash
   kubectl top pods -n sockshop
   ```

4. **Use Labels for Filtering**: Query specific components
   ```bash
   kubectl get pods -n sockshop -l app=front-end
   kubectl get pods -n sockshop -l coherenceCluster=SockShop
   ```

5. **Keep Release Notes**: Check what changed
   ```bash
   helm get notes sockshop -n sockshop
   ```

## Quick Reference

| Task | Command |
|------|---------|
| Install | `helm install sockshop ./helm/sockshop -n sockshop` |
| Upgrade | `helm upgrade sockshop ./helm/sockshop -n sockshop` |
| Uninstall | `helm uninstall sockshop -n sockshop` |
| Status | `helm status sockshop -n sockshop` |
| List | `helm list -n sockshop` |
| Get Values | `helm get values sockshop -n sockshop` |
| History | `helm history sockshop -n sockshop` |
| Rollback | `helm rollback sockshop -n sockshop` |
| Port Forward | `kubectl port-forward -n sockshop service/front-end 8079:80` |
| Logs | `kubectl logs -n sockshop -l app=loadgen -f` |

## Additional Resources

- [Full Chart Documentation](./sockshop/README.md)
- [Values Examples](./sockshop/values-examples.yaml)
- [Main Documentation](../README.md)
- [Load Generator Docs](../loadgen/README.md)

## Support

For issues and questions:
- GitHub Issues: https://github.com/oracle/coherence-helidon-sockshop-sample/issues
- Coherence Community: https://coherence.community/
