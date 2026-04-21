package com.ast.back.modules.plagiarism.domain;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class CClangToolchainProvider {

    private static final String RESOURCE_ROOT = "org/bytedeco/llvm";

    private final Path cacheRoot;

    public CClangToolchainProvider() {
        this(defaultCacheRoot());
    }

    public CClangToolchainProvider(Path cacheRoot) {
        this.cacheRoot = cacheRoot.toAbsolutePath().normalize();
    }

    public CClangToolchain resolve() {
        String platformDir = resolvePlatformDirectory();
        String executableName = resolveExecutableName();
        Path targetRoot = cacheRoot.resolve(platformDir);
        Path executablePath = targetRoot.resolve(executableName);
        Path shimIncludeDir = targetRoot.resolve("ast-shims");

        if (!Files.exists(executablePath)) {
            extractBundledToolchain(platformDir, targetRoot);
        }
        CStandardLibraryShimWriter.writeTo(shimIncludeDir);
        if (!Files.exists(executablePath)) {
            throw new IllegalStateException("Bundled clang executable is unavailable for platform " + platformDir);
        }
        executablePath.toFile().setExecutable(true);
        return new CClangToolchain(executablePath, shimIncludeDir);
    }

    private void extractBundledToolchain(String platformDir, Path targetRoot) {
        String resourcePath = RESOURCE_ROOT + "/" + platformDir + "/" + resolveExecutableName();
        URL resource = Thread.currentThread().getContextClassLoader().getResource(resourcePath);
        if (resource == null) {
            throw new IllegalStateException("Unable to find bundled clang toolchain resource " + resourcePath);
        }

        try {
            Files.createDirectories(targetRoot);
            URI resourceUri = resource.toURI();
            if ("jar".equalsIgnoreCase(resourceUri.getScheme())) {
                copyFromJar(resourceUri, resourcePath.substring(0, resourcePath.lastIndexOf('/')), targetRoot);
                return;
            }
            Path sourceDir = Paths.get(resourceUri).getParent();
            copyRecursively(sourceDir, targetRoot);
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException("Failed to unpack bundled clang toolchain", e);
        }
    }

    private void copyFromJar(URI resourceUri, String sourceDirPath, Path targetRoot) throws IOException {
        String uriText = resourceUri.toString();
        int separatorIndex = uriText.indexOf('!');
        URI jarUri = URI.create(uriText.substring(0, separatorIndex));
        FileSystem fileSystem = null;
        boolean shouldClose = false;
        try {
            try {
                fileSystem = FileSystems.newFileSystem(jarUri, Map.of());
                shouldClose = true;
            } catch (FileSystemAlreadyExistsException ignored) {
                fileSystem = FileSystems.getFileSystem(jarUri);
            }
            Path sourceDir = fileSystem.getPath("/" + sourceDirPath);
            copyRecursively(sourceDir, targetRoot);
        } finally {
            if (shouldClose && fileSystem != null) {
                fileSystem.close();
            }
        }
    }

    private void copyRecursively(Path sourceDir, Path targetRoot) throws IOException {
        try (var stream = Files.walk(sourceDir)) {
            for (Path sourcePath : stream.sorted(Comparator.naturalOrder()).toList()) {
                Path relativePath = sourceDir.relativize(sourcePath);
                Path targetPath = targetRoot.resolve(relativePath.toString()).normalize();
                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.createDirectories(Objects.requireNonNull(targetPath.getParent()));
                    Files.copy(sourcePath, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    targetPath.toFile().setExecutable(true);
                }
            }
        }
    }

    private String resolvePlatformDirectory() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String arch = normalizeArch(System.getProperty("os.arch", ""));
        if (os.contains("win")) {
            return "windows-" + arch;
        }
        if (os.contains("mac")) {
            return "macosx-" + arch;
        }
        if (os.contains("nux") || os.contains("nix")) {
            return "linux-" + arch;
        }
        throw new IllegalStateException("Unsupported operating system for bundled clang: " + os);
    }

    private String normalizeArch(String arch) {
        String value = arch.toLowerCase(Locale.ROOT);
        if ("amd64".equals(value)) {
            return "x86_64";
        }
        if ("x86_64".equals(value) || "arm64".equals(value)) {
            return value;
        }
        if ("aarch64".equals(value)) {
            return "arm64";
        }
        throw new IllegalStateException("Unsupported architecture for bundled clang: " + arch);
    }

    private String resolveExecutableName() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return os.contains("win") ? "clang.exe" : "clang";
    }

    private static Path defaultCacheRoot() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null && !localAppData.isBlank()) {
                return Paths.get(localAppData, ".ast-clang-toolchain");
            }
        }
        return Paths.get(System.getProperty("user.home"), ".ast-clang-toolchain");
    }
}
