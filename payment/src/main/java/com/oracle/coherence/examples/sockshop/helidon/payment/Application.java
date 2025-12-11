/*
 * Copyright (c) 2022, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.examples.sockshop.helidon.payment;

import io.helidon.microprofile.server.Server;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
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
		
		// Initialize OpenTelemetry SDK (traces + logs) BEFORE server starts
		initializeOpenTelemetry();
		
		// Start the server
		Server.create().start();
	}
	
	private static void initializeOpenTelemetry() {
		// Get the OTLP endpoint from system properties or environment
		String otlpEndpoint = System.getProperty("otel.exporter.otlp.endpoint",
				System.getenv().getOrDefault("OTEL_EXPORTER_OTLP_ENDPOINT",
						"http://opentelemetry-kube-stack-deployment-collector.monitoring:4317"));
		
		String serviceName = System.getProperty("otel.service.name",
				System.getenv().getOrDefault("OTEL_SERVICE_NAME", "Payment"));
		
		// Create shared resource
		Resource resource = Resource.getDefault().toBuilder()
				.put(SERVICE_NAME, serviceName)
				.build();
		
		// Create trace exporter and provider
		OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
				.setEndpoint(otlpEndpoint)
				.build();
		
		SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
				.setResource(resource)
				.addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
				.build();
		
		// Create log exporter and provider
		OtlpGrpcLogRecordExporter logExporter = OtlpGrpcLogRecordExporter.builder()
				.setEndpoint(otlpEndpoint)
				.build();
		
		SdkLoggerProvider sdkLoggerProvider = SdkLoggerProvider.builder()
				.setResource(resource)
				.addLogRecordProcessor(BatchLogRecordProcessor.builder(logExporter).build())
				.build();
		
		// Create OpenTelemetry SDK instance with both traces and logs
		OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder()
				.setTracerProvider(sdkTracerProvider)
				.setLoggerProvider(sdkLoggerProvider)
				.build();
		
		// Register as global OpenTelemetry instance
		GlobalOpenTelemetry.set(openTelemetrySdk);
		
		// Add shutdown hook to flush traces and logs before application exits
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			sdkTracerProvider.close();
			sdkLoggerProvider.close();
		}));
		
		// Install the SDK in the Logback appender for log correlation
		io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender.install(openTelemetrySdk);
	}
}
