package com.example.api.product.service;

import com.example.api.common.exception.BusinessException;
import com.example.api.common.web.PageResponse;
import com.example.api.kafka.producer.ProductEventProducer;
import com.example.api.product.dto.ProductRequest;
import com.example.api.product.dto.ProductResponse;
import com.example.api.product.event.ProductEvent;
import com.example.api.product.mapper.ProductMapper;
import com.example.api.product.model.Product;
import com.example.api.product.repository.ProductRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final ProductEventProducer productEventProducer;

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> listProducts(String search, Boolean available, Pageable pageable) {
        Specification<Product> spec = buildSpecification(search, available);
        Page<ProductResponse> page = productRepository.findAll(spec, pageable)
                .map(productMapper::toResponse);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        Product product = findProduct(id);
        return productMapper.toResponse(product);
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = productMapper.toEntity(request);
        if (request.available() != null) {
            product.setAvailable(request.available());
        }
        Product saved = productRepository.save(product);
        publishCreated(saved);
        return productMapper.toResponse(saved);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = findProduct(id);
        productMapper.updateEntity(request, product);
        if (request.available() != null) {
            product.setAvailable(request.available());
        }
        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw BusinessException.notFound("PRODUCT_NOT_FOUND", "Producto no encontrado");
        }
        productRepository.deleteById(id);
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("PRODUCT_NOT_FOUND", "Producto no encontrado"));
    }

    private Specification<Product> buildSpecification(String search, Boolean available) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + search.trim().toLowerCase() + "%"));
            }
            if (available != null) {
                predicates.add(cb.equal(root.get("available"), available));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void publishCreated(Product product) {
        productEventProducer.publish(new ProductEvent(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.isAvailable(),
                Instant.now()
        ));
    }
}