package com.raze.demo.model;

import com.raze.demo.model.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "branch_inventory",
        uniqueConstraints = @UniqueConstraint(columnNames = {"branch_id", "ingredient_id"})
)
public class BranchInventory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Column(name = "current_quantity", precision = 10, scale = 3)
    private BigDecimal currentQuantity = BigDecimal.ZERO;

    @Column(name = "minimum_stock", precision = 10, scale = 3)
    private BigDecimal minimumStock = BigDecimal.ZERO;

    @UpdateTimestamp
    @Column(name = "last_updated")
    private OffsetDateTime lastUpdated;
}
