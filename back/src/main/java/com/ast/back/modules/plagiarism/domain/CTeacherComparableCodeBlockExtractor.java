package com.ast.back.modules.plagiarism.domain;

import com.ast.back.modules.submission.domain.CProjectSourceIndex;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class CTeacherComparableCodeBlockExtractor {

    private static final int MIN_METHOD_STATEMENTS = 2;
    private static final int MIN_CONTROL_STATEMENTS = 1;
    private static final int MIN_METHOD_LINES = 3;
    private static final int MIN_CONTROL_LINES = 2;

    private final CProjectAstDumper astDumper;
    private final ObjectMapper objectMapper;
    private final CAstSignatureExtractor signatureExtractor;

    public CTeacherComparableCodeBlockExtractor() {
        this(new CClangProjectAstDumper(), new ObjectMapper());
    }

    public CTeacherComparableCodeBlockExtractor(CProjectAstDumper astDumper) {
        this(astDumper, new ObjectMapper());
    }

    CTeacherComparableCodeBlockExtractor(CProjectAstDumper astDumper, ObjectMapper objectMapper) {
        this.astDumper = astDumper;
        this.objectMapper = objectMapper;
        this.signatureExtractor = new CAstSignatureExtractor(astDumper, objectMapper);
    }

    public List<TeacherComparableCodeBlock> extract(CProjectSourceIndex sourceIndex) {
        if (sourceIndex == null || !sourceIndex.hasSourceFiles()) {
            return List.of();
        }

        Map<String, TeacherComparableCodeBlock> deduplicated = new LinkedHashMap<>();
        for (CClangAstDump dump : astDumper.dump(sourceIndex)) {
            collectBlocksForDump(dump, deduplicated);
        }
        return deduplicated.values().stream()
                .sorted(Comparator
                        .comparing(TeacherComparableCodeBlock::fileName)
                        .thenComparing(TeacherComparableCodeBlock::fullStartLine)
                        .thenComparing(TeacherComparableCodeBlock::fullEndLine))
                .toList();
    }

    private void collectBlocksForDump(CClangAstDump dump, Map<String, TeacherComparableCodeBlock> deduplicated) {
        try {
            JsonNode root = objectMapper.readTree(dump.astJson());
            traverse(root, dump, null, null, 0, deduplicated);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to parse clang AST JSON for comparable C blocks", e);
        }
    }

    private void traverse(
            JsonNode node,
            CClangAstDump dump,
            String inheritedFile,
            String methodKey,
            int relativeDepth,
            Map<String, TeacherComparableCodeBlock> deduplicated
    ) {
        String currentFile = resolveSourceFile(node, inheritedFile);
        String nextMethodKey = methodKey;
        int nextRelativeDepth = relativeDepth;

        if (isFunctionNode(node, dump.projectFiles(), currentFile)) {
            TeacherComparableCodeBlock methodBlock = toBlock(
                    "METHOD",
                    node,
                    dump.projectRoot(),
                    currentFile,
                    buildMethodKey(node, dump.projectRoot(), currentFile),
                    0,
                    MIN_METHOD_STATEMENTS,
                    MIN_METHOD_LINES
            );
            if (methodBlock != null) {
                deduplicated.putIfAbsent(uniqueKey(methodBlock), methodBlock);
                nextMethodKey = methodBlock.methodKey();
            }
            nextRelativeDepth = 0;
        } else if (nextMethodKey != null && isControlNode(node, dump.projectFiles(), currentFile)) {
            String blockKind = resolveControlKind(node);
            TeacherComparableCodeBlock block = toBlock(
                    blockKind,
                    node,
                    dump.projectRoot(),
                    currentFile,
                    nextMethodKey,
                    Math.max(1, relativeDepth),
                    MIN_CONTROL_STATEMENTS,
                    MIN_CONTROL_LINES
            );
            if (block != null) {
                deduplicated.putIfAbsent(uniqueKey(block), block);
            }
            nextRelativeDepth = relativeDepth + 1;
        }

        for (JsonNode child : childNodes(node)) {
            traverse(child, dump, currentFile, nextMethodKey, nextRelativeDepth, deduplicated);
        }
    }

    private TeacherComparableCodeBlock toBlock(
            String kind,
            JsonNode node,
            Path projectRoot,
            String currentFile,
            String methodKey,
            int relativeDepth,
            int minStatements,
            int minLines
    ) {
        String displayFileName = toDisplayFileName(projectRoot, currentFile);
        if (displayFileName == null) {
            return null;
        }

        int localStartLine = resolveLineNumber(node.path("range").path("begin"), node.path("loc"));
        int localEndLine = resolveLineNumber(node.path("range").path("end"), node.path("loc"));
        if (localStartLine <= 0 || localEndLine <= 0 || localEndLine - localStartLine + 1 < minLines) {
            return null;
        }

        StructuralProfile structuralProfile = buildStructuralProfile(node, currentFile);
        if (structuralProfile.totalNodes() <= 0 || structuralProfile.signatureCounts().isEmpty()) {
            return null;
        }

        int statementCount = countStatementLikeNodes(node, currentFile);
        if (statementCount < minStatements) {
            return null;
        }

        String normalizedSnippet = buildNormalizedSnippet(kind, structuralProfile);
        return new TeacherComparableCodeBlock(
                kind,
                displayFileName,
                buildSummary(kind),
                normalizedSnippet,
                localStartLine,
                localEndLine,
                localStartLine,
                localEndLine,
                statementCount,
                methodKey,
                relativeDepth,
                structuralProfile.totalNodes(),
                structuralProfile.signatureCounts()
        );
    }

    private boolean isFunctionNode(JsonNode node, Set<Path> projectFiles, String currentFile) {
        return "FunctionDecl".equals(node.path("kind").asText())
                && signatureExtractor.shouldCount(node, projectFiles, currentFile)
                && firstChildOfKind(node, "CompoundStmt") != null;
    }

    private boolean isControlNode(JsonNode node, Set<Path> projectFiles, String currentFile) {
        return signatureExtractor.shouldCount(node, projectFiles, currentFile)
                && switch (node.path("kind").asText()) {
                    case "IfStmt", "ForStmt", "WhileStmt", "DoStmt", "SwitchStmt", "CaseStmt", "DefaultStmt" -> true;
                    default -> false;
                };
    }

    private String resolveControlKind(JsonNode node) {
        return switch (node.path("kind").asText()) {
            case "IfStmt" -> childNodes(node).size() >= 3 ? "IF_ELSE" : "IF_THEN";
            case "ForStmt" -> "FOR";
            case "WhileStmt" -> "WHILE";
            case "DoStmt" -> "DO_WHILE";
            case "SwitchStmt" -> "SWITCH";
            case "CaseStmt", "DefaultStmt" -> "CASE_GROUP";
            default -> "BLOCK";
        };
    }

    private String buildMethodKey(JsonNode functionNode, Path projectRoot, String currentFile) {
        String fileName = toDisplayFileName(projectRoot, currentFile);
        int startLine = resolveLineNumber(functionNode.path("range").path("begin"), functionNode.path("loc"));
        int params = countChildrenOfKind(functionNode, "ParmVarDecl");
        return (fileName == null ? "c-file" : fileName) + "#L" + startLine + "(params" + params + ")";
    }

    private StructuralProfile buildStructuralProfile(JsonNode node, String currentFile) {
        Map<String, Integer> signatureCounts = new LinkedHashMap<>();
        IntBox totalNodes = new IntBox();
        Deque<String> signatureStack = new ArrayDeque<>();
        traverseStructural(node, currentFile, 0, signatureStack, signatureCounts, totalNodes);
        String controlSkeleton = buildControlSkeleton(node);
        if (!controlSkeleton.isBlank()) {
            signatureCounts.merge("Flow:" + controlSkeleton, 1, Integer::sum);
            totalNodes.value++;
        }
        return new StructuralProfile(totalNodes.value, signatureCounts);
    }

    private void traverseStructural(
            JsonNode node,
            String currentFile,
            int depth,
            Deque<String> signatureStack,
            Map<String, Integer> signatureCounts,
            IntBox totalNodes
    ) {
        String nodeFile = resolveSourceFile(node, currentFile);
        String signature = signatureExtractor.signatureOf(node);
        boolean counted = signature != null && Objects.equals(currentFile, nodeFile);

        if (counted) {
            signatureCounts.merge("Node:" + signature, 1, Integer::sum);
            totalNodes.value++;
            signatureCounts.merge("Depth:" + signature + "@d=" + toDepthBucket(depth), 1, Integer::sum);
            totalNodes.value++;
            if (!signatureStack.isEmpty()) {
                signatureCounts.merge("Path:" + signatureStack.peekLast() + "->" + signature, 1, Integer::sum);
                totalNodes.value++;
            }
            signatureStack.addLast(signature);
        }

        for (JsonNode child : childNodes(node)) {
            traverseStructural(child, nodeFile, depth + 1, signatureStack, signatureCounts, totalNodes);
        }

        if (counted && !signatureStack.isEmpty()) {
            signatureStack.removeLast();
        }
    }

    private int countStatementLikeNodes(JsonNode node, String currentFile) {
        return countStatementLikeNodes(node, currentFile, new IntBox());
    }

    private int countStatementLikeNodes(JsonNode node, String currentFile, IntBox box) {
        String nodeFile = resolveSourceFile(node, currentFile);
        if (Objects.equals(currentFile, nodeFile) && isStatementLike(node.path("kind").asText())) {
            box.value++;
        }
        for (JsonNode child : childNodes(node)) {
            countStatementLikeNodes(child, nodeFile, box);
        }
        return box.value;
    }

    private boolean isStatementLike(String kind) {
        return switch (kind) {
            case "IfStmt", "ForStmt", "WhileStmt", "DoStmt", "SwitchStmt", "CaseStmt", "DefaultStmt",
                    "ReturnStmt", "BreakStmt", "ContinueStmt", "GotoStmt", "CallExpr",
                    "BinaryOperator", "CompoundAssignOperator", "UnaryOperator" -> true;
            default -> false;
        };
    }

    private String buildNormalizedSnippet(String kind, StructuralProfile structuralProfile) {
        List<String> normalized = structuralProfile.signatureCounts().entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByKey())
                .limit(12)
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .toList();
        return kind + "::" + String.join("|", normalized);
    }

    private String buildSummary(String kind) {
        return switch (kind) {
            case "METHOD" -> "函数流程结构";
            case "IF_THEN" -> "if 分支结构";
            case "IF_ELSE" -> "if-else 分支结构";
            case "FOR" -> "for 循环结构";
            case "WHILE" -> "while 循环结构";
            case "DO_WHILE" -> "do-while 循环结构";
            case "SWITCH" -> "switch 分支结构";
            case "CASE_GROUP" -> "case 分组结构";
            default -> "结构片段";
        };
    }

    private String buildControlSkeleton(JsonNode node) {
        List<String> skeleton = new ArrayList<>();
        collectControlSkeleton(node, null, skeleton);
        if (skeleton.isEmpty()) {
            return "";
        }
        return String.join(">", skeleton.subList(0, Math.min(6, skeleton.size())));
    }

    private void collectControlSkeleton(JsonNode node, String inheritedFile, List<String> skeleton) {
        String currentFile = resolveSourceFile(node, inheritedFile);
        String kind = node.path("kind").asText();
        if (Objects.equals(inheritedFile, currentFile) || inheritedFile == null) {
            switch (kind) {
                case "IfStmt" -> skeleton.add(childNodes(node).size() >= 3 ? "IF_ELSE" : "IF");
                case "ForStmt" -> skeleton.add("FOR");
                case "WhileStmt" -> skeleton.add("WHILE");
                case "DoStmt" -> skeleton.add("DO_WHILE");
                case "SwitchStmt" -> skeleton.add("SWITCH");
                case "CaseStmt", "DefaultStmt" -> skeleton.add("CASE");
                default -> {
                }
            }
        }
        for (JsonNode child : childNodes(node)) {
            collectControlSkeleton(child, currentFile, skeleton);
        }
    }

    private int resolveLineNumber(JsonNode lineNode, JsonNode fallbackNode) {
        int direct = lineNode.path("line").asInt(0);
        if (direct > 0) {
            return direct;
        }
        int spelling = lineNode.path("spellingLoc").path("line").asInt(0);
        if (spelling > 0) {
            return spelling;
        }
        int expansion = lineNode.path("expansionLoc").path("line").asInt(0);
        if (expansion > 0) {
            return expansion;
        }
        int fallback = fallbackNode.path("line").asInt(0);
        if (fallback > 0) {
            return fallback;
        }
        int fallbackSpelling = fallbackNode.path("spellingLoc").path("line").asInt(0);
        if (fallbackSpelling > 0) {
            return fallbackSpelling;
        }
        return fallbackNode.path("expansionLoc").path("line").asInt(0);
    }

    private String resolveSourceFile(JsonNode node, String inheritedFile) {
        String file = firstNonBlank(
                node.path("loc").path("file").asText(null),
                node.path("loc").path("spellingLoc").path("file").asText(null),
                node.path("loc").path("expansionLoc").path("file").asText(null),
                node.path("range").path("begin").path("file").asText(null),
                node.path("range").path("begin").path("spellingLoc").path("file").asText(null),
                node.path("range").path("begin").path("expansionLoc").path("file").asText(null),
                node.path("range").path("end").path("file").asText(null),
                node.path("range").path("end").path("spellingLoc").path("file").asText(null),
                node.path("range").path("end").path("expansionLoc").path("file").asText(null)
        );
        return file == null || file.isBlank() ? inheritedFile : file;
    }

    private String toDisplayFileName(Path projectRoot, String currentFile) {
        if (currentFile == null || currentFile.isBlank()) {
            return null;
        }
        try {
            Path normalized = Path.of(currentFile).toAbsolutePath().normalize();
            if (normalized.startsWith(projectRoot)) {
                return projectRoot.relativize(normalized).toString().replace('\\', '/');
            }
            return normalized.getFileName() == null ? normalized.toString().replace('\\', '/') : normalized.getFileName().toString();
        } catch (InvalidPathException ex) {
            return currentFile.replace('\\', '/');
        }
    }

    private String uniqueKey(TeacherComparableCodeBlock block) {
        return block.fileName() + "::" + block.kind() + "::" + block.fullStartLine() + "::" + block.fullEndLine();
    }

    private int countChildrenOfKind(JsonNode node, String kind) {
        int count = 0;
        for (JsonNode child : childNodes(node)) {
            if (Objects.equals(kind, child.path("kind").asText())) {
                count++;
            }
        }
        return count;
    }

    private JsonNode firstChildOfKind(JsonNode node, String kind) {
        for (JsonNode child : childNodes(node)) {
            if (Objects.equals(kind, child.path("kind").asText())) {
                return child;
            }
        }
        return null;
    }

    private List<JsonNode> childNodes(JsonNode node) {
        JsonNode inner = node.path("inner");
        if (!inner.isArray()) {
            return List.of();
        }
        List<JsonNode> children = new ArrayList<>();
        inner.forEach(children::add);
        return children;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private int toDepthBucket(int depth) {
        if (depth <= 1) {
            return 1;
        }
        if (depth <= 3) {
            return 2;
        }
        return 3;
    }

    private record StructuralProfile(int totalNodes, Map<String, Integer> signatureCounts) {
    }

    private static final class IntBox {
        private int value;
    }
}
