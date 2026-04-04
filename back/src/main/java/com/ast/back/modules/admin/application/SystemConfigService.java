package com.ast.back.modules.admin.application;

import com.ast.back.modules.admin.dto.AdminConfigDTOs;
import com.ast.back.modules.ai.dto.AiRuntimeConfig;

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
