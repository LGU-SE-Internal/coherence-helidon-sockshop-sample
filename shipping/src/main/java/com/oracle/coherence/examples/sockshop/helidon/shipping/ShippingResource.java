/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.examples.sockshop.helidon.shipping;

import java.time.LocalDate;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;

import io.helidon.grpc.api.Grpc;
import io.opentelemetry.instrumentation.annotations.WithSpan;

import org.eclipse.microprofile.metrics.annotation.Timed;

/**
 * Implementation of the Shipping Service REST and gRPC API.
 */
@ApplicationScoped
@Path("/shipping")
@Grpc.GrpcService("ShippingGrpc")
@Grpc.GrpcMarshaller("jsonb")
@Timed
public class ShippingResource implements ShippingApi {
    private static final Logger LOGGER = Logger.getLogger(ShippingResource.class.getName());

    /**
     * Shipment repository to use.
     */
    @Inject
    private ShipmentRepository shipments;

    @Override
    @Grpc.Unary
    @WithSpan
    public Shipment getShipmentByOrderId(String orderId) {
        LOGGER.info("Getting shipment for order: " + orderId);
        Shipment shipment = shipments.getShipment(orderId);
        if (shipment == null) {
            LOGGER.warning("Shipment not found for order: " + orderId);
        }
        return shipment;
    }

    @Override
    @Grpc.Unary
    @WithSpan
    public Shipment ship(ShippingRequest req) {
        String orderId = req.getOrderId();
        int itemCount = req.getItemCount();
        LOGGER.info("Creating shipment for order: " + orderId + ", items: " + itemCount);

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

        LOGGER.info("Shipment created for order: " + orderId + ", carrier: " + carrier + ", tracking: " + trackingNumber);

        return shipment;
    }
}
