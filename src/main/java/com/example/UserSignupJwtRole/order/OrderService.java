package com.example.UserSignupJwtRole.order;

import com.example.UserSignupJwtRole.order.dto.OrderRequest;
import com.example.UserSignupJwtRole.order.dto.OrderResponse;
import com.example.UserSignupJwtRole.product.Product;
import com.example.UserSignupJwtRole.product.ProductRepository;
import com.example.UserSignupJwtRole.user.AppUser;
import com.example.UserSignupJwtRole.user.UserRepository;
import com.example.UserSignupJwtRole.user.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private UserRepository userRepository;

    public OrderResponse createOrder(OrderRequest request, String email) {
        AppUser user = findUser(email);
        // Lock this product until the transaction finishes to prevent overselling.
        Product product = productRepository.findByIdForUpdate(request.productId())
                .filter(Product::isActive)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Product not found"));

        if (product.getQuantity() < request.quantity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough stock");
        }

        Instant now = Instant.now();
        AppOrder order = new AppOrder();
        order.setUser(user);
        order.setProduct(product);
        order.setProductName(product.getName());
        order.setQuantity(request.quantity());
        order.setUnitPrice(product.getPrice());
        order.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(request.quantity())));
        order.setStatus(OrderStatus.PLACED);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        product.setQuantity(product.getQuantity() - request.quantity());
        product.setUpdated_at(now);
        productRepository.save(product);
        return toResponse(orderRepository.save(order));
    }

    public List<OrderResponse> getMyOrders(String email) {
        AppUser user = findUser(email);
        return toResponses(orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()));
    }

    public List<OrderResponse> getAllOrders(String email) {
        if (findUser(email).getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
        return toResponses(orderRepository.findAllByOrderByCreatedAtDesc());
    }

    public OrderResponse getOrder(Long id, String email) {
        AppOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        checkAccess(order, findUser(email));
        return toResponse(order);
    }

    public OrderResponse cancelOrder(Long id, String email) {
        AppUser user = findUser(email);
        // Lock the order so simultaneous cancellation requests cannot restore stock twice.
        AppOrder order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        checkAccess(order, user);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order is already cancelled");
        }

        Product product = productRepository.findByIdForUpdate(order.getProduct().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        long restoredQuantity = (long) product.getQuantity() + order.getQuantity();
        if (restoredQuantity > Integer.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Stock exceeds the supported limit");
        }
        Instant now = Instant.now();
        product.setQuantity((int) restoredQuantity);
        product.setUpdated_at(now);
        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(now);
        productRepository.save(product);
        return toResponse(orderRepository.save(order));
    }

    private AppUser findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private void checkAccess(AppOrder order, AppUser user) {
        boolean owner = order.getUser().getId().equals(user.getId());
        if (!owner && user.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot access this order");
        }
    }

    private List<OrderResponse> toResponses(List<AppOrder> orders) {
        List<OrderResponse> responses = new ArrayList<>();
        for (AppOrder order : orders) {
            responses.add(toResponse(order));
        }
        return responses;
    }

    private OrderResponse toResponse(AppOrder order) {
        return new OrderResponse(order.getId(), order.getUser().getId(),
                order.getProduct().getId(), order.getProductName(), order.getQuantity(),
                order.getUnitPrice(), order.getTotalPrice(), order.getStatus(),
                order.getCreatedAt(), order.getUpdatedAt());
    }
}
