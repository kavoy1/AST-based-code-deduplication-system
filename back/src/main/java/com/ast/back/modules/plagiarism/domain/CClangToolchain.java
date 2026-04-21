package com.ast.back.modules.plagiarism.domain;

import java.nio.file.Path;

public record CClangToolchain(
        Path executablePath,
        Path shimIncludeDir
) {
}
