package com.raze.demo.controller;

import com.raze.demo.dto.OrderItemRequest;
import com.raze.demo.dto.OrderRequest;
import com.raze.demo.dto.OrderResponse;
import com.raze.demo.dto.OrderStatusUpdateRequest;
import com.raze.demo.dto.PaymentRequest;
import com.raze.demo.enums.OrderStatus;
import com.raze.demo.enums.PaymentMethod;
import com.raze.demo.exception.InvalidStateException;
import com.raze.demo.exception.ResourceNotFoundException;
import com.raze.demo.model.Order;
import com.raze.demo.security.JwtService;
import com.raze.demo.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TEST DE "SLICE" (capa web) — mismo patrón que {@link ProductControllerTest}.
 */
@WebMvcTest(OrderController.class)
@WithMockUser(roles = "ADMIN")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private JwtService jwtService;

    private OrderResponse sampleResponse(UUID id, OrderStatus status) {
        return new OrderResponse(
                id,
                UUID.randomUUID(),
                "Sucursal Centro",
                UUID.randomUUID(),
                null,
                status,
                new BigDecimal("100.00"),
                new BigDecimal("16.00"),
                new BigDecimal("116.00"),
                new BigDecimal("116.00"),
                OffsetDateTime.now(),
                List.of(),
                List.of()
        );
    }

    @Test
    void getById_retorna200_conOrden() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.findById(id)).thenReturn(sampleResponse(id, OrderStatus.PENDING));

        mockMvc.perform(get("/api/v1/orders/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getById_retorna404_cuandoNoExiste() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.findById(id)).thenThrow(new ResourceNotFoundException("Order not found: " + id));

        mockMvc.perform(get("/api/v1/orders/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_retorna201_conCuerpoValido() throws Exception {
        UUID id = UUID.randomUUID();
        OrderRequest request = new OrderRequest(UUID.randomUUID(), UUID.randomUUID(), null);
        when(orderService.create(any(OrderRequest.class))).thenReturn(sampleResponse(id, OrderStatus.PENDING));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    // Bean Validation (@NotNull en branchId/employeeId) debe rechazar la petición antes de
    // tocar el service.
    void create_retorna400_cuandoFaltanCamposObligatorios() throws Exception {
        String invalidJson = """
                {}
                """;

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addItem_retorna201_conOrdenActualizada() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderItemRequest request = new OrderItemRequest(UUID.randomUUID(), 2, "sin azucar");
        when(orderService.addItem(eq(orderId), any(OrderItemRequest.class)))
                .thenReturn(sampleResponse(orderId, OrderStatus.PENDING));

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(100.00));
    }

    @Test
    void addItem_retorna400_cuandoOrdenNoEstaPendiente() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderItemRequest request = new OrderItemRequest(UUID.randomUUID(), 1, null);
        when(orderService.addItem(eq(orderId), any(OrderItemRequest.class)))
                .thenThrow(new InvalidStateException("Order items can only be modified while PENDING"));

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    // Bean Validation (@Max(500) en quantity) debe rechazar la petición antes de tocar el service.
    void addItem_retorna400_cuandoCantidadExcedeElMaximo() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderItemRequest request = new OrderItemRequest(UUID.randomUUID(), 501, null);

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void removeItem_retorna204() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/orders/" + orderId + "/items/" + itemId))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateStatus_retorna200_conNuevoEstado() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(OrderStatus.CANCELLED);
        when(orderService.updateStatus(eq(orderId), any(OrderStatusUpdateRequest.class)))
                .thenReturn(sampleResponse(orderId, OrderStatus.CANCELLED));

        mockMvc.perform(patch("/api/v1/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void updateStatus_retorna400_cuandoTransicionInvalida() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(OrderStatus.PREPARING);
        when(orderService.updateStatus(eq(orderId), any(OrderStatusUpdateRequest.class)))
                .thenThrow(new InvalidStateException("Cannot transition order"));

        mockMvc.perform(patch("/api/v1/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addPayment_retorna201_conOrdenActualizada() throws Exception {
        UUID orderId = UUID.randomUUID();
        PaymentRequest request = new PaymentRequest(PaymentMethod.CASH, new BigDecimal("116.00"));
        when(orderService.addPayment(eq(orderId), any(PaymentRequest.class)))
                .thenReturn(sampleResponse(orderId, OrderStatus.PAID));

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void addPayment_retorna400_cuandoExcedeSaldoPendiente() throws Exception {
        UUID orderId = UUID.randomUUID();
        PaymentRequest request = new PaymentRequest(PaymentMethod.CASH, new BigDecimal("999.00"));
        when(orderService.addPayment(eq(orderId), any(PaymentRequest.class)))
                .thenThrow(new InvalidStateException("Payment exceeds pending balance"));

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    // Bean Validation (@Digits(fraction = 2) en amount) debe rechazar la petición antes de
    // tocar el service.
    void addPayment_retorna400_cuandoMontoTieneMasDeDosDecimales() throws Exception {
        UUID orderId = UUID.randomUUID();
        String invalidJson = """
                {"method":"CASH","amount":10.999}
                """;

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    // Dos pagos concurrentes sobre la misma orden: el segundo choca con la versión de
    // Order ya actualizada por el primero y Hibernate lanza esta excepción, que el
    // GlobalExceptionHandler mapea a 409 en vez de un 500 sin manejar.
    void addPayment_retorna409_cuandoHayConflictoDeConcurrencia() throws Exception {
        UUID orderId = UUID.randomUUID();
        PaymentRequest request = new PaymentRequest(PaymentMethod.CASH, new BigDecimal("50.00"));
        when(orderService.addPayment(eq(orderId), any(PaymentRequest.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Order.class, orderId));

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void addPayment_retorna409_cuandoHayViolacionDeIntegridad() throws Exception {
        UUID orderId = UUID.randomUUID();
        PaymentRequest request = new PaymentRequest(PaymentMethod.CASH, new BigDecimal("50.00"));
        when(orderService.addPayment(eq(orderId), any(PaymentRequest.class)))
                .thenThrow(new DataIntegrityViolationException("constraint violation"));

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}
