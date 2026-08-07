package com.raze.coffeeshop.controller;

import com.raze.coffeeshop.dto.BranchInventoryResponse;
import com.raze.coffeeshop.dto.InventoryMovementResponse;
import com.raze.coffeeshop.enums.MovementType;
import com.raze.coffeeshop.security.JwtService;
import com.raze.coffeeshop.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TEST DE "SLICE" (capa web) — mismo patrón que {@link ProductControllerTest}. Solo cubre los
 * GET de consulta: el POST de movimientos lee {@code @AuthenticationPrincipal UserPrincipal},
 * cuyo resolver (igual que el interceptor de {@code @PreAuthorize}) no se activa en un slice
 * {@code @WebMvcTest}, así que ese endpoint y la autorización por rol se prueban de punta a
 * punta en {@code InventoryIntegrationTest}.
 */
@WebMvcTest(BranchInventoryController.class)
@WithMockUser(roles = "ADMIN")
class BranchInventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryService inventoryService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void getAll_retorna200_conStockDeLaSucursal() throws Exception {
        UUID branchId = UUID.randomUUID();
        BranchInventoryResponse response = new BranchInventoryResponse(
                UUID.randomUUID(), branchId, UUID.randomUUID(), "Leche entera", "ml",
                new BigDecimal("500.000"), new BigDecimal("100.000"), OffsetDateTime.now());
        when(inventoryService.findByBranch(branchId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/branches/" + branchId + "/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ingredientName").value("Leche entera"))
                .andExpect(jsonPath("$[0].currentQuantity").value(500.000));
    }

    @Test
    void findMovements_retorna200_conHistorial() throws Exception {
        UUID branchId = UUID.randomUUID();
        UUID ingredientId = UUID.randomUUID();
        InventoryMovementResponse response = new InventoryMovementResponse(
                UUID.randomUUID(), ingredientId, MovementType.SALE, new BigDecimal("300.000"),
                "Order 1", UUID.randomUUID(), OffsetDateTime.now());
        when(inventoryService.findMovements(branchId, ingredientId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/branches/" + branchId + "/inventory/" + ingredientId + "/movements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("SALE"));
    }
}
