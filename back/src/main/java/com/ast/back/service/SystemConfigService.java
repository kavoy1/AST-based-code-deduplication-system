package com.ast.back.service;

import com.ast.back.dto.AdminConfigDTOs;
import com.ast.back.service.dto.AiRuntimeConfig;

import java.util.List;
import java.util.Map;

public interface SystemConfigService {

    Map<String, Object> getAdminConfigView();

    void updateBatch(List<AdminConfigDTOs.ConfigItemUpdateDTO> items, Long adminId);

    void updateSecret(AdminConfigDTOs.SecretUpdateDTO dto, Long adminId);

    void invalidateAll();

    String buildJobConfigSnapshot();

    AiRuntimeConfig getAiRuntimeConfig();
}
