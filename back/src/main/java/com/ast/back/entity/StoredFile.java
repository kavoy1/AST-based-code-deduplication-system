package com.ast.back.entity;

public record StoredFile(String storagePath, String sha256, Long bytes) {
}
