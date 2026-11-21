package com.example.UserSignupJwtRole.product.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record StockRequest(
        @NotNull(message = "Quantity is required")
        @PositiveOrZero(message = "Quantity must be zero or greater")
        Integer quantity
) {
}
