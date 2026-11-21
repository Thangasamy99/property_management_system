package com.pcc.document.controller;

import com.pcc.document.dto.request.CategoryRequest;
import com.pcc.document.dto.response.CategoryResponse;
import com.pcc.document.service.CategoryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Document Categories")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final CategoryService service;

    @GetMapping
    @PreAuthorize("hasAuthority('DOCUMENT_CATEGORY_VIEW')")
    public List<CategoryResponse> list(@RequestParam(required = false) String domain) {
        return service.list(domain);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('DOCUMENT_CATEGORY_MANAGE')")
    public CategoryResponse create(@RequestBody @Valid CategoryRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('DOCUMENT_CATEGORY_MANAGE')")
    public CategoryResponse update(@PathVariable Long id, @RequestBody @Valid CategoryRequest req) {
        return service.update(id, req);
    }
}
