package com.raze.demo.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.raze.demo.dto.OrderItemRequest;
import com.raze.demo.dto.OrderRequest;
import com.raze.demo.dto.OrderResponse;
import com.raze.demo.dto.OrderStatusUpdateRequest;
import com.raze.demo.dto.PaymentRequest;
import com.raze.demo.enums.OrderStatus;

public interface OrderService {

    public Page<OrderResponse> findAll(OrderStatus status, Pageable pageable);

    public OrderResponse findById(UUID id);

    public OrderResponse create(OrderRequest request);

    public OrderResponse addItem(UUID orderId, OrderItemRequest request);

    public OrderResponse removeItem(UUID orderId, UUID itemId);

    public OrderResponse updateStatus(UUID orderId, OrderStatusUpdateRequest request);

    public OrderResponse addPayment(UUID orderId, PaymentRequest request);

}
