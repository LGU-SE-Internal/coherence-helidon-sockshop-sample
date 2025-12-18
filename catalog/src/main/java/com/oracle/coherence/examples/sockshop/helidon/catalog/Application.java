/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.examples.sockshop.helidon.catalog;

import io.helidon.microprofile.server.Server;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.resources.Resource;
import org.slf4j.bridge.SLF4JBridgeHandler;

import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;

/**
 * Entry point into the application
 */
public class Application {
	public static void main(String... args) {
		// Route JUL logs to SLF4J first, before initializing OpenTelemetry
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
		
		// Check if OTel Java agent is present
		boolean agentPresent = Boolean.parseBoolean(
				System.getProperty("otel.agent.present",
						System.getenv().getOrDefault("OTEL_AGENT_PRESENT", "false")));
		
		// Only initialize manual OpenTelemetry logs if agent is NOT present
		// When the agent is present, it handles all OpenTelemetry setup
		if (!agentPresent) {
			// Initialize OpenTelemetry logs BEFORE server starts
			// This must happen before Logback creates appenders from logback.xml
			initializeOpenTelemetryLogs();
		}
		
		// Start the server - Helidon will initialize GlobalOpenTelemetry with traces
		// The span context will be available via Context API when logs are emitted
		Server.create().start();
	}
	
	private static void initializeOpenTelemetryLogs() {
		// Get the OTLP endpoint from system properties or environment, with fallback to config
		String otlpEndpoint = System.getProperty("otel.exporter.otlp.logs.endpoint",
				System.getProperty("otel.exporter.otlp.endpoint",
						System.getenv().getOrDefault("OTEL_EXPORTER_OTLP_LOGS_ENDPOINT",
								System.getenv().getOrDefault("OTEL_EXPORTER_OTLP_ENDPOINT",
										"http://opentelemetry-kube-stack-deployment-collector.monitoring:4317"))));
		
		String serviceName = System.getProperty("otel.service.name",
				System.getenv().getOrDefault("OTEL_SERVICE_NAME", "catalog"));
		
		// Create a log exporter that sends logs to the OTLP collector
		OtlpGrpcLogRecordExporter logExporter = OtlpGrpcLogRecordExporter.builder()
				.setEndpoint(otlpEndpoint)
				.build();
		
		// Create the SdkLoggerProvider with the exporter
		SdkLoggerProvider sdkLoggerProvider = SdkLoggerProvider.builder()
				.setResource(Resource.getDefault().toBuilder()
						.put(SERVICE_NAME, serviceName)
						.build())
				.addLogRecordProcessor(BatchLogRecordProcessor.builder(logExporter).build())
				.build();
		
		// Create an OpenTelemetry SDK instance with logs support
		//NOTE: The span context comes from the global Context API (set by Helidon's tracer)
		// The appender will use this SDK for log export, but get span context from Context.current()
		OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder()
				.setLoggerProvider(sdkLoggerProvider)
				.build();
		
		// Add shutdown hook to flush logs before application exits
		Runtime.getRuntime().addShutdownHook(new Thread(openTelemetrySdk::close));
		
		// Install the SDK in the Logback appender
		// The appender uses Context.current() to get span context (from Helidon's tracer)
		// and uses this SDK's LoggerProvider to emit logs with OTLP export
		io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender.install(openTelemetrySdk);
	}
}
