package com.pcc.identity.service;

import com.pcc.identity.dto.response.PermissionResponse;

import java.util.List;
import java.util.Map;

public interface PermissionService {

    List<PermissionResponse> listAll();

    /** Permissions grouped by module name (e.g. AUTH, USER, PROPERTY). */
    Map<String, List<PermissionResponse>> groupedByModule();
}
