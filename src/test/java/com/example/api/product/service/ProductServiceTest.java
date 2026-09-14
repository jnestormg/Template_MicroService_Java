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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private ProductEventProducer productEventProducer;

    @InjectMocks
    private ProductService productService;

    private Product buildProduct() {
        return Product.builder()
                .id(1L)
                .name("Laptop")
                .price(new BigDecimal("1200.00"))
                .available(true)
                .build();
    }

    private ProductResponse buildResponse(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getDescription(),
                product.getPrice(), product.isAvailable(), null, null);
    }

    @Test
    void createProduct_savesAndPublishesEvent() {
        Product product = buildProduct();
        ProductRequest request = new ProductRequest("Laptop", "Portatil", new BigDecimal("1200.00"), true);

        when(productMapper.toEntity(request)).thenReturn(product);
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(buildResponse(product));

        ProductResponse result = productService.createProduct(request);

        assertThat(result.name()).isEqualTo("Laptop");
        verify(productRepository).save(product);
        verify(productEventProducer).publish(any(ProductEvent.class));
    }

    @Test
    void getProduct_returnsProductWhenExists() {
        Product product = buildProduct();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(buildResponse(product));

        ProductResponse result = productService.getProduct(1L);

        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    void getProduct_throwsWhenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProduct(99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus().value()).isEqualTo(404));
    }

    @Test
    void deleteProduct_deletesExistingProduct() {
        when(productRepository.existsById(1L)).thenReturn(true);

        productService.deleteProduct(1L);

        verify(productRepository).deleteById(1L);
    }

    @Test
    void deleteProduct_throwsWhenNotFound() {
        when(productRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> productService.deleteProduct(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus().value()).isEqualTo(404));

        verify(productRepository, never()).deleteById(anyLong());
    }

    @Test
    void listProducts_paginatesResults() {
        Product product = buildProduct();
        Page<Product> page = new org.springframework.data.domain.PageImpl<>(
                java.util.List.of(product),
                org.springframework.data.domain.PageRequest.of(0, 10),
                1);

        when(productRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class),
                any(org.springframework.data.domain.Pageable.class))).thenReturn(page);
        when(productMapper.toResponse(product)).thenReturn(buildResponse(product));

        PageResponse<ProductResponse> result = productService.listProducts(
                "lap", true, org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).name()).isEqualTo("Laptop");
    }
}