package com.raze.demo.service.impl;

import com.raze.demo.dto.OrderItemRequest;
import com.raze.demo.dto.OrderItemResponse;
import com.raze.demo.dto.OrderRequest;
import com.raze.demo.dto.OrderResponse;
import com.raze.demo.dto.OrderStatusUpdateRequest;
import com.raze.demo.dto.PaymentRequest;
import com.raze.demo.dto.PaymentResponse;
import com.raze.demo.enums.OrderStatus;
import com.raze.demo.exception.InvalidStateException;
import com.raze.demo.exception.ResourceNotFoundException;
import com.raze.demo.model.Branch;
import com.raze.demo.model.Customer;
import com.raze.demo.model.Employee;
import com.raze.demo.model.Order;
import com.raze.demo.model.OrderItem;
import com.raze.demo.model.Payment;
import com.raze.demo.model.Product;
import com.raze.demo.repository.BranchRepository;
import com.raze.demo.repository.CustomerRepository;
import com.raze.demo.repository.EmployeeRepository;
import com.raze.demo.repository.OrderItemRepository;
import com.raze.demo.repository.OrderRepository;
import com.raze.demo.repository.PaymentRepository;
import com.raze.demo.repository.ProductRepository;
import com.raze.demo.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Servicio encargado del flujo de ventas: creación de órdenes, gestión de items,
 * cambios de estado y registro de pagos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        VALID_TRANSITIONS.put(OrderStatus.PENDING, EnumSet.of(OrderStatus.CANCELLED));
        VALID_TRANSITIONS.put(OrderStatus.PAID, EnumSet.of(OrderStatus.PREPARING, OrderStatus.CANCELLED));
        VALID_TRANSITIONS.put(OrderStatus.PREPARING, EnumSet.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED));
        VALID_TRANSITIONS.put(OrderStatus.DELIVERED, EnumSet.noneOf(OrderStatus.class));
        VALID_TRANSITIONS.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
    }

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final BranchRepository branchRepository;
    private final EmployeeRepository employeeRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    @Value("${app.tax-rate:0.16}")
    private BigDecimal taxRate;

    @Transactional(readOnly = true)
    public List<OrderResponse> findAll() {
        return orderRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(UUID id) {
        return toResponse(getOrder(id));
    }

    @Transactional
    public OrderResponse create(OrderRequest request) {
        Order order = new Order();
        order.setBranch(getBranch(request.branchId()));
        order.setEmployee(getEmployee(request.employeeId()));
        order.setCustomer(request.customerId() == null ? null : getCustomer(request.customerId()));

        order = orderRepository.save(order);
        log.info("Order created: id={}, branchId={}, employeeId={}", order.getId(), request.branchId(), request.employeeId());
        return toResponse(order);
    }

    @Transactional
    public OrderResponse addItem(UUID orderId, OrderItemRequest request) {
        Order order = getOrder(orderId);
        ensurePending(order);
        Product product = getProduct(request.productId());

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(request.quantity());
        item.setUnitPrice(product.getBasePrice());
        item.setNotes(request.notes());
        orderItemRepository.save(item);

        recalculateTotals(order);
        log.info("Item added to order {}: productId={}, quantity={}, unitPrice={}",
                orderId, product.getId(), request.quantity(), item.getUnitPrice());
        return toResponse(order);
    }

    @Transactional
    public OrderResponse removeItem(UUID orderId, UUID itemId) {
        Order order = getOrder(orderId);
        ensurePending(order);
        OrderItem item = orderItemRepository.findByIdAndOrderId(itemId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found: " + itemId));

        orderItemRepository.delete(item);
        recalculateTotals(order);
        log.info("Item {} removed from order {}", itemId, orderId);
        return toResponse(order);
    }

    @Transactional
    public OrderResponse updateStatus(UUID orderId, OrderStatusUpdateRequest request) {
        Order order = getOrder(orderId);
        OrderStatus target = request.status();

        if (target == OrderStatus.PENDING || target == OrderStatus.PAID) {
            log.warn("Rejected manual status change on order {}: target {} is not settable manually", orderId, target);
            throw new InvalidStateException("Status " + target + " cannot be set manually");
        }

        Set<OrderStatus> allowedTargets = VALID_TRANSITIONS.getOrDefault(order.getStatus(), EnumSet.noneOf(OrderStatus.class));
        if (!allowedTargets.contains(target)) {
            log.warn("Rejected invalid status transition on order {}: {} -> {}", orderId, order.getStatus(), target);
            throw new InvalidStateException("Cannot transition order from " + order.getStatus() + " to " + target);
        }

        OrderStatus previous = order.getStatus();
        order.setStatus(target);
        order = orderRepository.save(order);
        log.info("Order {} status changed: {} -> {}", orderId, previous, target);
        return toResponse(order);
    }

    @Transactional
    public OrderResponse addPayment(UUID orderId, PaymentRequest request) {
        Order order = getOrder(orderId);
        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("Rejected payment on order {}: order is not open for payment (status={})", orderId, order.getStatus());
            throw new InvalidStateException("Order is not open for payment: " + order.getStatus());
        }

        BigDecimal alreadyPaid = sumPayments(paymentRepository.findByOrderId(orderId));
        BigDecimal remaining = order.getTotal().subtract(alreadyPaid);
        if (request.amount().compareTo(remaining) > 0) {
            log.warn("Rejected payment on order {}: amount={} exceeds remaining balance={}", orderId, request.amount(), remaining);
            throw new InvalidStateException("Payment exceeds pending balance: remaining " + remaining);
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(request.method());
        payment.setAmount(request.amount());
        paymentRepository.save(payment);
        log.info("Payment registered on order {}: method={}, amount={}", orderId, request.method(), request.amount());

        if (alreadyPaid.add(request.amount()).compareTo(order.getTotal()) >= 0) {
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);
            log.info("Order {} fully paid, status changed to PAID", orderId);
        }

        return toResponse(order);
    }

    private void recalculateTotals(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());

        BigDecimal subtotal = items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxes = subtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);

        order.setSubtotal(subtotal);
        order.setTaxes(taxes);
        order.setTotal(subtotal.add(taxes));
        orderRepository.save(order);
    }

    private void ensurePending(Order order) {
        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("Rejected item change on order {}: order is not PENDING (status={})", order.getId(), order.getStatus());
            throw new InvalidStateException("Order items can only be modified while PENDING: current status " + order.getStatus());
        }
    }

    private BigDecimal sumPayments(List<Payment> payments) {
        return payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Order getOrder(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    private Branch getBranch(UUID id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + id));
    }

    private Employee getEmployee(UUID id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
    }

    private Customer getCustomer(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
    }

    private Product getProduct(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        List<Payment> payments = paymentRepository.findByOrderId(order.getId());
        BigDecimal balanceDue = order.getTotal().subtract(sumPayments(payments));
        Customer customer = order.getCustomer();

        return new OrderResponse(
                order.getId(),
                order.getBranch().getId(),
                order.getBranch().getName(),
                order.getEmployee().getUserId(),
                customer == null ? null : customer.getUserId(),
                order.getStatus(),
                order.getSubtotal(),
                order.getTaxes(),
                order.getTotal(),
                balanceDue,
                order.getCreatedAt(),
                items.stream().map(this::toItemResponse).toList(),
                payments.stream().map(this::toPaymentResponse).toList()
        );
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        Product product = item.getProduct();
        BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new OrderItemResponse(
                item.getId(),
                product.getId(),
                product.getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                lineTotal,
                item.getNotes()
        );
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getMethod(),
                payment.getAmount(),
                payment.getPaymentDate()
        );
    }
}
