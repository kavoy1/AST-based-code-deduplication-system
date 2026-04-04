package com.ast.back.modules.admin.application.impl;

import com.ast.back.shared.common.BusinessException;
import com.ast.back.modules.admin.dto.AdminConfigDTOs;
import com.ast.back.modules.admin.persistence.entity.SysConfig;
import com.ast.back.shared.security.SecretCryptoService;
import com.ast.back.modules.admin.application.SysConfigService;
import com.ast.back.modules.admin.application.SystemConfigService;
import com.ast.back.modules.ai.dto.AiRuntimeConfig;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class SystemConfigServiceImpl implements SystemConfigService {

    private static final long CACHE_TTL_MILLIS = 10_000L;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String TYPE_STRING = "STRING";
    private static final String TYPE_INT = "INT";
    private static final String TYPE_DOUBLE = "DOUBLE";
    private static final String TYPE_BOOL = "BOOL";
    private static final String TYPE_JSON = "JSON";
    private static final String TYPE_SECRET = "SECRET";

    private record ConfigMeta(String key, String type, String group, String defaultValue, boolean secret) {
    }

    private static final Map<String, ConfigMeta> CONFIG_META_MAP;

    static {
        Map<String, ConfigMeta> map = new LinkedHashMap<>();
        map.put("plagiarism.threshold", new ConfigMeta("plagiarism.threshold", TYPE_DOUBLE, "plagiarism", "0.8", false));
        map.put("plagiarism.topK", new ConfigMeta("plagiarism.topK", TYPE_INT, "plagiarism", "20", false));
        map.put("upload.max_files", new ConfigMeta("upload.max_files", TYPE_INT, "upload", "20", false));
        map.put("upload.max_file_size_mb", new ConfigMeta("upload.max_file_size_mb", TYPE_INT, "upload", "5", false));
        map.put("upload.allowed_exts", new ConfigMeta("upload.allowed_exts", TYPE_JSON, "upload", "[\".java\"]", false));
        map.put("storage.base_path", new ConfigMeta("storage.base_path", TYPE_STRING, "storage", "uploads", false));
        map.put("ai.enabled", new ConfigMeta("ai.enabled", TYPE_BOOL, "ai", "false", false));
        map.put("ai.provider", new ConfigMeta("ai.provider", TYPE_STRING, "ai", "QWEN", false));
        map.put("ai.base_url", new ConfigMeta("ai.base_url", TYPE_STRING, "ai", "https://dashscope.aliyuncs.com/compatible-mode/v1", false));
        map.put("ai.model", new ConfigMeta("ai.model", TYPE_STRING, "ai", "qwen-plus", false));
        map.put("ai.timeout_ms", new ConfigMeta("ai.timeout_ms", TYPE_INT, "ai", "8000", false));
        map.put("ai.prompt_version", new ConfigMeta("ai.prompt_version", TYPE_STRING, "ai", "QWEN_EXPLAIN_V1", false));
        map.put("ai.api_key", new ConfigMeta("ai.api_key", TYPE_SECRET, "ai", "", true));
        CONFIG_META_MAP = Collections.unmodifiableMap(map);
    }

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private SecretCryptoService secretCryptoService;

    @Value("${app.ai.enabled:false}")
    private boolean appAiEnabled;

    @Value("${app.ai.provider:QWEN}")
    private String appAiProvider;

    @Value("${app.ai.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String appAiBaseUrl;

    @Value("${app.ai.dashscope-api-key:}")
    private String appAiApiKey;

    @Value("${app.ai.model:qwen-plus}")
    private String appAiModel;

    @Value("${app.ai.timeout-ms:8000}")
    private int appAiTimeoutMs;

    @Value("${app.ai.prompt-version:QWEN_EXPLAIN_V1}")
    private String appAiPromptVersion;

    private volatile long cacheExpireAt = 0L;
    private final Map<String, SysConfig> cacheData = new ConcurrentHashMap<>();

    @Override
    public Map<String, Object> getAdminConfigView() {
        Map<String, SysConfig> data = loadCurrentConfig();
        Map<String, Object> result = new LinkedHashMap<>();
        for (ConfigMeta meta : CONFIG_META_MAP.values()) {
            SysConfig config = data.get(meta.key());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("key", meta.key());
            item.put("type", meta.type());
            item.put("group", meta.group());
            item.put("secret", meta.secret());
            if (meta.secret()) {
                String raw = config == null ? null : config.getValue();
                item.put("configured", raw != null && !raw.isBlank());
                item.put("masked", raw == null ? null : secretCryptoService.readMasked(raw));
            } else {
                item.put("value", config == null ? parseTyped(meta.type(), meta.defaultValue()) : parseTyped(meta.type(), config.getValue()));
            }
            result.put(meta.key(), item);
        }
        return result;
    }

    @Override
    public void updateBatch(List<AdminConfigDTOs.ConfigItemUpdateDTO> items, Long adminId) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (AdminConfigDTOs.ConfigItemUpdateDTO item : items) {
            if (item == null || item.getKey() == null) {
                continue;
            }
            ConfigMeta meta = CONFIG_META_MAP.get(item.getKey());
            if (meta == null) {
                throw new BusinessException("不支持的配置项：" + item.getKey());
            }
            if (meta.secret()) {
                throw new BusinessException("密钥配置请使用专用接口");
            }
            validateValue(meta, item.getValue());
            saveOrUpdate(meta.key(), item.getValue(), meta.type(), adminId);
        }
        invalidateAll();
    }

    @Override
    public void updateSecret(AdminConfigDTOs.SecretUpdateDTO dto, Long adminId) {
        if (dto == null || dto.getKey() == null || dto.getKey().isBlank()) {
            throw new BusinessException("缺少密钥配置 key");
        }
        ConfigMeta meta = CONFIG_META_MAP.get(dto.getKey());
        if (meta == null || !meta.secret()) {
            throw new BusinessException("仅允许更新密钥类型配置");
        }
        boolean clear = Boolean.TRUE.equals(dto.getClear());
        if (clear) {
            saveOrUpdate(meta.key(), "", meta.type(), adminId);
            invalidateAll();
            return;
        }
        if (dto.getSecretValue() == null || dto.getSecretValue().isBlank()) {
            throw new BusinessException("密钥内容不能为空");
        }
        String envelope = secretCryptoService.encryptToEnvelope(dto.getSecretValue());
        saveOrUpdate(meta.key(), envelope, meta.type(), adminId);
        invalidateAll();
    }

    @Override
    public void invalidateAll() {
        cacheExpireAt = 0L;
        cacheData.clear();
    }

    @Override
    public String buildJobConfigSnapshot() {
        Map<String, SysConfig> data = loadCurrentConfig();
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("plagiarism", Map.of(
                "threshold", getDoubleValue(data, "plagiarism.threshold"),
                "topK", getIntValue(data, "plagiarism.topK")
        ));
        snapshot.put("upload", Map.of(
                "maxFiles", getIntValue(data, "upload.max_files"),
                "maxFileSizeMb", getIntValue(data, "upload.max_file_size_mb"),
                "allowedExts", getJsonArray(data, "upload.allowed_exts")
        ));
        snapshot.put("storage", Map.of(
                "basePath", getStringValue(data, "storage.base_path")
        ));
        String keyRaw = getRawValue(data, "ai.api_key");
        snapshot.put("ai", Map.of(
                "enabled", getBooleanWithFallback(data, "ai.enabled", appAiEnabled),
                "provider", getStringWithFallback(data, "ai.provider", appAiProvider),
                "baseUrl", getStringWithFallback(data, "ai.base_url", appAiBaseUrl),
                "model", getStringWithFallback(data, "ai.model", appAiModel),
                "timeoutMs", getIntWithFallback(data, "ai.timeout_ms", appAiTimeoutMs),
                "promptVersion", getStringWithFallback(data, "ai.prompt_version", appAiPromptVersion),
                "apiKeyConfigured", keyRaw != null && !keyRaw.isBlank(),
                "apiKeyMasked", keyRaw == null ? null : secretCryptoService.readMasked(keyRaw)
        ));
        try {
            return OBJECT_MAPPER.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new BusinessException("构建配置快照失败");
        }
    }

    @Override
    public AiRuntimeConfig getAiRuntimeConfig() {
        Map<String, SysConfig> data = loadCurrentConfig();
        String rawSecret = getRawValue(data, "ai.api_key");
        String decryptedKey = null;
        if (rawSecret != null && !rawSecret.isBlank()) {
            try {
                decryptedKey = secretCryptoService.decryptFromEnvelope(rawSecret);
            } catch (BusinessException ignored) {
                decryptedKey = rawSecret;
            }
        } else if (appAiApiKey != null && !appAiApiKey.isBlank()) {
            decryptedKey = appAiApiKey;
        }
        return new AiRuntimeConfig(
                getBooleanWithFallback(data, "ai.enabled", appAiEnabled),
                getStringWithFallback(data, "ai.provider", appAiProvider),
                getStringWithFallback(data, "ai.base_url", appAiBaseUrl),
                getStringWithFallback(data, "ai.model", appAiModel),
                getIntWithFallback(data, "ai.timeout_ms", appAiTimeoutMs),
                decryptedKey,
                getStringWithFallback(data, "ai.prompt_version", appAiPromptVersion)
        );
    }

    private void saveOrUpdate(String key, String value, String type, Long adminId) {
        SysConfig existing = sysConfigService.getOne(new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getKey, key));
        if (existing == null) {
            SysConfig config = new SysConfig();
            config.setKey(key);
            config.setValue(value);
            config.setType(type);
            config.setUpdatedAt(LocalDateTime.now());
            config.setUpdatedBy(adminId);
            sysConfigService.save(config);
            return;
        }
        existing.setValue(value);
        existing.setType(type);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(adminId);
        sysConfigService.updateById(existing);
    }

    private Map<String, SysConfig> loadCurrentConfig() {
        long now = System.currentTimeMillis();
        if (now < cacheExpireAt && !cacheData.isEmpty()) {
            return cacheData;
        }
        synchronized (this) {
            now = System.currentTimeMillis();
            if (now < cacheExpireAt && !cacheData.isEmpty()) {
                return cacheData;
            }
            List<SysConfig> list;
            try {
                list = sysConfigService.list();
            } catch (Exception ignored) {
                cacheData.clear();
                cacheExpireAt = now + CACHE_TTL_MILLIS;
                return cacheData;
            }
            cacheData.clear();
            for (SysConfig config : list) {
                cacheData.put(config.getKey(), config);
            }
            cacheExpireAt = now + CACHE_TTL_MILLIS;
        }
        return cacheData;
    }

    private void validateValue(ConfigMeta meta, String rawValue) {
        if (rawValue == null) {
            throw new BusinessException("配置值不能为空: " + meta.key());
        }
        String type = meta.type().toUpperCase(Locale.ROOT);
        switch (type) {
            case TYPE_STRING -> {
                if (meta.key().equals("storage.base_path") && rawValue.isBlank()) {
                    throw new BusinessException("存储路径不能为空");
                }
            }
            case TYPE_INT -> {
                int value;
                try {
                    value = Integer.parseInt(rawValue);
                } catch (NumberFormatException e) {
                    throw new BusinessException("配置值必须是整数: " + meta.key());
                }
                if (meta.key().equals("plagiarism.topK") && value <= 0) {
                    throw new BusinessException("TopK 必须大于 0");
                }
                if (meta.key().equals("upload.max_files") && value <= 0) {
                    throw new BusinessException("上传文件数必须大于 0");
                }
                if (meta.key().equals("upload.max_file_size_mb") && value <= 0) {
                    throw new BusinessException("上传大小必须大于 0");
                }
                if (meta.key().equals("ai.timeout_ms") && value < 1000) {
                    throw new BusinessException("AI 超时时间不能小于 1000ms");
                }
            }
            case TYPE_DOUBLE -> {
                double value;
                try {
                    value = Double.parseDouble(rawValue);
                } catch (NumberFormatException e) {
                    throw new BusinessException("配置值必须是小数: " + meta.key());
                }
                if (meta.key().equals("plagiarism.threshold") && (value < 0 || value > 1)) {
                    throw new BusinessException("阈值必须在 0 到 1 之间");
                }
            }
            case TYPE_BOOL -> {
                if (!"true".equalsIgnoreCase(rawValue) && !"false".equalsIgnoreCase(rawValue)) {
                    throw new BusinessException("布尔配置仅支持 true/false: " + meta.key());
                }
            }
            case TYPE_JSON -> {
                try {
                    List<String> extensions = OBJECT_MAPPER.readValue(rawValue, new TypeReference<List<String>>() {
                    });
                    if (extensions.isEmpty()) {
                        throw new BusinessException("扩展名列表不能为空");
                    }
                    for (String ext : extensions) {
                        if (ext == null || ext.isBlank() || !ext.startsWith(".")) {
                            throw new BusinessException("扩展名格式错误，应以 . 开头");
                        }
                    }
                } catch (BusinessException e) {
                    throw e;
                } catch (Exception e) {
                    throw new BusinessException("JSON 配置格式错误: " + meta.key());
                }
            }
            default -> throw new BusinessException("未知配置类型: " + type);
        }
    }

    private Object parseTyped(String type, String rawValue) {
        if (rawValue == null) {
            return null;
        }
        try {
            return switch (type.toUpperCase(Locale.ROOT)) {
                case TYPE_INT -> Integer.parseInt(rawValue);
                case TYPE_DOUBLE -> Double.parseDouble(rawValue);
                case TYPE_BOOL -> Boolean.parseBoolean(rawValue);
                case TYPE_JSON -> OBJECT_MAPPER.readValue(rawValue, new TypeReference<List<String>>() {
                });
                default -> rawValue;
            };
        } catch (Exception e) {
            return rawValue;
        }
    }

    private String getRawValue(Map<String, SysConfig> data, String key) {
        ConfigMeta meta = CONFIG_META_MAP.get(key);
        SysConfig config = data.get(key);
        if (config == null) {
            return meta == null ? null : meta.defaultValue();
        }
        return config.getValue();
    }

    private String getStringValue(Map<String, SysConfig> data, String key) {
        String raw = getRawValue(data, key);
        return raw == null ? "" : raw;
    }

    private String getStringWithFallback(Map<String, SysConfig> data, String key, String fallback) {
        SysConfig config = data.get(key);
        if (config == null || config.getValue() == null || config.getValue().isBlank()) {
            return fallback == null ? "" : fallback;
        }
        return config.getValue();
    }

    private Integer getIntValue(Map<String, SysConfig> data, String key) {
        String raw = getRawValue(data, key);
        try {
            return Integer.parseInt(raw);
        } catch (Exception e) {
            return Integer.parseInt(CONFIG_META_MAP.get(key).defaultValue());
        }
    }

    private int getIntWithFallback(Map<String, SysConfig> data, String key, int fallback) {
        SysConfig config = data.get(key);
        if (config == null || config.getValue() == null || config.getValue().isBlank()) {
            return fallback;
        }
        return Integer.parseInt(config.getValue());
    }

    private Double getDoubleValue(Map<String, SysConfig> data, String key) {
        String raw = getRawValue(data, key);
        try {
            return Double.parseDouble(raw);
        } catch (Exception e) {
            return Double.parseDouble(CONFIG_META_MAP.get(key).defaultValue());
        }
    }

    private Boolean getBooleanWithFallback(Map<String, SysConfig> data, String key, boolean fallback) {
        SysConfig config = data.get(key);
        if (config == null || config.getValue() == null || config.getValue().isBlank()) {
            return fallback;
        }
        return Boolean.parseBoolean(config.getValue());
    }

    private List<String> getJsonArray(Map<String, SysConfig> data, String key) {
        String raw = getRawValue(data, key);
        if (raw == null || raw.isBlank()) {
            return List.of(".java");
        }
        try {
            List<String> result = OBJECT_MAPPER.readValue(raw, new TypeReference<List<String>>() {
            });
            return result == null ? List.of(".java") : result;
        } catch (Exception e) {
            return List.of(".java");
        }
    }

    public Map<String, ConfigMeta> getConfigMetaMap() {
        return CONFIG_META_MAP;
    }

    public Set<String> supportedKeys() {
        return CONFIG_META_MAP.keySet();
    }

    public List<String> supportedSecretKeys() {
        return CONFIG_META_MAP.values().stream()
                .filter(ConfigMeta::secret)
                .map(ConfigMeta::key)
                .collect(Collectors.toList());
    }
}
