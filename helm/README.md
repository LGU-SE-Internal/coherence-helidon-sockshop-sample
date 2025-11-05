# Helm Charts for Coherence Helidon Sock Shop

This directory contains Helm charts for deploying the Coherence Helidon Sock Shop application.

## Available Charts

### sockshop

The main Helm chart that deploys the complete Sock Shop application including:
- **Backend Services**: 6 microservices (carts, catalog, orders, payment, shipping, users) built with Helidon and Coherence
- **Frontend**: Web UI for the sock shop
- **Load Generator**: Automated load testing tool based on Locust

For detailed documentation, see [sockshop/README.md](./sockshop/README.md).

## Quick Start

### Prerequisites

1. Kubernetes cluster (1.16+)
2. Helm 3.0+
3. Coherence Operator installed

Install Coherence Operator:
```bash
kubectl apply -f https://github.com/oracle/coherence-operator/releases/download/v3.3.4/coherence-operator.yaml
```

### Installation

1. Create a namespace:
```bash
kubectl create namespace sockshop
```

2. Install the chart:
```bash
helm install sockshop ./helm/sockshop -n sockshop
```

3. Access the application:
```bash
kubectl port-forward -n sockshop service/front-end 8079:80
```

Open http://localhost:8079 in your browser.

### Verify Installation

```bash
# Check Coherence services
kubectl get coherence -n sockshop

# Check all pods
kubectl get pods -n sockshop

# Check Helm release
helm list -n sockshop
```

## Configuration

The chart can be configured via values.yaml or command-line parameters.

### Common Configuration Examples

**Deploy without load generator:**
```bash
helm install sockshop ./helm/sockshop -n sockshop --set loadgen.enabled=false
```

**Use custom image registry:**
```bash
helm install sockshop ./helm/sockshop -n sockshop --set global.imageRegistry=my-registry.com
```

**Scale backend services:**
```bash
helm install sockshop ./helm/sockshop -n sockshop \
  --set carts.replicas=3 \
  --set catalog.replicas=3 \
  --set orders.replicas=3
```

**Custom load configuration:**
```bash
helm install sockshop ./helm/sockshop -n sockshop \
  --set loadgen.load.users=100 \
  --set loadgen.load.spawnRate=10
```

## Chart Structure

```
helm/sockshop/
├── Chart.yaml              # Chart metadata
├── values.yaml             # Default configuration values
├── templates/              # Kubernetes manifest templates
│   ├── _helpers.tpl       # Template helpers
│   ├── NOTES.txt          # Post-installation notes
│   ├── frontend-deployment.yaml
│   ├── loadgen-configmap.yaml
│   ├── loadgen-deployment.yaml
│   ├── carts.yaml
│   ├── catalog.yaml
│   ├── orders.yaml
│   ├── payment.yaml
│   ├── shipping.yaml
│   └── users.yaml
└── README.md              # Chart documentation
```

## Comparison with Kustomize Deployment

Previously, the application was deployed using Kustomize:
```bash
kubectl apply -k k8s/coherence --namespace sockshop
kubectl apply -f k8s/optional/original-front-end.yaml --namespace sockshop
kubectl apply -f loadgen/k8s-deployment.yaml --namespace sockshop
```

The Helm chart provides several advantages:
- **Single Command Deployment**: Deploy everything with one command
- **Configuration Management**: Centralized configuration via values.yaml
- **Parameterization**: Easy to customize deployments for different environments
- **Version Control**: Track releases and rollback if needed
- **Templating**: Reusable templates with Go templating
- **Package Management**: Bundle all resources together
- **Release Management**: Track what's deployed with `helm list`

## Upgrading

### Update Configuration

```bash
helm upgrade sockshop ./helm/sockshop -n sockshop \
  --set loadgen.load.users=200
```

### Rollback

```bash
helm rollback sockshop -n sockshop
```

## Uninstallation

```bash
helm uninstall sockshop -n sockshop
```

## Documentation

- [Sock Shop Chart Documentation](./sockshop/README.md)
- [Main Project Documentation](../README.md)
- [Load Generator Documentation](../loadgen/README.md)
- [Complete Application Deployment](../doc/complete-application-deployment.md)

## Support

For issues and questions:
- GitHub Issues: https://github.com/oracle/coherence-helidon-sockshop-sample/issues
- Coherence Community: https://coherence.community/

## License

Universal Permissive License (UPL), Version 1.0
