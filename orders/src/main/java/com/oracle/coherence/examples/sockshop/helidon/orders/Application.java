/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.examples.sockshop.helidon.orders;

import io.helidon.microprofile.server.Server;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * Entry point into the application
 */
public class Application {
	public static void main(String... args) {
		// Initialize OpenTelemetry before starting the server
		initializeOpenTelemetry();
		
		// Route JUL logs to SLF4J
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
		
		Server.create().start();
	}
	
	private static void initializeOpenTelemetry() {
		// Get the global OpenTelemetry instance configured by Helidon
		OpenTelemetry openTelemetry = GlobalOpenTelemetry.get();
		
		// Install OpenTelemetry in the Logback appender
		io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender.install(openTelemetry);
	}
}
