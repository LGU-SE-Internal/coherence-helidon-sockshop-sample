# Sock Shop Helm Chart

This Helm chart deploys the complete Coherence Helidon Sock Shop application, including:
- 6 backend microservices (carts, catalog, orders, payment, shipping, users) based on Coherence
- Frontend service
- Load generator for performance testing

## Prerequisites

- Kubernetes 1.16+
- Helm 3.0+
- Coherence Operator installed in the cluster

### Installing the Coherence Operator

Follow the [Coherence Operator Quick Start](https://oracle.github.io/coherence-operator/docs/latest/#/docs/about/03_quickstart) to install the operator.

Quick installation:
```bash
kubectl apply -f https://github.com/oracle/coherence-operator/releases/download/v3.3.4/coherence-operator.yaml
```

## Installation

### Create Namespace

```bash
kubectl create namespace sockshop
```

### Install the Chart

```bash
helm install sockshop helm/sockshop --namespace sockshop
```

### Install with Custom Values

```bash
helm install sockshop helm/sockshop --namespace sockshop \
  --set global.imageRegistry=your-registry.com \
  --set frontend.replicas=2 \
  --set loadgen.enabled=false
```

### Install from Repository Root

```bash
cd /path/to/coherence-helidon-sockshop-sample
helm install sockshop ./helm/sockshop -n sockshop
```

## Uninstallation

```bash
helm uninstall sockshop --namespace sockshop
```

## Configuration

The following table lists the configurable parameters and their default values.

### Global Parameters

| Parameter | Description | Default |
|-----------|-------------|---------|
| `global.imageRegistry` | Docker image registry | `10.10.10.240/library` |
| `global.imagePullPolicy` | Image pull policy | `IfNotPresent` |
| `global.coherenceCluster` | Coherence cluster name | `SockShop` |

### Frontend Parameters

| Parameter | Description | Default |
|-----------|-------------|---------|
| `frontend.enabled` | Enable frontend deployment | `true` |
| `frontend.replicas` | Number of frontend replicas | `1` |
| `frontend.image.repository` | Frontend image repository | `ss-frontend` |
| `frontend.image.tag` | Frontend image tag | `3eea74f` |
| `frontend.service.type` | Frontend service type | `NodePort` |
| `frontend.service.port` | Frontend service port | `80` |
| `frontend.resources.requests.cpu` | CPU resource requests | `100m` |
| `frontend.resources.requests.memory` | Memory resource requests | `400Mi` |
| `frontend.resources.limits.cpu` | CPU resource limits | `1000m` |
| `frontend.resources.limits.memory` | Memory resource limits | `400Mi` |

### Load Generator Parameters

| Parameter | Description | Default |
|-----------|-------------|---------|
| `loadgen.enabled` | Enable load generator | `true` |
| `loadgen.replicas` | Number of load generator replicas | `1` |
| `loadgen.image.repository` | Load generator image repository | `ss-loadgen` |
| `loadgen.image.tag` | Load generator image tag | `879a1cd` |
| `loadgen.load.users` | Number of concurrent users | `50` |
| `loadgen.load.spawnRate` | Users spawned per second | `5` |
| `loadgen.load.runTime` | Test duration (empty for continuous) | `""` |
| `loadgen.load.targetHost` | Target host for load testing | `http://front-end.sockshop.svc.cluster.local` |

### Backend Services Parameters

All backend services (carts, catalog, orders, payment, shipping, users) share common parameters:

| Parameter | Description | Default |
|-----------|-------------|---------|
| `<service>.enabled` | Enable the service | `true` |
| `<service>.replicas` | Number of replicas | `1` |
| `<service>.image.repository` | Image repository | `ss-<service>` |
| `<service>.image.tag` | Image tag | `2.7.0` |

### Coherence Common Parameters

| Parameter | Description | Default |
|-----------|-------------|---------|
| `coherence.jvm.memory.heapSize` | JVM heap size | `2g` |
| `coherence.metrics.enabled` | Enable Coherence metrics | `true` |
| `coherence.serviceMonitor.enabled` | Enable Prometheus ServiceMonitor | `true` |

## Usage Examples

### Install with Custom Image Registry

```bash
helm install sockshop helm/sockshop -n sockshop \
  --set global.imageRegistry=my-registry.example.com
```

### Install Backend Only (No Frontend or Load Generator)

```bash
helm install sockshop helm/sockshop -n sockshop \
  --set frontend.enabled=false \
  --set loadgen.enabled=false
```

### Install with Different Load Configuration

```bash
helm install sockshop helm/sockshop -n sockshop \
  --set loadgen.load.users=100 \
  --set loadgen.load.spawnRate=10 \
  --set loadgen.load.runTime=30m
```

### Scale Backend Services

```bash
helm upgrade sockshop helm/sockshop -n sockshop \
  --set carts.replicas=3 \
  --set catalog.replicas=3 \
  --set orders.replicas=3 \
  --set payment.replicas=3 \
  --set shipping.replicas=3 \
  --set users.replicas=3
```

### Use Custom Values File

Create a `custom-values.yaml`:
```yaml
global:
  imageRegistry: my-registry.example.com

frontend:
  replicas: 2
  service:
    type: LoadBalancer

loadgen:
  enabled: true
  load:
    users: 200
    spawnRate: 20

carts:
  replicas: 3

catalog:
  replicas: 3
```

Install with custom values:
```bash
helm install sockshop helm/sockshop -n sockshop -f custom-values.yaml
```

## Accessing the Application

### Frontend

After installation, access the frontend using:

**Port Forward:**
```bash
kubectl port-forward -n sockshop service/front-end 8079:80
```
Then open http://localhost:8079

**NodePort (default):**
```bash
export NODE_PORT=$(kubectl get -n sockshop -o jsonpath="{.spec.ports[0].nodePort}" services front-end)
export NODE_IP=$(kubectl get nodes -n sockshop -o jsonpath="{.items[0].status.addresses[0].address}")
echo "Frontend URL: http://$NODE_IP:$NODE_PORT/"
```

### Load Generator Web UI

```bash
kubectl port-forward -n sockshop service/loadgen 8089:8089
```
Then open http://localhost:8089

### View Load Generator Logs

```bash
kubectl logs -n sockshop -l app=loadgen -f
```

## Monitoring

### Check Service Status

```bash
# Check Coherence services
kubectl get coherence -n sockshop

# Check all pods
kubectl get pods -n sockshop

# Check services
kubectl get svc -n sockshop
```

### Scale Services

```bash
# Scale a single service
kubectl scale coherence/carts -n sockshop --replicas=3

# Scale all backend services
for name in carts catalog orders payment shipping users; do
  kubectl scale coherence/$name -n sockshop --replicas=3
done
```

## Load Generator Control

### Stop Load Generation

```bash
kubectl scale deployment loadgen -n sockshop --replicas=0
```

### Resume Load Generation

```bash
kubectl scale deployment loadgen -n sockshop --replicas=1
```

### Change Load Parameters

Edit the deployment:
```bash
kubectl edit deployment loadgen -n sockshop
```

Or upgrade with Helm:
```bash
helm upgrade sockshop helm/sockshop -n sockshop \
  --set loadgen.load.users=100 \
  --set loadgen.load.spawnRate=10
```

## Troubleshooting

### Check Helm Release Status

```bash
helm status sockshop -n sockshop
```

### View Rendered Templates

```bash
helm template sockshop helm/sockshop -n sockshop
```

### Debug Installation Issues

```bash
helm install sockshop helm/sockshop -n sockshop --dry-run --debug
```

### Check Pod Logs

```bash
# Frontend
kubectl logs -n sockshop -l app=front-end

# Load generator
kubectl logs -n sockshop -l app=loadgen

# Backend service (e.g., carts)
kubectl logs -n sockshop -l coherenceRole=Carts
```

### Common Issues

1. **Coherence Operator Not Installed**
   - Error: `no matches for kind "Coherence"`
   - Solution: Install Coherence Operator first (see Prerequisites)

2. **Image Pull Errors**
   - Check `global.imageRegistry` setting
   - Verify image tags are correct
   - Check if ImagePullSecrets are needed

3. **Pods Not Starting**
   - Check resource limits: `kubectl describe pod <pod-name> -n sockshop`
   - View logs: `kubectl logs <pod-name> -n sockshop`

## Customization

### Custom Configuration

You can customize the load generator configuration by modifying `loadgen.config` in values.yaml:

```yaml
loadgen:
  config:
    scenarios:
      custom:
        users: 150
        spawn_rate: 15
        run_time: "20m"
        description: "Custom load scenario"
    api_weights:
      browse_catalogue: 50
      view_product_details: 25
      user_login_flow: 10
      shopping_cart: 10
      place_order: 5
```

### Adding Environment Variables

To add custom environment variables to any service, you can extend the chart or modify the templates.

## Upgrading

### Upgrade Release

```bash
helm upgrade sockshop helm/sockshop -n sockshop
```

### Upgrade with New Values

```bash
helm upgrade sockshop helm/sockshop -n sockshop \
  --set loadgen.load.users=200
```

### Rollback

```bash
# List release history
helm history sockshop -n sockshop

# Rollback to previous version
helm rollback sockshop -n sockshop

# Rollback to specific revision
helm rollback sockshop 1 -n sockshop
```

## Integration with Monitoring Tools

### Prometheus

The chart automatically creates ServiceMonitor resources if `coherence.serviceMonitor.enabled=true` (default).

### Grafana

Import Coherence dashboards from the [Coherence Operator repository](https://oracle.github.io/coherence-operator/docs/latest/#/metrics/030_importing).

### Jaeger

For distributed tracing, deploy Jaeger and configure the backend services. See the [main documentation](../../doc/complete-application-deployment.md) for details.

## Additional Resources

- [Coherence Helidon Sock Shop Documentation](../../README.md)
- [Load Generator Documentation](../../loadgen/README.md)
- [Coherence Operator Documentation](https://oracle.github.io/coherence-operator/)
- [Helm Documentation](https://helm.sh/docs/)

## License

Universal Permissive License (UPL), Version 1.0
