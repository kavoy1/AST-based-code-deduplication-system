package com.ast.back.modules.plagiarism.domain;

import java.nio.file.Path;
import java.util.Set;

public record CClangAstDump(
        Path projectRoot,
        Set<Path> projectFiles,
        Path translationUnitPath,
        String astJson
) {

    public CClangAstDump {
        projectFiles = Set.copyOf(projectFiles);
    }
}
