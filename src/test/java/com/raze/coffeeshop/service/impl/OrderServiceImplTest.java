package com.raze.coffeeshop.service.impl;

import com.raze.coffeeshop.dto.OrderItemRequest;
import com.raze.coffeeshop.dto.OrderRequest;
import com.raze.coffeeshop.dto.OrderResponse;
import com.raze.coffeeshop.dto.OrderStatusUpdateRequest;
import com.raze.coffeeshop.dto.PaymentRequest;
import com.raze.coffeeshop.enums.OrderStatus;
import com.raze.coffeeshop.enums.PaymentMethod;
import com.raze.coffeeshop.exception.InvalidStateException;
import com.raze.coffeeshop.exception.ResourceNotFoundException;
import com.raze.coffeeshop.model.Branch;
import com.raze.coffeeshop.model.Employee;
import com.raze.coffeeshop.model.Order;
import com.raze.coffeeshop.model.OrderItem;
import com.raze.coffeeshop.model.Payment;
import com.raze.coffeeshop.model.Product;
import com.raze.coffeeshop.model.User;
import com.raze.coffeeshop.repository.BranchRepository;
import com.raze.coffeeshop.repository.CustomerRepository;
import com.raze.coffeeshop.repository.EmployeeRepository;
import com.raze.coffeeshop.repository.OrderItemRepository;
import com.raze.coffeeshop.repository.OrderRepository;
import com.raze.coffeeshop.repository.PaymentRepository;
import com.raze.coffeeshop.repository.ProductRepository;
import com.raze.coffeeshop.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TEST DE UNIDAD (service layer) — mismo patrón que {@link ProductServiceImplTest}, pero con
 * los repositorios de Order/OrderItem/Payment más las entidades relacionadas (Branch,
 * Employee, Customer, Product) que Order referencia.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private UUID orderId;
    private UUID branchId;
    private UUID employeeId;
    private UUID productId;
    private Branch branch;
    private Employee employee;
    private Product product;
    private Order order;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "taxRate", new BigDecimal("0.16"));

        orderId = UUID.randomUUID();
        branchId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        productId = UUID.randomUUID();

        branch = new Branch();
        branch.setId(branchId);
        branch.setName("Sucursal Centro");

        User employeeUser = new User();
        employeeUser.setId(employeeId);

        employee = new Employee();
        employee.setUserId(employeeId);
        employee.setUser(employeeUser);

        product = new Product();
        product.setId(productId);
        product.setName("Latte");
        product.setBasePrice(new BigDecimal("55.00"));

        order = new Order();
        order.setId(orderId);
        order.setBranch(branch);
        order.setEmployee(employee);
        order.setStatus(OrderStatus.PENDING);
    }

    @Test
    void findById_devuelveOrden_cuandoExiste() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of());
        when(paymentRepository.findByOrderId(orderId)).thenReturn(List.of());

        OrderResponse response = orderService.findById(orderId);

        assertThat(response.branchName()).isEqualTo("Sucursal Centro");
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void findById_lanzaExcepcion_cuandoNoExiste() {
        UUID missingId = UUID.randomUUID();
        when(orderRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.findById(missingId));
    }

    @Test
    void create_guardaOrdenPendiente_cuandoBranchYEmployeeExisten() {
        OrderRequest request = new OrderRequest(branchId, employeeId, null);
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of());
        when(paymentRepository.findByOrderId(orderId)).thenReturn(List.of());

        OrderResponse response = orderService.create(request);

        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.subtotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void create_lanzaExcepcion_cuandoBranchNoExiste() {
        OrderRequest request = new OrderRequest(branchId, employeeId, null);
        when(branchRepository.findById(branchId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.create(request));
    }

    @Test
    void addItem_calculaSubtotalTaxesTotal_conElPrecioActualDelProducto() {
        OrderItemRequest request = new OrderItemRequest(productId, 2, null);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        OrderItem savedItem = new OrderItem();
        savedItem.setId(UUID.randomUUID());
        savedItem.setProduct(product);
        savedItem.setQuantity(2);
        savedItem.setUnitPrice(new BigDecimal("55.00"));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(savedItem));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(List.of());

        OrderResponse response = orderService.addItem(orderId, request);

        // subtotal = 55.00 * 2 = 110.00; taxes = 110.00 * 0.16 = 17.60; total = 127.60
        assertThat(response.subtotal()).isEqualByComparingTo("110.00");
        assertThat(response.taxes()).isEqualByComparingTo("17.60");
        assertThat(response.total()).isEqualByComparingTo("127.60");
    }

    @Test
    void addItem_congelaElPrecioDelItemYaAgregado_aunqueElProductoCambieDePrecioDespues() {
        // Item ya vendido con el precio vigente en su momento (55.00).
        OrderItem existingItem = new OrderItem();
        existingItem.setId(UUID.randomUUID());
        existingItem.setProduct(product);
        existingItem.setQuantity(1);
        existingItem.setUnitPrice(new BigDecimal("55.00"));

        // El precio del producto sube DESPUÉS de que el primer item ya fue vendido.
        product.setBasePrice(new BigDecimal("70.00"));

        OrderItemRequest secondItemRequest = new OrderItemRequest(productId, 1, null);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        List<OrderItem> itemsAfterSecondAdd = new ArrayList<>();
        itemsAfterSecondAdd.add(existingItem);
        OrderItem newItem = new OrderItem();
        newItem.setId(UUID.randomUUID());
        newItem.setProduct(product);
        newItem.setQuantity(1);
        newItem.setUnitPrice(product.getBasePrice());
        itemsAfterSecondAdd.add(newItem);
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(itemsAfterSecondAdd);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(List.of());

        OrderResponse response = orderService.addItem(orderId, secondItemRequest);

        // El item viejo sigue en 55.00 (congelado) y el nuevo toma el precio nuevo (70.00).
        assertThat(existingItem.getUnitPrice()).isEqualByComparingTo("55.00");
        assertThat(newItem.getUnitPrice()).isEqualByComparingTo("70.00");
        // subtotal = 55.00 + 70.00 = 125.00
        assertThat(response.subtotal()).isEqualByComparingTo("125.00");
    }

    @Test
    void addItem_lanzaExcepcion_cuandoOrdenNoEstaPendiente() {
        order.setStatus(OrderStatus.PAID);
        OrderItemRequest request = new OrderItemRequest(productId, 1, null);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(InvalidStateException.class, () -> orderService.addItem(orderId, request));
    }

    @Test
    void addItem_lanzaExcepcion_cuandoProductoNoExiste() {
        OrderItemRequest request = new OrderItemRequest(productId, 1, null);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.addItem(orderId, request));
    }

    @Test
    void removeItem_recalculaTotales_trasQuitarElItem() {
        UUID itemId = UUID.randomUUID();
        OrderItem item = new OrderItem();
        item.setId(itemId);
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("55.00"));

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByIdAndOrderId(itemId, orderId)).thenReturn(Optional.of(item));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of());
        when(paymentRepository.findByOrderId(orderId)).thenReturn(List.of());

        OrderResponse response = orderService.removeItem(orderId, itemId);

        assertThat(response.subtotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void removeItem_lanzaExcepcion_cuandoItemNoPerteneceALaOrden() {
        UUID itemId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByIdAndOrderId(itemId, orderId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.removeItem(orderId, itemId));
    }

    @Test
    void updateStatus_permiteCancelarUnaOrdenPendiente() {
        OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(OrderStatus.CANCELLED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of());
        when(paymentRepository.findByOrderId(orderId)).thenReturn(List.of());

        OrderResponse response = orderService.updateStatus(orderId, request);

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void updateStatus_lanzaExcepcion_cuandoLaTransicionNoEsValida() {
        order.setStatus(OrderStatus.DELIVERED);
        OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(OrderStatus.PREPARING);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(InvalidStateException.class, () -> orderService.updateStatus(orderId, request));
    }

    @Test
    void updateStatus_lanzaExcepcion_cuandoIntentanSetearPaidManualmente() {
        OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(OrderStatus.PAID);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(InvalidStateException.class, () -> orderService.updateStatus(orderId, request));
    }

    @Test
    void addPayment_marcaOrdenComoPaid_cuandoLaSumaDePagosCubreElTotal() {
        order.setSubtotal(new BigDecimal("100.00"));
        order.setTaxes(new BigDecimal("16.00"));
        order.setTotal(new BigDecimal("116.00"));

        Payment firstPayment = new Payment();
        firstPayment.setAmount(new BigDecimal("50.00"));

        PaymentRequest secondPaymentRequest = new PaymentRequest(PaymentMethod.CARD, new BigDecimal("66.00"));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(List.of(firstPayment));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of());

        OrderResponse response = orderService.addPayment(orderId, secondPaymentRequest);

        assertThat(response.status()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void addPayment_descuentaInventarioPorCadaItem_cuandoOrdenQuedaPagada() {
        order.setTotal(new BigDecimal("55.00"));

        OrderItem item = new OrderItem();
        item.setId(UUID.randomUUID());
        item.setProduct(product);
        item.setQuantity(3);
        item.setUnitPrice(new BigDecimal("55.00"));

        PaymentRequest request = new PaymentRequest(PaymentMethod.CASH, new BigDecimal("55.00"));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(List.of());
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(item));

        orderService.addPayment(orderId, request);

        verify(inventoryService).discountForSale(branchId, productId, 3, orderId, employeeId);
    }

    @Test
    void addPayment_noDescuentaInventario_cuandoElPagoNoCompletaElTotal() {
        order.setTotal(new BigDecimal("100.00"));
        PaymentRequest request = new PaymentRequest(PaymentMethod.CASH, new BigDecimal("50.00"));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(List.of());

        orderService.addPayment(orderId, request);

        verify(inventoryService, never()).discountForSale(any(), any(), anyInt(), any(), any());
    }

    @Test
    void addPayment_lanzaExcepcion_cuandoExcedeElSaldoPendiente() {
        order.setTotal(new BigDecimal("50.00"));
        PaymentRequest request = new PaymentRequest(PaymentMethod.CASH, new BigDecimal("100.00"));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(List.of());

        assertThrows(InvalidStateException.class, () -> orderService.addPayment(orderId, request));
    }

    @Test
    void addPayment_lanzaExcepcion_cuandoLaOrdenNoEstaPendiente() {
        order.setStatus(OrderStatus.PAID);
        PaymentRequest request = new PaymentRequest(PaymentMethod.CASH, new BigDecimal("10.00"));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(InvalidStateException.class, () -> orderService.addPayment(orderId, request));
    }
}
