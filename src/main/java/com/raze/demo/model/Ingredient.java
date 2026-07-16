package com.raze.demo.model;

import com.raze.demo.model.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ingredients")
@Getter @Setter
public class Ingredient extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "measure_unit", nullable = false, length = 20)
    private String measureUnit;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

}
