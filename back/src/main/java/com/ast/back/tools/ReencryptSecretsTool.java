package com.ast.back.tools;

import com.ast.back.BackApplication;
import com.ast.back.entity.SysConfig;
import com.ast.back.service.SysConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReencryptSecretsTool {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        String oldKey = System.getenv("OLD_APP_MASTER_KEY");
        String newKey = System.getenv("NEW_APP_MASTER_KEY");
        if (isBlank(oldKey) || isBlank(newKey)) {
            System.err.println("缂哄皯鐜鍙橀噺 OLD_APP_MASTER_KEY 鎴?NEW_APP_MASTER_KEY");
            System.exit(1);
            return;
        }

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(BackApplication.class)
                .web(WebApplicationType.NONE)
                .run(args)) {
            SysConfigService sysConfigService = context.getBean(SysConfigService.class);
            List<SysConfig> secretConfigs = sysConfigService.lambdaQuery()
                    .eq(SysConfig::getType, "SECRET")
                    .list();

            int success = 0;
            for (SysConfig config : secretConfigs) {
                if (config.getValue() == null || config.getValue().isBlank()) {
                    continue;
                }
                String plain = decryptEnvelope(config.getValue(), oldKey);
                String reencrypted = encryptEnvelope(plain, newKey);
                config.setValue(reencrypted);
                config.setUpdatedAt(LocalDateTime.now());
                sysConfigService.updateById(config);
                success++;
            }
            System.out.println("閲嶅姞瀵嗗畬鎴愶紝鏇存柊璁板綍鏁? " + success);
        }
    }

    private static String encryptEnvelope(String plainText, String keySeed) throws Exception {
        byte[] iv = new byte[12];
        new java.security.SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, loadKey(keySeed), new GCMParameterSpec(128, iv));
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        Map<String, Object> envelope = new HashMap<>();
        envelope.put("v", 1);
        envelope.put("alg", "AES/GCM/NoPadding");
        envelope.put("iv", Base64.getEncoder().encodeToString(iv));
        envelope.put("ct", Base64.getEncoder().encodeToString(encrypted));
        envelope.put("masked", mask(plainText));
        envelope.put("createdAt", LocalDateTime.now().toString());
        return OBJECT_MAPPER.writeValueAsString(envelope);
    }

    private static String decryptEnvelope(String envelopeJson, String keySeed) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> envelope = OBJECT_MAPPER.readValue(envelopeJson, Map.class);
        String ivBase64 = String.valueOf(envelope.get("iv"));
        String ctBase64 = String.valueOf(envelope.get("ct"));
        byte[] iv = Base64.getDecoder().decode(ivBase64);
        byte[] encrypted = Base64.getDecoder().decode(ctBase64);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, loadKey(keySeed), new GCMParameterSpec(128, iv));
        byte[] plain = cipher.doFinal(encrypted);
        return new String(plain, StandardCharsets.UTF_8);
    }

    private static SecretKeySpec loadKey(String keySeed) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(keySeed);
        } catch (IllegalArgumentException e) {
            keyBytes = keySeed.getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            byte[] normalized = new byte[32];
            int copyLength = Math.min(keyBytes.length, normalized.length);
            System.arraycopy(keyBytes, 0, normalized, 0, copyLength);
            keyBytes = normalized;
        }
        return new SecretKeySpec(keyBytes, "AES");
    }

    private static String mask(String secret) {
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

    private static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }
}

