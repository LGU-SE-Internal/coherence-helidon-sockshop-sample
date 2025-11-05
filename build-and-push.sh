#!/bin/bash

set -e

REGISTRY="10.10.10.240"
REPO="library"

echo "=== Step 1: Building images locally with Jib ==="
mvn clean package -Pcontainer -DskipTests \
  -Djib.goal=dockerBuild \
  -Ddocker.registry.url=${REGISTRY} \
  -Ddocker.registry.name=${REPO}

echo ""
echo "=== Step 2: Pushing images to ${REGISTRY}/${REPO} ==="
for img in $(docker images --format "{{.Repository}}:{{.Tag}}" | grep "${REGISTRY}/${REPO}/ss-"| grep 2.4.0); do
  echo "Pushing $img..."
  docker push $img
done

echo ""
echo "=== Done! All images built and pushed successfully ==="
docker images | grep "${REGISTRY}/${REPO}/ss-"