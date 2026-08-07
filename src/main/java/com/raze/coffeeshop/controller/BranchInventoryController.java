package com.raze.coffeeshop.controller;

import com.raze.coffeeshop.dto.BranchInventoryResponse;
import com.raze.coffeeshop.dto.InventoryMovementRequest;
import com.raze.coffeeshop.dto.InventoryMovementResponse;
import com.raze.coffeeshop.security.UserPrincipal;
import com.raze.coffeeshop.service.InventoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para el inventario de ingredientes por sucursal: consulta del stock,
 * registro de movimientos manuales (entradas, mermas, ajustes) e historial por ingrediente.
 * Los movimientos de tipo {@code SALE} no se registran aquí: los dispara automáticamente el
 * pago de una orden en {@code /api/orders}.
 */
@RestController
@RequestMapping("/api/v1/branches/{branchId}/inventory")
@RequiredArgsConstructor
public class BranchInventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public List<BranchInventoryResponse> findAll(@PathVariable UUID branchId) {
        return inventoryService.findByBranch(branchId);
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping("/movements")
    public ResponseEntity<InventoryMovementResponse> recordMovement(
            @PathVariable UUID branchId,
            @Valid @RequestBody InventoryMovementRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        InventoryMovementResponse response = inventoryService.recordMovement(branchId, request, principal.getId());
        return ResponseEntity
                .created(URI.create("/api/v1/branches/" + branchId + "/inventory"))
                .body(response);
    }

    @GetMapping("/{ingredientId}/movements")
    public List<InventoryMovementResponse> findMovements(
            @PathVariable UUID branchId,
            @PathVariable UUID ingredientId
    ) {
        return inventoryService.findMovements(branchId, ingredientId);
    }
}
