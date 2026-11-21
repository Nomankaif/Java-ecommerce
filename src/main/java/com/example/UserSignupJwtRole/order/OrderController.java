package com.example.UserSignupJwtRole.order;

import com.example.UserSignupJwtRole.order.dto.OrderRequest;
import com.example.UserSignupJwtRole.order.dto.OrderResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(@Valid @RequestBody OrderRequest request, Principal principal) {
        return orderService.createOrder(request, principal.getName());
    }

    @GetMapping("/my")
    public List<OrderResponse> getMyOrders(Principal principal) {
        return orderService.getMyOrders(principal.getName());
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable Long id, Principal principal) {
        return orderService.getOrder(id, principal.getName());
    }

    @PatchMapping("/{id}/cancel")
    public OrderResponse cancelOrder(@PathVariable Long id, Principal principal) {
        return orderService.cancelOrder(id, principal.getName());
    }

    @GetMapping
    public List<OrderResponse> getAllOrders(Principal principal) {
        return orderService.getAllOrders(principal.getName());
    }
}
