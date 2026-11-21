package com.example.UserSignupJwtRole.product;


import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "products")
@Data
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String Description;
    private BigDecimal price;
    private Integer quantity;
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;
    private Instant created_at;
    private Instant updated_at;
}
