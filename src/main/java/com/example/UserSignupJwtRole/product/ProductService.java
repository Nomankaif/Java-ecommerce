package com.example.UserSignupJwtRole.product;


import com.example.UserSignupJwtRole.product.dto.ProductRequest;
import com.example.UserSignupJwtRole.product.dto.ProductResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    @Autowired
    public ProductRepository productRepository;

    public ProductResponse createProduct(ProductRequest request){
        Product product = new Product();

        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setQuantity(request.quantity());

        Instant now = Instant.now();
        product.setCreated_at(now);
        product.setUpdated_at(now);

        Product savedProduct = productRepository.save(product);

        return toResponse(savedProduct);
    }

    public List<ProductResponse> getAllProducts() {
        List<ProductResponse> responses = new ArrayList<>();
        for (Product product : productRepository.findAllByActiveTrue()) {
            responses.add(toResponse(product));
        }
        return responses;
    }

    public ProductResponse getProduct(Long id) {
        Product product = findProduct(id);
        return toResponse(product);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = findProductForUpdate(id);
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setQuantity(request.quantity());
        product.setUpdated_at(Instant.now());

        Product savedProduct = productRepository.save(product);
        return toResponse(savedProduct);
    }

    @Transactional
    public ProductResponse updateStock(Long id, Integer quantity) {
        Product product = findProductForUpdate(id);
        product.setQuantity(quantity);
        product.setUpdated_at(Instant.now());

        Product savedProduct = productRepository.save(product);
        return toResponse(savedProduct);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = findProductForUpdate(id);
        // Keep the row so future orders can still reference this product.
        product.setActive(false);
        product.setUpdated_at(Instant.now());
        productRepository.save(product);
    }

    private Product findProductForUpdate(Long id) {
        return productRepository.findByIdForUpdate(id)
                .filter(Product::isActive)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Product not found"));
    }

    private Product findProduct(Long id) {
        return productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Product not found"));
    }

    private ProductResponse toResponse(Product savedProduct) {
        return new ProductResponse(
                savedProduct.getId(),
                savedProduct.getName(),
                savedProduct.getDescription(),
                savedProduct.getPrice(),
                savedProduct.getQuantity(),
                savedProduct.getCreated_at(),
                savedProduct.getUpdated_at()
        );
    }
}
