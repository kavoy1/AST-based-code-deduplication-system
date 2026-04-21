package com.ast.back.modules.plagiarism.domain;

import com.ast.back.modules.submission.domain.CProjectSourceFile;
import com.ast.back.modules.submission.domain.CProjectSourceIndex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CClangProjectAstDumper implements CProjectAstDumper {

    private final CClangToolchainProvider toolchainProvider;

    public CClangProjectAstDumper() {
        this(new CClangToolchainProvider());
    }

    public CClangProjectAstDumper(CClangToolchainProvider toolchainProvider) {
        this.toolchainProvider = toolchainProvider;
    }

    @Override
    public List<CClangAstDump> dump(CProjectSourceIndex sourceIndex) {
        if (sourceIndex == null || sourceIndex.sourceFiles().isEmpty()) {
            throw new IllegalArgumentException("C project sources are empty");
        }

        Path workspace = createWorkspace();
        try {
            ProjectLayout projectLayout = materializeProject(sourceIndex, workspace);
            if (projectLayout.translationUnits().isEmpty()) {
                throw new IllegalArgumentException("C project must contain at least one .c translation unit");
            }
            CClangToolchain toolchain = toolchainProvider.resolve();
            List<CClangAstDump> dumps = new ArrayList<>();
            for (Path translationUnit : projectLayout.translationUnits()) {
                dumps.add(new CClangAstDump(
                        workspace,
                        projectLayout.projectFiles(),
                        translationUnit,
                        dumpTranslationUnit(toolchain, workspace, projectLayout.includeDirs(), translationUnit)
                ));
            }
            return dumps;
        } finally {
            deleteRecursively(workspace);
        }
    }

    private ProjectLayout materializeProject(CProjectSourceIndex sourceIndex, Path workspace) {
        List<Path> translationUnits = new ArrayList<>();
        Set<Path> includeDirs = new LinkedHashSet<>();
        Set<Path> projectFiles = new LinkedHashSet<>();
        for (CProjectSourceFile sourceFile : sourceIndex.sourceFiles()) {
            Path filePath = workspace.resolve(sourceFile.path()).normalize();
            if (!filePath.startsWith(workspace)) {
                throw new IllegalArgumentException("C project contains an invalid path: " + sourceFile.path());
            }
            try {
                Files.createDirectories(filePath.getParent());
                Files.writeString(filePath, sourceFile.source(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to materialize C project sources", e);
            }
            projectFiles.add(filePath);
            if (filePath.getParent() != null) {
                includeDirs.add(filePath.getParent());
            }
            if (sourceFile.path().toLowerCase().endsWith(".c")) {
                translationUnits.add(filePath);
            }
        }
        includeDirs.add(workspace);
        translationUnits.sort(Comparator.naturalOrder());
        return new ProjectLayout(translationUnits, includeDirs, projectFiles);
    }

    private String dumpTranslationUnit(
            CClangToolchain toolchain,
            Path workspace,
            Set<Path> includeDirs,
            Path translationUnit
    ) {
        List<String> command = new ArrayList<>();
        command.add(toolchain.executablePath().toString());
        command.add("-Xclang");
        command.add("-ast-dump=json");
        command.add("-fsyntax-only");
        command.add("-std=c11");
        command.add("-Wno-everything");
        for (Path includeDir : includeDirs) {
            command.add("-I");
            command.add(includeDir.toString());
        }
        command.add("-I");
        command.add(toolchain.shimIncludeDir().toString());
        command.add(translationUnit.toString());

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workspace.toFile());
        try {
            Process process = builder.start();
            String stdout = readAll(process.getInputStream());
            String stderr = readAll(process.getErrorStream());
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IllegalArgumentException("Failed to dump C AST for " + translationUnit.getFileName() + ": " + stderr);
            }
            return stdout;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to execute bundled clang for C AST extraction", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for bundled clang", e);
        }
    }

    private Path createWorkspace() {
        try {
            return Files.createTempDirectory("ast-c-project-").toAbsolutePath().normalize();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create temporary C project workspace", e);
        }
    }

    private String readAll(InputStream inputStream) throws IOException {
        try (InputStream stream = inputStream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            stream.transferTo(output);
            return output.toString(StandardCharsets.UTF_8);
        }
    }

    private void deleteRecursively(Path root) {
        if (root == null || Files.notExists(root)) {
            return;
        }
        try (var stream = Files.walk(root)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ignored) {
            // Best effort cleanup for temporary workspaces.
        }
    }

    private record ProjectLayout(
            List<Path> translationUnits,
            Set<Path> includeDirs,
            Set<Path> projectFiles
    ) {
    }
}
