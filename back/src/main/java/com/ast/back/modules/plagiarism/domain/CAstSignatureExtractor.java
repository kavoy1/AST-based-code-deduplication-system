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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class CAstSignatureExtractor {

    protected final CProjectAstDumper astDumper;
    protected final ObjectMapper objectMapper;

    public CAstSignatureExtractor() {
        this(new CClangProjectAstDumper(), new ObjectMapper());
    }

    public CAstSignatureExtractor(CProjectAstDumper astDumper) {
        this(astDumper, new ObjectMapper());
    }

    CAstSignatureExtractor(CProjectAstDumper astDumper, ObjectMapper objectMapper) {
        this.astDumper = astDumper;
        this.objectMapper = objectMapper;
    }

    public AstSignatureProfile extract(CProjectSourceIndex sourceIndex) {
        return mergeCoarseProfiles(astDumper.dump(sourceIndex));
    }

    protected AstSignatureProfile mergeCoarseProfiles(List<CClangAstDump> dumps) {
        Map<String, Integer> mergedCounts = new HashMap<>();
        int totalNodes = 0;
        for (CClangAstDump dump : dumps) {
            AstSignatureProfile profile = collectCoarseProfile(dump);
            totalNodes += profile.totalNodes();
            profile.signatureCounts().forEach((key, value) -> mergedCounts.merge(key, value, Integer::sum));
        }
        return new AstSignatureProfile(totalNodes, mergedCounts);
    }

    protected AstSignatureProfile collectCoarseProfile(CClangAstDump dump) {
        try {
            JsonNode root = objectMapper.readTree(dump.astJson());
            Map<String, Integer> counts = new HashMap<>();
            IntBox totalNodes = new IntBox();
            traverse(root, dump.projectFiles(), null, 0, new ArrayDeque<>(), counts, totalNodes, null);
            return new AstSignatureProfile(totalNodes.value, counts);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to parse clang AST JSON", e);
        }
    }

    protected DeepAstProfile collectDeepProfile(CClangAstDump dump) {
        try {
            JsonNode root = objectMapper.readTree(dump.astJson());
            Map<String, Integer> coarseCounts = new HashMap<>();
            Map<String, Integer> deepCounts = new HashMap<>();
            IntBox coarseTotal = new IntBox();
            IntBox deepTotal = new IntBox();
            traverse(root, dump.projectFiles(), null, 0, new ArrayDeque<>(), coarseCounts, coarseTotal, new DeepState(deepCounts, deepTotal));
            List<DeepAstMethodProfile> methodProfiles = collectMethodProfiles(root, dump.projectFiles());
            methodProfiles.sort(Comparator.comparing(DeepAstMethodProfile::methodKey));
            return new DeepAstProfile(coarseTotal.value, coarseCounts, deepTotal.value, deepCounts, methodProfiles);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to parse clang AST JSON", e);
        }
    }

    protected String signatureOf(JsonNode node) {
        String kind = node.path("kind").asText("");
        return switch (kind) {
            case "FunctionDecl" -> signatureOfFunction(node);
            case "RecordDecl" -> "Record:tag=" + node.path("tagUsed").asText("struct")
                    + ":fields=" + sizeBucket(countChildrenOfKind(node, "FieldDecl"));
            case "EnumDecl" -> "Enum:constants=" + sizeBucket(countChildrenOfKind(node, "EnumConstantDecl"));
            case "IfStmt" -> "If:else=" + (childNodes(node).size() >= 3 ? 1 : 0);
            case "ForStmt" -> "For";
            case "WhileStmt" -> "While";
            case "DoStmt" -> "DoWhile";
            case "SwitchStmt" -> "Switch";
            case "CaseStmt" -> "Case";
            case "ReturnStmt" -> "Return";
            case "BreakStmt" -> "Break";
            case "ContinueStmt" -> "Continue";
            case "GotoStmt" -> "Goto";
            case "DeclStmt" -> "DeclStmt:vars=" + sizeBucket(countChildrenOfKind(node, "VarDecl"));
            case "VarDecl" -> signatureOfVar(node);
            case "FieldDecl" -> signatureOfField(node);
            case "BinaryOperator" -> "BinaryExpr:" + node.path("opcode").asText("?");
            case "CompoundAssignOperator" -> "Assign:" + node.path("opcode").asText("?");
            case "UnaryOperator" -> "UnaryExpr:" + node.path("opcode").asText("?");
            case "CallExpr" -> "Call:arity=" + Math.max(0, childNodes(node).size() - 1);
            case "ArraySubscriptExpr" -> "ArraySubscript";
            case "MemberExpr" -> "MemberAccess";
            case "ConditionalOperator" -> "Ternary";
            case "InitListExpr" -> "InitList:size=" + sizeBucket(childNodes(node).size());
            case "IntegerLiteral", "FloatingLiteral" -> "Literal:NUM";
            case "StringLiteral" -> "Literal:STR";
            case "CharacterLiteral" -> "Literal:CHAR";
            default -> null;
        };
    }

    protected boolean shouldCount(JsonNode node, Set<Path> projectFiles, String currentFile) {
        if (node.path("isImplicit").asBoolean(false) || node.path("implicit").asBoolean(false)) {
            return false;
        }
        if (currentFile == null || currentFile.isBlank()) {
            return false;
        }
        try {
            Path normalizedFile = Path.of(currentFile).toAbsolutePath().normalize();
            return projectFiles.contains(normalizedFile);
        } catch (InvalidPathException ex) {
            return false;
        }
    }

    private String signatureOfFunction(JsonNode node) {
        int params = countChildrenOfKind(node, "ParmVarDecl");
        JsonNode body = firstChildOfKind(node, "CompoundStmt");
        int bodyStatements = body == null ? 0 : childNodes(body).size();
        int variadic = node.path("variadic").asBoolean(false) ? 1 : 0;
        return "Function:params=" + params + ":body=" + sizeBucket(bodyStatements) + ":variadic=" + variadic;
    }

    private String signatureOfVar(JsonNode node) {
        String qualType = node.path("type").path("qualType").asText("").toLowerCase(Locale.ROOT);
        int isPointer = qualType.contains("*") ? 1 : 0;
        int isArray = qualType.contains("[") ? 1 : 0;
        int hasInit = childNodes(node).isEmpty() ? 0 : 1;
        return "Var:pointer=" + isPointer + ":array=" + isArray + ":init=" + hasInit;
    }

    private String signatureOfField(JsonNode node) {
        String qualType = node.path("type").path("qualType").asText("").toLowerCase(Locale.ROOT);
        int isPointer = qualType.contains("*") ? 1 : 0;
        int isArray = qualType.contains("[") ? 1 : 0;
        return "Field:pointer=" + isPointer + ":array=" + isArray;
    }

    private List<DeepAstMethodProfile> collectMethodProfiles(JsonNode root, Set<Path> projectFiles) {
        List<FunctionNode> functions = new ArrayList<>();
        collectFunctions(root, projectFiles, null, functions);
        List<DeepAstMethodProfile> methodProfiles = new ArrayList<>();
        int index = 0;
        for (FunctionNode function : functions) {
            JsonNode body = firstChildOfKind(function.node(), "CompoundStmt");
            if (body == null) {
                continue;
            }
            Map<String, Integer> methodCounts = new HashMap<>();
            IntBox totalNodes = new IntBox();
            traverse(body, projectFiles, function.sourceFile(), 0, new ArrayDeque<>(), null, null, new DeepState(methodCounts, totalNodes));
            if (!methodCounts.isEmpty()) {
                int params = countChildrenOfKind(function.node(), "ParmVarDecl");
                methodProfiles.add(new DeepAstMethodProfile(
                        "Function#" + (++index) + "(params" + params + ")",
                        totalNodes.value,
                        methodCounts
                ));
            }
        }
        return methodProfiles;
    }

    private void collectFunctions(JsonNode node, Set<Path> projectFiles, String inheritedFile, List<FunctionNode> functions) {
        String currentFile = resolveSourceFile(node, inheritedFile);
        if ("FunctionDecl".equals(node.path("kind").asText())
                && shouldCount(node, projectFiles, currentFile)) {
            functions.add(new FunctionNode(node, currentFile));
        }
        for (JsonNode child : childNodes(node)) {
            collectFunctions(child, projectFiles, currentFile, functions);
        }
    }

    private void traverse(
            JsonNode node,
            Set<Path> projectFiles,
            String inheritedFile,
            int depth,
            Deque<String> signatureStack,
            Map<String, Integer> coarseCounts,
            IntBox coarseTotal,
            DeepState deepState
    ) {
        String currentFile = resolveSourceFile(node, inheritedFile);
        String signature = signatureOf(node);
        boolean counted = signature != null && shouldCount(node, projectFiles, currentFile);

        if (counted) {
            if (coarseCounts != null && coarseTotal != null) {
                coarseCounts.merge(signature, 1, Integer::sum);
                coarseTotal.value++;
            }
            if (deepState != null) {
                String deepSignature = signature + "@depth=" + toDepthBucket(depth);
                deepState.counts().merge(deepSignature, 1, Integer::sum);
                deepState.total().value++;
                if (!signatureStack.isEmpty()) {
                    deepState.counts().merge("Path:" + signatureStack.peekLast() + "->" + signature, 1, Integer::sum);
                    deepState.total().value++;
                }
                signatureStack.addLast(signature);
            }
        }

        for (JsonNode child : childNodes(node)) {
            traverse(child, projectFiles, currentFile, depth + 1, signatureStack, coarseCounts, coarseTotal, deepState);
        }

        if (counted && deepState != null && !signatureStack.isEmpty()) {
            signatureStack.removeLast();
        }
    }

    private String resolveSourceFile(JsonNode node, String inheritedFile) {
        String file = firstNonBlank(
                node.path("loc").path("file").asText(null),
                node.path("range").path("begin").path("file").asText(null),
                node.path("range").path("end").path("file").asText(null)
        );
        return file == null || file.isBlank() ? inheritedFile : file;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
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

    private int toDepthBucket(int depth) {
        if (depth <= 3) {
            return 1;
        }
        if (depth <= 6) {
            return 2;
        }
        return 3;
    }

    protected String sizeBucket(int size) {
        if (size <= 0) {
            return "0";
        }
        if (size <= 2) {
            return "1_2";
        }
        if (size <= 4) {
            return "3_4";
        }
        if (size <= 8) {
            return "5_8";
        }
        return "9_PLUS";
    }

    private record FunctionNode(JsonNode node, String sourceFile) {
    }

    private record DeepState(Map<String, Integer> counts, IntBox total) {
    }

    protected static final class IntBox {
        private int value;
    }
}
