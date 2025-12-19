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
		
		// Initialize OpenTelemetry logs BEFORE server starts
		// This must happen before Logback creates appenders from logback.xml
		initializeOpenTelemetryLogs();
		
		// Start the server - Helidon will initialize GlobalOpenTelemetry with traces
		// The span context will be available via Context API when logs are emitted
		Server server = Server.create();
		server.start();
		
		// After server starts, if agent is present, install its GlobalOpenTelemetry in logback appender
		// This ensures the agent's OpenTelemetry instance is fully initialized before we use it
		if (isAgentPresent()) {
			installAgentOpenTelemetryInLogback();
		}
	}
	
	/**
	 * Install the agent's GlobalOpenTelemetry instance in the logback appender after server startup.
	 * This is necessary because the agent initializes GlobalOpenTelemetry during server startup,
	 * and the appender needs a fully initialized OpenTelemetry instance.
	 */
	private static void installAgentOpenTelemetryInLogback() {
		try {
			// Give the agent a moment to fully initialize
			Thread.sleep(1000);
			
			// Get the GlobalOpenTelemetry instance from the agent
			OpenTelemetry globalOtel = io.opentelemetry.api.GlobalOpenTelemetry.get();
			
			// Install it in the logback appender so logs are correlated with traces
			io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender.install(globalOtel);
			
			System.out.println("Successfully installed agent's GlobalOpenTelemetry in logback appender");
		} catch (Exception e) {
			System.err.println("Failed to install agent's GlobalOpenTelemetry in logback appender: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	private static void initializeOpenTelemetryLogs() {
		// Check if the OpenTelemetry Java agent is present
		// When agent is present, it manages the GlobalOpenTelemetry instance
		// We should use that instead of creating our own SDK
		boolean isAgentPresent = isAgentPresent();
		
		if (isAgentPresent) {
			// Agent is present - we will install its GlobalOpenTelemetry AFTER server starts
			// to ensure the agent has fully initialized
			System.out.println("OpenTelemetry Java agent detected - will install GlobalOpenTelemetry after server starts");
			return;
		}
		
		// No agent present - initialize our own OpenTelemetry SDK for logs
		System.out.println("No OpenTelemetry agent detected - initializing manual SDK for logs");
		
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
	
	/**
	 * Check if the OpenTelemetry Java agent is present by looking for the agent's classes.
	 * 
	 * This uses the well-known AgentClassLoader class which is a stable part of the
	 * OpenTelemetry Java agent's public contract. This class exists in all agent versions
	 * and is specifically designed to be detectable by applications.
	 * 
	 * @return true if the agent is present, false otherwise
	 * @see <a href="https://github.com/open-telemetry/opentelemetry-java-instrumentation">OpenTelemetry Java Instrumentation</a>
	 */
	private static boolean isAgentPresent() {
		try {
			// Try to load a class that only exists when the agent is attached
			Class.forName("io.opentelemetry.javaagent.bootstrap.AgentClassLoader");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}
}
