package com.ast.back.modules.admin.dto;

import lombok.Data;

import java.util.List;

public class AdminConfigDTOs {

    @Data
    public static class ConfigItemUpdateDTO {
        private String key;
        private String value;
    }

    @Data
    public static class ConfigBatchUpdateDTO {
        private List<ConfigItemUpdateDTO> items;
    }

    @Data
    public static class SecretUpdateDTO {
        private String key;
        private String secretValue;
        private Boolean clear;
    }
}

