package com.example.UserSignupJwtRole.order.dto;

import com.example.UserSignupJwtRole.order.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
        Long id,
        Long userId,
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        OrderStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
