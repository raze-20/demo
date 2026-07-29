package com.raze.demo.controller;

import com.raze.demo.dto.OrderItemRequest;
import com.raze.demo.dto.OrderRequest;
import com.raze.demo.dto.OrderResponse;
import com.raze.demo.dto.OrderStatusUpdateRequest;
import com.raze.demo.dto.PaymentRequest;
import com.raze.demo.enums.OrderStatus;
import com.raze.demo.service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * Controlador REST para el flujo de ventas: órdenes, items y pagos.
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','CASHIER','BARISTA')")
    @GetMapping
    public Page<OrderResponse> findAll(
            @RequestParam(required = false) OrderStatus status,
            Pageable pageable
    ) {
        return orderService.findAll(status, pageable);
    }

    @PostAuthorize("hasAnyRole('ADMIN','MANAGER','CASHIER','BARISTA') or returnObject.customerId() == authentication.principal.id")
    @GetMapping("/{id}")
    public OrderResponse findById(@PathVariable UUID id) {
        return orderService.findById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','CASHIER','BARISTA')")
    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request) {
        OrderResponse response = orderService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/orders/" + response.id())).body(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','CASHIER','BARISTA')")
    @PostMapping("/{orderId}/items")
    public ResponseEntity<OrderResponse> addItem(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderItemRequest request
    ) {
        OrderResponse response = orderService.addItem(orderId, request);
        return ResponseEntity.created(URI.create("/api/v1/orders/" + orderId)).body(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','CASHIER','BARISTA')")
    @DeleteMapping("/{orderId}/items/{itemId}")
    public ResponseEntity<Void> removeItem(@PathVariable UUID orderId, @PathVariable UUID itemId) {
        orderService.removeItem(orderId, itemId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','CASHIER','BARISTA')")
    @PatchMapping("/{orderId}/status")
    public OrderResponse updateStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderStatusUpdateRequest request
    ) {
        return orderService.updateStatus(orderId, request);
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','CASHIER','BARISTA')")
    @PostMapping("/{orderId}/payments")
    public ResponseEntity<OrderResponse> addPayment(
            @PathVariable UUID orderId,
            @Valid @RequestBody PaymentRequest request
    ) {
        OrderResponse response = orderService.addPayment(orderId, request);
        return ResponseEntity.created(URI.create("/api/v1/orders/" + orderId)).body(response);
    }
}
