/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.examples.sockshop.helidon.shipping;

import java.time.LocalDate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;

import io.helidon.grpc.api.Grpc;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.microprofile.metrics.annotation.Timed;

/**
 * Implementation of the Shipping Service REST and gRPC API.
 */
@ApplicationScoped
@Path("/shipping")
@Grpc.GrpcService("ShippingGrpc")
@Grpc.GrpcMarshaller("jsonb")
@Grpc.GrpcInterceptors(MetadataLogger.class)
@Timed
@Slf4j
public class ShippingResource implements ShippingApi {

    /**
     * Shipment repository to use.
     */
    @Inject
    private ShipmentRepository shipments;

    @Override
    @Grpc.Unary
    public Shipment getShipmentByOrderId(String orderId) {
        log.info("Getting shipment for order: " + orderId);
        Shipment shipment = shipments.getShipment(orderId);
        if (shipment == null) {
            log.warn("Shipment not found for order: " + orderId);
        }
        return shipment;
    }

    @Override
    @Grpc.Unary
    public Shipment ship(ShippingRequest req) {
        String tp = req.getTraceParent();
        log.info(">>>> [RELAY RECEIVE] Received Trace: {}", tp);

        io.helidon.tracing.Tracer tracer = io.helidon.tracing.Tracer.global();
        io.helidon.tracing.Span.Builder<?> spanBuilder = tracer.spanBuilder("ShippingGrpc/ship")
                .kind(io.helidon.tracing.Span.Kind.SERVER);

        // Use HeaderProvider to extract parent context from traceParent field
        if (tp != null && !tp.isEmpty()) {
            io.helidon.tracing.HeaderProvider hp = new io.helidon.tracing.HeaderProvider() {
                @Override 
                public java.util.Optional<String> get(String key) { 
                    return "traceparent".equals(key) ? java.util.Optional.of(tp) : java.util.Optional.empty(); 
                }
                
                @Override 
                public Iterable<String> keys() { 
                    return java.util.List.of("traceparent"); 
                }
                
                @Override
                public Iterable<String> getAll(String key) {
                    if ("traceparent".equals(key)) {
                        return java.util.List.of(tp);
                    }
                    return java.util.List.of();
                }
                
                @Override 
                public boolean contains(String key) { 
                    return "traceparent".equals(key); 
                }
            };
            
            // Force parent context linkage
            tracer.extract(hp).ifPresent(spanBuilder::parent);
        }

        io.helidon.tracing.Span serverSpan = spanBuilder.start();
        try (io.helidon.tracing.Scope scope = serverSpan.activate()) {
            // Execute business logic within traced context
            String orderId = req.getOrderId();
            int itemCount = req.getItemCount();
            log.info("Creating shipment for order: " + orderId + ", items: " + itemCount);

            // defaults
            String carrier = "USPS";
            String trackingNumber = "9205 5000 0000 0000 0000 00";
            LocalDate deliveryDate = LocalDate.now().plusDays(5);

            if (itemCount == 1) {  // use FedEx
                carrier = "FEDEX";
                trackingNumber = "231300687629630";
                deliveryDate = LocalDate.now().plusDays(1);
            }
            else if (itemCount <= 3) {  // use UPS
                carrier = "UPS";
                trackingNumber = "1Z999AA10123456784";
                deliveryDate = LocalDate.now().plusDays(3);
            }

            Shipment shipment = Shipment.builder()
                    .orderId(orderId)
                    .carrier(carrier)
                    .trackingNumber(trackingNumber)
                    .deliveryDate(deliveryDate)
                    .build();

            shipments.saveShipment(shipment);

            log.info("Shipment created for order: " + orderId + ", carrier: " + carrier + ", tracking: " + trackingNumber);

            serverSpan.status(io.helidon.tracing.Span.Status.OK);
            return shipment;
        } catch (Exception e) {
            log.error("Error creating shipment", e);
            serverSpan.status(io.helidon.tracing.Span.Status.ERROR);
            throw e;
        } finally {
            serverSpan.end();
        }
    }
}
