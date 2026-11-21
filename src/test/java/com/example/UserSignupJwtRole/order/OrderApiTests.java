package com.example.UserSignupJwtRole.order;

import com.example.UserSignupJwtRole.order.dto.OrderRequest;
import com.example.UserSignupJwtRole.order.dto.OrderResponse;
import com.example.UserSignupJwtRole.product.Product;
import com.example.UserSignupJwtRole.product.ProductRepository;
import com.example.UserSignupJwtRole.security.JwtService;
import com.example.UserSignupJwtRole.user.AppUser;
import com.example.UserSignupJwtRole.user.UserRepository;
import com.example.UserSignupJwtRole.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:order-api-tests;LOCK_TIMEOUT=10000",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class OrderApiTests {
    @Autowired private MockMvc mvc;
    @Autowired private OrderRepository orders;
    @Autowired private ProductRepository products;
    @Autowired private UserRepository users;
    @Autowired private OrderService service;
    @Autowired private JwtService jwt;

    private Long productId;
    private String ownerToken;
    private String otherToken;
    private String adminToken;

    @BeforeEach
    void setup() {
        orders.deleteAll();
        products.deleteAll();
        users.deleteAll();
        addUser("owner@example.com", UserRole.USER);
        addUser("other@example.com", UserRole.USER);
        addUser("admin@example.com", UserRole.ADMIN);
        ownerToken = "Bearer " + jwt.generateToken("owner@example.com");
        otherToken = "Bearer " + jwt.generateToken("other@example.com");
        adminToken = "Bearer " + jwt.generateToken("admin@example.com");
        Product product = new Product();
        product.setName("Keyboard");
        product.setPrice(new BigDecimal("1500.25"));
        product.setQuantity(5);
        product.setCreated_at(Instant.now());
        product.setUpdated_at(Instant.now());
        productId = products.save(product).getId();
    }

    private void addUser(String email, UserRole role) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setName("Test user");
        user.setPassword("unused-by-jwt-authentication");
        user.setRole(role);
        users.save(user);
    }

    @Test
    void orderUsesAuthenticatedOwnerAndSavedPriceWithRestrictedReads() throws Exception {
        mvc.perform(post("/orders").header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"quantity\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalPrice").value(3000.50))
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.userId").value(users.findByEmail("owner@example.com").orElseThrow().getId()));
        Long id = orders.findAll().get(0).getId();
        assertEquals(3, products.findById(productId).orElseThrow().getQuantity());
        Product product = products.findById(productId).orElseThrow();
        product.setPrice(new BigDecimal("2000.00"));
        products.save(product);
        mvc.perform(get("/orders/{id}", id).header("Authorization", ownerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.unitPrice").value(1500.25));
        mvc.perform(get("/orders/{id}", id).header("Authorization", otherToken))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/orders/{id}/cancel", id).header("Authorization", otherToken))
                .andExpect(status().isForbidden());
        mvc.perform(get("/orders/my").header("Authorization", otherToken))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/orders/my").header("Authorization", ownerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/orders").header("Authorization", ownerToken)).andExpect(status().isForbidden());
        mvc.perform(get("/orders").header("Authorization", adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/orders/{id}", id).header("Authorization", adminToken)).andExpect(status().isOk());
        mvc.perform(patch("/orders/{id}/cancel", id).header("Authorization", adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(patch("/orders/{id}/cancel", id).header("Authorization", ownerToken))
                .andExpect(status().isConflict());
        assertEquals(5, products.findById(productId).orElseThrow().getQuantity());
    }

    @Test
    void invalidUnavailableAndMissingProductsDoNotCreateOrders() throws Exception {
        for (String body : new String[]{"{}", "{\"productId\":1,\"quantity\":0}",
                "{\"productId\":1,\"quantity\":-2}"}) {
            mvc.perform(post("/orders").header("Authorization", ownerToken)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/orders").header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"quantity\":6}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/orders").header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":999999,\"quantity\":1}"))
                .andExpect(status().isNotFound());
        Product product = products.findById(productId).orElseThrow();
        product.setActive(false);
        products.save(product);
        mvc.perform(post("/orders").header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"quantity\":1}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/orders/999999").header("Authorization", ownerToken))
                .andExpect(status().isNotFound());
        assertEquals(0, orders.count());
        assertEquals(5, products.findById(productId).orElseThrow().getQuantity());
        mvc.perform(get("/orders/my")).andExpect(status().isUnauthorized());
        mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void concurrentPurchasesCannotOversell() throws Exception {
        runTwice(() -> service.createOrder(new OrderRequest(productId, 4), "owner@example.com"));
        assertEquals(1, orders.count());
        assertEquals(1, products.findById(productId).orElseThrow().getQuantity());
    }

    @Test
    void concurrentCancellationRestoresStockOnceEvenForInactiveProduct() throws Exception {
        OrderResponse order = service.createOrder(new OrderRequest(productId, 2), "owner@example.com");
        Product product = products.findById(productId).orElseThrow();
        product.setActive(false);
        products.save(product);
        runTwice(() -> service.cancelOrder(order.id(), "owner@example.com"));
        assertEquals(5, products.findById(productId).orElseThrow().getQuantity());
        assertEquals(OrderStatus.CANCELLED, orders.findById(order.id()).orElseThrow().getStatus());
    }

    private void runTwice(Runnable action) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> task = () -> {
            start.await();
            try {
                action.run();
                return 200;
            } catch (ResponseStatusException exception) {
                return exception.getStatusCode().value();
            }
        };
        try {
            Future<Integer> first = executor.submit(task);
            Future<Integer> second = executor.submit(task);
            start.countDown();
            int firstStatus = first.get(20, TimeUnit.SECONDS);
            int secondStatus = second.get(20, TimeUnit.SECONDS);
            assertTrue((firstStatus == 200 && secondStatus == 409)
                    || (firstStatus == 409 && secondStatus == 200));
        } finally {
            executor.shutdownNow();
        }
    }
}
