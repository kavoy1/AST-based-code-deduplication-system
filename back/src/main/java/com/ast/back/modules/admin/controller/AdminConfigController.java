package com.ast.back.modules.admin.controller;

import com.ast.back.modules.admin.application.SystemConfigService;
import com.ast.back.modules.admin.dto.AdminConfigDTOs;
import com.ast.back.shared.common.Result;
import com.ast.back.shared.security.CurrentUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminConfigController {

    private final SystemConfigService systemConfigService;
    private final CurrentUserService currentUserService;

    public AdminConfigController(SystemConfigService systemConfigService, CurrentUserService currentUserService) {
        this.systemConfigService = systemConfigService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/config")
    public Result<Map<String, Object>> getConfig() {
        currentUserService.requireRole("ADMIN");
        return Result.success(systemConfigService.getAdminConfigView());
    }

    @PutMapping("/config")
    public Result<String> updateConfig(@RequestBody AdminConfigDTOs.ConfigBatchUpdateDTO dto) {
        currentUserService.requireRole("ADMIN");
        systemConfigService.updateBatch(dto == null ? null : dto.getItems(), currentUserService.getCurrentUserId());
        return Result.success("配置更新成功");
    }

    @PutMapping("/config/secret")
    public Result<String> updateSecret(@RequestBody AdminConfigDTOs.SecretUpdateDTO dto) {
        currentUserService.requireRole("ADMIN");
        systemConfigService.updateSecret(dto, currentUserService.getCurrentUserId());
        return Result.success("密钥更新成功");
    }
}
