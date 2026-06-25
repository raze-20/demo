package com.raze.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode
@Embeddable
public class RecipeId implements Serializable {

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "ingredient_id")
    private UUID ingredientId;
}
