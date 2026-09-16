package com.example.api.product.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.api.common.web.ApiResponse;
import com.example.api.common.web.PageResponse;
import com.example.api.product.dto.ProductRequest;
import com.example.api.product.dto.ProductResponse;
import com.example.api.product.service.ProductService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Productos", description = "CRUD de ejemplo con permisos por autoridad")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PreAuthorize("hasAuthority('PRODUCT:READ')")
    @GetMapping
    public ApiResponse<PageResponse<ProductResponse>> listProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean available,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ApiResponse.ok(productService.listProducts(search, available, pageable));
    }

    @PreAuthorize("hasAuthority('PRODUCT:READ')")
    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getProduct(@PathVariable Long id) {
        return ApiResponse.ok(productService.getProduct(id));
    }

    @PreAuthorize("hasAuthority('PRODUCT:CREATE')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        return ApiResponse.ok("Producto creado", productService.createProduct(request));
    }

    @PreAuthorize("hasAuthority('PRODUCT:UPDATE')")
    @PutMapping("/{id}")
    public ApiResponse<ProductResponse> updateProduct(@PathVariable Long id,
                                                      @Valid @RequestBody ProductRequest request) {
        return ApiResponse.ok("Producto actualizado", productService.updateProduct(id, request));
    }

    @PreAuthorize("hasAuthority('PRODUCT:DELETE')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
    }
}