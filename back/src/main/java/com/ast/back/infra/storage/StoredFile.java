package com.ast.back.infra.storage;

public record StoredFile(String storagePath, String sha256, Long bytes) {
}
