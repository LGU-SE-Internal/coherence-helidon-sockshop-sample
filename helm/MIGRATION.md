# Migration Guide: From Kustomize to Helm

This guide helps you migrate from the existing Kustomize-based deployment to the new Helm chart.

## Why Migrate to Helm?

### Advantages of Helm over Kustomize

1. **Single Command Deployment**: Deploy everything (backend, frontend, loadgen) with one command
2. **Parameterization**: Easy configuration via values.yaml without modifying manifests
3. **Release Management**: Track deployments, rollback easily, and manage versions
4. **Templating**: Powerful Go templating for complex scenarios
5. **Package Management**: Bundle and distribute as a single package
6. **Environment Management**: Easily maintain separate configurations for dev, test, prod

### When to Keep Using Kustomize

- You prefer a simpler, declarative approach
- You don't need complex parameterization
- You want to avoid learning Helm syntax
- Your deployment patterns are very straightforward

## Migration Steps

### Current Kustomize Deployment

Your current deployment likely looks like this:

```bash
# Backend services
kubectl apply -k k8s/coherence --namespace sockshop

# Frontend (optional)
kubectl apply -f k8s/optional/original-front-end.yaml --namespace sockshop

# Load generator (optional)
kubectl apply -f loadgen/k8s-deployment.yaml --namespace sockshop
```

### New Helm Deployment

Replace all of the above with:

```bash
helm install sockshop ./helm/sockshop -n sockshop
```

## Detailed Migration Process

### Step 1: Backup Current Deployment

Before migrating, save your current configuration:

```bash
# Export current resources
kubectl get all -n sockshop -o yaml > backup-sockshop.yaml

# Export specific resources
kubectl get coherence -n sockshop -o yaml > backup-coherence.yaml
kubectl get deployment -n sockshop -o yaml > backup-deployments.yaml
kubectl get service -n sockshop -o yaml > backup-services.yaml
kubectl get configmap -n sockshop -o yaml > backup-configmaps.yaml
```

### Step 2: Note Custom Configurations

If you've customized any deployments, note the changes:

```bash
# Check replica counts
kubectl get coherence -n sockshop
kubectl get deployment -n sockshop

# Check custom environment variables
kubectl get deployment -n sockshop -o yaml | grep -A 10 "env:"

# Check custom image tags
kubectl get coherence -n sockshop -o yaml | grep "image:"
kubectl get deployment -n sockshop -o yaml | grep "image:"
```

### Step 3: Create Custom Values File

Based on your current configuration, create a `my-values.yaml` file:

```yaml
# Example: If you've scaled services
carts:
  replicas: 3
catalog:
  replicas: 3
orders:
  replicas: 2

# Example: If you're using a custom registry
global:
  imageRegistry: "my-registry.example.com"

# Example: If you've customized images
carts:
  image:
    tag: "2.4.0"

# Example: If you've customized load generator
loadgen:
  load:
    users: 100
    spawnRate: 10
```

### Step 4: Remove Old Deployment

**Important**: This will cause downtime. Plan accordingly.

```bash
# Remove load generator (if deployed)
kubectl delete -f loadgen/k8s-deployment.yaml --namespace sockshop

# Remove frontend (if deployed)
kubectl delete -f k8s/optional/original-front-end.yaml --namespace sockshop

# Remove backend services
kubectl delete -k k8s/coherence --namespace sockshop

# Verify everything is deleted
kubectl get all -n sockshop
```

**Alternative**: Zero-downtime migration (see Advanced section below)

### Step 5: Deploy with Helm

```bash
# Install with default values
helm install sockshop ./helm/sockshop -n sockshop

# Or install with custom values
helm install sockshop ./helm/sockshop -n sockshop -f my-values.yaml
```

### Step 6: Verify New Deployment

```bash
# Check Helm release
helm list -n sockshop
helm status sockshop -n sockshop

# Check resources
kubectl get coherence -n sockshop
kubectl get deployment -n sockshop
kubectl get pods -n sockshop

# Check services are accessible
kubectl port-forward -n sockshop service/front-end 8079:80
# Open http://localhost:8079
```

## Configuration Mapping

### Kustomize vs Helm Configuration

| Kustomize Approach | Helm Approach |
|-------------------|---------------|
| Edit `k8s/coherence/carts/app.yaml` | Set `carts.replicas=3` in values.yaml |
| Create overlay patches | Use `--set` flags or custom values.yaml |
| Multiple `kubectl apply` commands | Single `helm install` command |
| Manual image updates | Set `global.imageRegistry` or `<service>.image.tag` |
| Manual ConfigMap edits | Configure via `loadgen.config` in values.yaml |

### Example: Scaling Services

**Kustomize:**
```bash
kubectl scale coherence/carts -n sockshop --replicas=3
```

**Helm:**
```bash
helm upgrade sockshop ./helm/sockshop -n sockshop --set carts.replicas=3
```

### Example: Changing Image Registry

**Kustomize:**
Edit each service's YAML file and change the image field.

**Helm:**
```bash
helm upgrade sockshop ./helm/sockshop -n sockshop \
  --set global.imageRegistry=new-registry.com
```

### Example: Customizing Load Generator

**Kustomize:**
Edit `loadgen/k8s-deployment.yaml` and modify args/env.

**Helm:**
```bash
helm upgrade sockshop ./helm/sockshop -n sockshop \
  --set loadgen.load.users=100 \
  --set loadgen.load.spawnRate=10
```

## Advanced: Zero-Downtime Migration

