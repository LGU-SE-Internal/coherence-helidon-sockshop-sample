# coherence-operator Helm Chart Installation
```
helm repo add coherence https://oracle.github.io/coherence-operator/charts
helm repo update
helm install --namespace coherence-test  operator coherence/coherence-operator --create-namespace
```

# build and push Docker images

```
# env setup
sdk env install
# build images
./build-and-push.sh
```
