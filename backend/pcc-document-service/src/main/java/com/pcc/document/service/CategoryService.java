package com.pcc.document.service;

import com.pcc.document.dto.request.CategoryRequest;
import com.pcc.document.dto.response.CategoryResponse;
import com.pcc.document.entity.DocumentCategory;
import com.pcc.document.exception.DuplicateResourceException;
import com.pcc.document.exception.ResourceNotFoundException;
import com.pcc.document.repository.DocumentCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final DocumentCategoryRepository categories;

    @Transactional(readOnly = true)
    public List<CategoryResponse> list(String domain) {
        var list = domain == null ? categories.findAll() : categories.findByDomainOrderByName(domain);
        return list.stream().map(this::toResponse).toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest req) {
        categories.findByCode(req.code()).ifPresent(c -> {
            throw new DuplicateResourceException("Category code exists: " + req.code());
        });
        DocumentCategory c = new DocumentCategory();
        apply(c, req);
        c.setCreatedAt(Instant.now());
        return toResponse(categories.save(c));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest req) {
        DocumentCategory c = categories.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        apply(c, req);
        return toResponse(categories.save(c));
    }

    private void apply(DocumentCategory c, CategoryRequest req) {
        c.setCode(req.code());
        c.setName(req.name());
        c.setDomain(req.domain());
        c.setDescription(req.description());
        c.setRequiresExpiry(req.requiresExpiry());
        c.setActive(req.active());
    }

    private CategoryResponse toResponse(DocumentCategory c) {
        return new CategoryResponse(c.getId(), c.getCode(), c.getName(), c.getDomain(),
                c.getDescription(), c.isRequiresExpiry(), c.isActive());
    }
}
