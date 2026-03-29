package com.ast.back.security;

import com.ast.back.common.BusinessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class SecretCryptoService {

    private static final String ENV_MASTER_KEY = "APP_MASTER_KEY";
    private static final String ENV_MASTER_KEY_FALLBACK = "AI_KEY_MASTER_KEY";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SecureRandom secureRandom = new SecureRandom();

    public String encryptToEnvelope(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            throw new BusinessException("密钥内容不能为空");
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, loadMasterKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            Map<String, Object> envelope = new HashMap<>();
            envelope.put("v", 1);
            envelope.put("alg", "AES/GCM/NoPadding");
            envelope.put("iv", Base64.getEncoder().encodeToString(iv));
            envelope.put("ct", Base64.getEncoder().encodeToString(encrypted));
            envelope.put("masked", maskSecret(plainText));
            envelope.put("createdAt", LocalDateTime.now().toString());
            return objectMapper.writeValueAsString(envelope);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("密钥加密失败");
        }
    }

    public String decryptFromEnvelope(String envelopeJson) {
        if (envelopeJson == null || envelopeJson.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> envelope = objectMapper.readValue(envelopeJson, new TypeReference<Map<String, Object>>() {
            });
            String ivBase64 = (String) envelope.get("iv");
            String cipherBase64 = (String) envelope.get("ct");
            if (ivBase64 == null || cipherBase64 == null) {
                throw new BusinessException("密钥数据格式错误");
            }

            byte[] iv = Base64.getDecoder().decode(ivBase64);
            byte[] encrypted = Base64.getDecoder().decode(cipherBase64);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, loadMasterKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] plain = cipher.doFinal(encrypted);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("密钥解密失败，请检查主密钥是否正确");
        }
    }

    public boolean hasMasterKey() {
        String key = System.getenv(ENV_MASTER_KEY);
        if (key == null || key.isBlank()) {
            key = System.getenv(ENV_MASTER_KEY_FALLBACK);
        }
        return key != null && !key.isBlank();
    }

    public String readMasked(String envelopeJson) {
        if (envelopeJson == null || envelopeJson.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> envelope = objectMapper.readValue(envelopeJson, new TypeReference<Map<String, Object>>() {
            });
            Object masked = envelope.get("masked");
            return masked == null ? null : String.valueOf(masked);
        } catch (Exception e) {
            return "****";
        }
    }

    public String maskSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            return null;
        }
        if (secret.length() <= 4) {
            return "****";
        }
        if (secret.length() <= 8) {
            return secret.charAt(0) + "****" + secret.charAt(secret.length() - 1);
        }
        return secret.substring(0, 3) + "****" + secret.substring(secret.length() - 4);
    }

    private SecretKeySpec loadMasterKey() {
        String key = System.getenv(ENV_MASTER_KEY);
        if (key == null || key.isBlank()) {
            key = System.getenv(ENV_MASTER_KEY_FALLBACK);
        }
        if (key == null || key.isBlank()) {
            throw new BusinessException("未配置主密钥环境变量 APP_MASTER_KEY（或 AI_KEY_MASTER_KEY）");
        }

        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(key);
        } catch (IllegalArgumentException e) {
            keyBytes = key.getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 16) {
            throw new BusinessException("主密钥长度不足，至少 16 字节");
        }
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            byte[] normalized = new byte[32];
            int copyLength = Math.min(keyBytes.length, normalized.length);
            System.arraycopy(keyBytes, 0, normalized, 0, copyLength);
            keyBytes = normalized;
        }
        return new SecretKeySpec(keyBytes, "AES");
    }
}

