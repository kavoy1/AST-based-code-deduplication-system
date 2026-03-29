package com.ast.back.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.ast.back.common.Result;
import com.ast.back.dto.AdminConfigDTOs;
import com.ast.back.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminConfigController {

    @Autowired
    private SystemConfigService systemConfigService;

    @GetMapping("/config")
    public Result<Map<String, Object>> getConfig() {
        checkAdminRole();
        return Result.success(systemConfigService.getAdminConfigView());
    }

    @PutMapping("/config")
    public Result<String> updateConfig(@RequestBody AdminConfigDTOs.ConfigBatchUpdateDTO dto) {
        checkAdminRole();
        Long adminId = StpUtil.getLoginIdAsLong();
        systemConfigService.updateBatch(dto == null ? null : dto.getItems(), adminId);
        return Result.success("配置更新成功");
    }

    @PutMapping("/config/secret")
    public Result<String> updateSecret(@RequestBody AdminConfigDTOs.SecretUpdateDTO dto) {
        checkAdminRole();
        Long adminId = StpUtil.getLoginIdAsLong();
        systemConfigService.updateSecret(dto, adminId);
        return Result.success("密钥更新成功");
    }

    private void checkAdminRole() {
        StpUtil.checkLogin();
        StpUtil.checkRole("ADMIN");
    }
}

