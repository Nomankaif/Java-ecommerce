package com.example.UserSignupJwtRole.product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:product-api-tests",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class ProductApiTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ProductRepository repository;

    private static final String PRODUCT = """
            {"name":"Keyboard","description":"Wireless","price":1500.00,"quantity":20}
            """;

    @BeforeEach
    void clearProducts() {
        repository.deleteAll();
    }

    @Test
    void adminCanCreateUpdateStockAndRemoveProduct() throws Exception {
        mvc.perform(post("/product/add").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(PRODUCT))
                .andExpect(status().isCreated());

        Long id = repository.findAll().get(0).getId();
        mvc.perform(put("/product/{id}", id).with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Mouse","description":"Wired","price":500.25,"quantity":10}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mouse"))
                .andExpect(jsonPath("$.price").value(500.25));

        mvc.perform(patch("/product/{id}/stock", id).with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":30}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(30));
        assertEquals(30, repository.findById(id).orElseThrow().getQuantity());

        mvc.perform(get("/product/{id}", id).with(user("reader").roles("USER")))
                .andExpect(status().isOk());
        mvc.perform(delete("/product/{id}", id).with(user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent());
        assertFalse(repository.findById(id).orElseThrow().isActive());
        mvc.perform(get("/product").with(user("reader").roles("USER")))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/product/{id}", id).with(user("reader").roles("USER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCanReadButCannotChangeProducts() throws Exception {
        mvc.perform(get("/product").with(user("reader").roles("USER")))
                .andExpect(status().isOk());
        mvc.perform(post("/product/add").with(user("reader").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content(PRODUCT))
                .andExpect(status().isForbidden());
        mvc.perform(put("/product/1").with(user("reader").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content(PRODUCT))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/product/1/stock").with(user("reader").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":30}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/product/1").with(user("reader").roles("USER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/product")).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidInputIsRejectedAndMissingProductsReturn404() throws Exception {
        mvc.perform(post("/product/add").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"price\":-1,\"quantity\":-1}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/product/99999").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(PRODUCT))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/product/99999/stock").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":-1}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/product/99999/stock").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/product/99999").with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }
}
