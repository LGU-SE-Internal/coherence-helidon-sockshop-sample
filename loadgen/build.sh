#!/bin/bash
#
# Build Load Generator Docker Image
#
# Usage: ./build.sh [tag]
#   tag: Docker image tag (default: latest)
#

set -e

TAG=${1:-latest}
IMAGE_NAME="loadgen"

echo "=========================================="
echo "Building Load Generator Docker Image"
echo "=========================================="
echo "Image: $IMAGE_NAME:$TAG"
echo "=========================================="
echo ""

# Build the image
docker build -t "$IMAGE_NAME:$TAG" .

echo ""
echo "Build completed successfully!"
echo "Image: $IMAGE_NAME:$TAG"
echo ""
echo "To run the load generator:"
echo "  docker run -p 8089:8089 $IMAGE_NAME:$TAG -f locustfile.py --host=http://your-target"
echo ""
echo "To push to a registry:"
echo "  docker tag $IMAGE_NAME:$TAG your-registry/$IMAGE_NAME:$TAG"
echo "  docker push your-registry/$IMAGE_NAME:$TAG"
