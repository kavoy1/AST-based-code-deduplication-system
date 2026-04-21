package com.ast.back.modules.submission.domain;

import java.util.List;

public record CProjectSourceIndex(List<CProjectSourceFile> sourceFiles, List<String> ignoredEntries) {

    public CProjectSourceIndex {
        sourceFiles = List.copyOf(sourceFiles);
        ignoredEntries = List.copyOf(ignoredEntries);
    }

    public boolean hasSourceFiles() {
        return !sourceFiles.isEmpty();
    }

    public boolean hasImplementationFiles() {
        return sourceFiles.stream().anyMatch(file -> file.path().toLowerCase().endsWith(".c"));
    }
}
