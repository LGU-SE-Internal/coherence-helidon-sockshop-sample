FROM python:3.13-slim

# Set working directory
WORKDIR /loadgen

# Install dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy load test files
COPY locustfile.py .
COPY config.yaml .

# Expose Locust web UI port
EXPOSE 8089

# Default command - can be overridden
# Usage: docker run loadgen -H http://target-host [other locust options]
ENTRYPOINT ["locust"]
CMD ["-f", "locustfile.py"]
