/*
 * Copyright (c) 2022, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.examples.sockshop.helidon.shipping;

import io.helidon.microprofile.server.Server;
import io.opentelemetry.api.GlobalOpenTelemetry;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * Entry point into the application
 */
public class Application {
	public static void main(String... args) {
		// Route JUL logs to SLF4J first, before initializing OpenTelemetry
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
		
		// Start the server - Helidon will initialize GlobalOpenTelemetry with tracing
		Server server = Server.create().start();
		
		// Install the OpenTelemetry appender AFTER Helidon has initialized GlobalOpenTelemetry
		// This ensures logs will have access to the trace context from Helidon's tracer
		io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender.install(GlobalOpenTelemetry.get());
	}
}
