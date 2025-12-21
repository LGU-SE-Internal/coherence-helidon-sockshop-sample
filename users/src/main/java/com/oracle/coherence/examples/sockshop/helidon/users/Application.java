/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.examples.sockshop.helidon.users;

import io.helidon.microprofile.server.Server;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * Entry point into the application
 * 
 * Note: OpenTelemetry tracing and log correlation is handled by the Java agent,
 * injected via Kubernetes annotation: instrumentation.opentelemetry.io/inject-java
 */
public class Application {
public static void main(String... args) {
// Route JUL logs to SLF4J (for libraries that use java.util.logging)
SLF4JBridgeHandler.removeHandlersForRootLogger();
SLF4JBridgeHandler.install();

// Start the server - OpenTelemetry Java agent handles all instrumentation
Server.create().start();
}
}
