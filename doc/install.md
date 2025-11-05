# coherence-operator Helm Chart Installation
```bash
helm repo add coherence https://oracle.github.io/coherence-operator/charts
helm repo update
helm install --namespace coherence-test  operator coherence/coherence-operator --create-namespace
```

# build and push Docker images

```bash
# env setup
sdk env install
# build images
./build-and-push.sh
```


# deploy

```bash
helm install sockshop helm/sockshop --namespace sockshop
```