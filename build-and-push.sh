#!/bin/bash

set -e

REGISTRY="10.10.10.240"
REPO="library"

# Extract version from root pom.xml dynamically
VERSION=$(mvn -B -q help:evaluate -Dexpression=docker.image.tag -DforceStdout 2>/dev/null | sed 's/\x1b\[[0-9;]*m//g' | tr -d '[:space:]')
: ${VERSION:="2.11.0"} 

echo "=== Building version: ${VERSION} ==="
echo "=== Step 1: Building images locally with Jib ==="
mvn clean package -Pcontainer -DskipTests \
  -Djib.goal=dockerBuild \
  -Ddocker.registry.url=${REGISTRY} \
  -Ddocker.registry.name=${REPO}

echo ""
echo "=== Step 2: Pushing images to ${REGISTRY}/${REPO} ==="
for img in $(docker images --format "{{.Repository}}:{{.Tag}}" | grep "${REGISTRY}/${REPO}/ss-" | grep -F "$VERSION"); do
  echo "Pushing $img..."
  docker push "$img"
done

echo ""
echo "=== Done! All images built and pushed successfully ==="