For production environments, you can perform a zero-downtime migration:

### Option 1: Blue-Green Deployment

1. Deploy Helm chart to a new namespace:
```bash
kubectl create namespace sockshop-v2
helm install sockshop ./helm/sockshop -n sockshop-v2
```

2. Test the new deployment:
```bash
kubectl port-forward -n sockshop-v2 service/front-end 8080:80
```

3. Switch traffic (update ingress/load balancer to point to sockshop-v2)

4. Remove old deployment:
```bash
kubectl delete namespace sockshop
```

5. Rename namespace (optional):
```bash
# This requires manual resource recreation
```

### Option 2: Service-by-Service Migration

Migrate one service at a time:

1. Disable a service in Helm, deploy:
```bash
helm install sockshop ./helm/sockshop -n sockshop \
  --set carts.enabled=false
```

2. Remove the old cart service:
```bash
kubectl delete coherence/carts -n sockshop
```

3. Enable the service in Helm:
```bash
helm upgrade sockshop ./helm/sockshop -n sockshop \
  --set carts.enabled=true
```

4. Repeat for each service

## Rollback Plan

If the migration fails, you can rollback:

### Rollback to Kustomize Deployment

```bash
# Remove Helm deployment
helm uninstall sockshop -n sockshop

# Restore from Kustomize
kubectl apply -k k8s/coherence --namespace sockshop
kubectl apply -f k8s/optional/original-front-end.yaml --namespace sockshop
kubectl apply -f loadgen/k8s-deployment.yaml --namespace sockshop
```

### Rollback from Backup

```bash
# Remove Helm deployment
helm uninstall sockshop -n sockshop

# Restore from backup
kubectl apply -f backup-sockshop.yaml -n sockshop
```

## Common Migration Issues

### Issue 1: Resource Already Exists

**Problem:** Helm tries to create a resource that already exists.

**Solution:** Remove the old resources first:
```bash
kubectl delete -k k8s/coherence --namespace sockshop
kubectl delete -f k8s/optional/original-front-end.yaml --namespace sockshop
kubectl delete -f loadgen/k8s-deployment.yaml --namespace sockshop
```

### Issue 2: Different Resource Names

**Problem:** Helm-deployed resources have different names than Kustomize-deployed ones.

**Solution:** This is expected. Services will use the same names (e.g., `front-end`, `carts`), so applications should continue to work.

### Issue 3: Configuration Not Preserved

**Problem:** Custom configurations from Kustomize are lost.

**Solution:** Create a custom values.yaml file before migration (see Step 3 above).

### Issue 4: Coherence Operator CRD Issues

**Problem:** CRD conflicts or version mismatches.

**Solution:** Ensure Coherence Operator is up-to-date:
```bash
kubectl apply -f https://github.com/oracle/coherence-operator/releases/download/v3.3.4/coherence-operator.yaml
```

## Post-Migration Tasks

After successful migration:

1. **Update Documentation**: Update your team's documentation to use Helm commands
2. **Update CI/CD**: Modify deployment pipelines to use Helm
3. **Create Values Files**: Create environment-specific values files (dev, test, prod)
4. **Test Scenarios**: Verify all your use cases work with Helm
5. **Training**: Ensure team members understand Helm basics

## CI/CD Pipeline Updates

### Before (Kustomize)

```yaml
deploy:
  script:
    - kubectl apply -k k8s/coherence --namespace sockshop
    - kubectl apply -f k8s/optional/original-front-end.yaml --namespace sockshop
    - kubectl apply -f loadgen/k8s-deployment.yaml --namespace sockshop
```

### After (Helm)

```yaml
deploy:
  script:
    - helm upgrade --install sockshop ./helm/sockshop -n sockshop -f values-${ENV}.yaml
```

## FAQ

**Q: Can I use both Kustomize and Helm?**

A: Not recommended. Choose one approach to avoid conflicts and confusion.

**Q: Will service names change?**

A: No. Service names (like `front-end`, `carts`, `catalogue`) remain the same.

**Q: Do I need to change application code?**

A: No. The Helm chart maintains API compatibility.

**Q: What about custom Kustomize overlays?**

A: Convert them to custom values.yaml files or use Helm's templating.

**Q: Can I preview changes before applying?**

A: Yes, use `helm upgrade --dry-run --debug`

**Q: How do I manage multiple environments?**

A: Create separate values files: `values-dev.yaml`, `values-test.yaml`, `values-prod.yaml`

## Getting Help

If you encounter issues during migration:

1. Check the [Helm documentation](./sockshop/README.md)
2. Review [Quick Start Guide](./QUICKSTART.md)
3. Look at [example values](./sockshop/values-examples.yaml)
4. Open an issue on GitHub

## Summary Checklist

- [ ] Backup current deployment
- [ ] Note custom configurations
- [ ] Create custom values.yaml file
- [ ] Test Helm deployment in dev/test environment
- [ ] Plan downtime window (or use zero-downtime approach)
- [ ] Remove old Kustomize deployment
- [ ] Deploy with Helm
- [ ] Verify all services are working
- [ ] Update documentation and CI/CD pipelines
- [ ] Monitor for issues

## Additional Resources

- [Helm Chart Documentation](./sockshop/README.md)
- [Quick Start Guide](./QUICKSTART.md)
- [Example Configurations](./sockshop/values-examples.yaml)
- [Helm Documentation](https://helm.sh/docs/)
- [Coherence Operator Documentation](https://oracle.github.io/coherence-operator/)
