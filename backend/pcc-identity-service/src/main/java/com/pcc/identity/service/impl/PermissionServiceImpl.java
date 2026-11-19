package com.pcc.identity.service.impl;

import com.pcc.identity.dto.response.PermissionResponse;
import com.pcc.identity.entity.Permission;
import com.pcc.identity.mapper.PermissionMapper;
import com.pcc.identity.repository.PermissionRepository;
import com.pcc.identity.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> listAll() {
        return permissionRepository.findAllByOrderByModuleAscCodeAsc().stream()
                .map(PermissionMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<PermissionResponse>> groupedByModule() {
        Map<String, List<PermissionResponse>> grouped = new LinkedHashMap<>();
        for (Permission p : permissionRepository.findAllByOrderByModuleAscCodeAsc()) {
            grouped.computeIfAbsent(p.getModule(), k -> new java.util.ArrayList<>())
                    .add(PermissionMapper.toResponse(p));
        }
        return grouped;
    }
}
