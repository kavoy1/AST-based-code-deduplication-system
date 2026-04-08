package com.ast.back.modules.plagiarism.domain;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TextBlockLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.TypeParameter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TeacherComparableCodeBlockExtractor {

    private static final int MIN_NORMALIZED_LENGTH = 24;
    private static final int MIN_STATEMENT_COUNT = 2;
    private final JavaAstSignatureExtractor signatureExtractor = new JavaAstSignatureExtractor();

    public List<TeacherComparableCodeBlock> extract(String fileName, String source, int sourceStartLineInFullCode) {
        if (source == null || source.isBlank()) {
            return List.of();
        }

        CompilationUnit root;
        try {
            root = StaticJavaParser.parse(source);
        } catch (ParseProblemException ex) {
            return List.of();
        }

        List<TeacherComparableCodeBlock> blocks = new ArrayList<>();
        int methodIndex = 0;
        for (MethodDeclaration method : root.findAll(MethodDeclaration.class)) {
            if (method.getBody().isEmpty()) {
                continue;
            }
            String methodKey = method.getNameAsString() + "#" + (++methodIndex) + "(参数" + method.getParameters().size() + ")";
            collectBlock("METHOD", fileName, method, sourceStartLineInFullCode, methodKey, blocks);
            collectBlocks(method.findAll(IfStmt.class), fileName, sourceStartLineInFullCode, methodKey, blocks);
            collectBlocks(method.findAll(ForStmt.class), "FOR", fileName, sourceStartLineInFullCode, methodKey, blocks);
            collectBlocks(method.findAll(ForEachStmt.class), "FOREACH", fileName, sourceStartLineInFullCode, methodKey, blocks);
            collectBlocks(method.findAll(WhileStmt.class), "WHILE", fileName, sourceStartLineInFullCode, methodKey, blocks);
            collectBlocks(method.findAll(DoStmt.class), "DO_WHILE", fileName, sourceStartLineInFullCode, methodKey, blocks);
            collectBlocks(method.findAll(TryStmt.class), "TRY", fileName, sourceStartLineInFullCode, methodKey, blocks);
            collectBlocks(method.findAll(CatchClause.class), "CATCH", fileName, sourceStartLineInFullCode, methodKey, blocks);
            collectBlocks(method.findAll(SwitchStmt.class), "SWITCH", fileName, sourceStartLineInFullCode, methodKey, blocks);
            collectBlocks(method.findAll(SwitchEntry.class), "CASE_GROUP", fileName, sourceStartLineInFullCode, methodKey, blocks);
        }
        blocks.sort(Comparator
                .comparing(TeacherComparableCodeBlock::fullStartLine)
                .thenComparing(TeacherComparableCodeBlock::fullEndLine));
        return blocks;
    }

    private void collectBlocks(
            List<? extends Node> nodes,
            String kind,
            String fileName,
            int sourceStartLineInFullCode,
            String methodKey,
            List<TeacherComparableCodeBlock> output
    ) {
        for (Node node : nodes) {
            collectBlock(kind, fileName, node, sourceStartLineInFullCode, methodKey, output);
        }
    }

    private void collectBlocks(
            List<IfStmt> nodes,
            String fileName,
            int sourceStartLineInFullCode,
            String methodKey,
            List<TeacherComparableCodeBlock> output
    ) {
        for (IfStmt node : nodes) {
            String kind = resolveIfKind(node);
            collectBlock(kind, fileName, node, sourceStartLineInFullCode, methodKey, output);
        }
    }

    private void collectBlock(
            String kind,
            String fileName,
            Node node,
            int sourceStartLineInFullCode,
            String methodKey,
            List<TeacherComparableCodeBlock> output
    ) {
        TeacherComparableCodeBlock block = toBlock(kind, fileName, node, sourceStartLineInFullCode, methodKey);
        if (block != null) {
            output.add(block);
        }
    }

    private TeacherComparableCodeBlock toBlock(
            String kind,
            String fileName,
            Node node,
            int sourceStartLineInFullCode,
            String methodKey
    ) {
        if (node.getRange().isEmpty()) {
            return null;
        }

        int localStartLine = node.getRange().get().begin.line;
        int localEndLine = node.getRange().get().end.line;
        if (localStartLine >= localEndLine) {
            return null;
        }

        int statementCount = countStatements(node);
        if (statementCount < minimumStatementCount(kind)) {
            return null;
        }

        String normalizedSnippet = normalizeSnippet(node);
        if (normalizedSnippet == null || normalizedSnippet.length() < minimumNormalizedLength(kind)) {
            return null;
        }

        StructuralProfile structuralProfile = buildStructuralProfile(node);
        if (structuralProfile.totalNodes() <= 0 || structuralProfile.signatureCounts().isEmpty()) {
            return null;
        }

        return new TeacherComparableCodeBlock(
                kind,
                fileName,
                buildSummary(kind, node),
                normalizedSnippet,
                localStartLine,
                localEndLine,
                sourceStartLineInFullCode + localStartLine - 1,
                sourceStartLineInFullCode + localEndLine - 1,
                statementCount,
                methodKey,
                resolveRelativeDepth(node),
                structuralProfile.totalNodes(),
                structuralProfile.signatureCounts()
        );
    }

    private String resolveIfKind(IfStmt node) {
        Node parent = node.getParentNode().orElse(null);
        if (parent instanceof IfStmt parentIf
                && parentIf.getElseStmt().isPresent()
                && parentIf.getElseStmt().get() == node) {
            return "ELSE_IF";
        }
        return node.getElseStmt().isPresent() ? "IF_ELSE" : "IF_THEN";
    }

    private StructuralProfile buildStructuralProfile(Node node) {
        Node normalized = node.clone();
        normalizeNode(normalized);

        Map<String, Integer> signatureCounts = new HashMap<>();
        int totalNodes = 0;
        for (Node current : normalized.findAll(Node.class)) {
            String signature = signatureExtractor.signatureOf(current);
            if (signature == null) {
                continue;
            }
            mergeSignature(signatureCounts, "Node:" + signature);
            totalNodes++;
            mergeSignature(signatureCounts, "Depth:" + signature + "@d=" + toRelativeDepthBucket(normalized, current));
            totalNodes++;

            String pathSignature = buildParentPathSignature(normalized, current, signature);
            if (pathSignature != null) {
                mergeSignature(signatureCounts, "Path:" + pathSignature);
                totalNodes++;
            }
        }
        String controlSkeleton = buildControlSkeleton(normalized);
        if (!controlSkeleton.isBlank()) {
            mergeSignature(signatureCounts, "Flow:" + controlSkeleton);
            totalNodes++;
        }

        return new StructuralProfile(totalNodes, signatureCounts);
    }

    private int countStatements(Node node) {
        return (int) node.findAll(Statement.class).stream()
                .filter(statement -> !(statement instanceof BlockStmt))
                .count();
    }

    private int minimumStatementCount(String kind) {
        return switch (kind) {
            case "IF_THEN", "IF_ELSE", "ELSE_IF", "CATCH", "CASE_GROUP" -> 1;
            default -> MIN_STATEMENT_COUNT;
        };
    }

    private int minimumNormalizedLength(String kind) {
        return switch (kind) {
            case "IF_THEN", "IF_ELSE", "ELSE_IF", "CATCH", "CASE_GROUP" -> 8;
            default -> MIN_NORMALIZED_LENGTH;
        };
    }

    private String normalizeSnippet(Node node) {
        Node normalized = node.clone();
        normalized.findAll(SimpleName.class).forEach(name -> name.setIdentifier("ID"));
        normalized.findAll(ClassOrInterfaceType.class).forEach(type -> type.setName("TYPE"));
        normalized.findAll(PrimitiveType.class).forEach(type -> type.setType(PrimitiveType.Primitive.INT));
        normalized.findAll(TypeParameter.class).forEach(type -> type.setName("TYPE"));
        normalized.findAll(StringLiteralExpr.class).forEach(expr -> expr.setString("STR"));
        normalized.findAll(TextBlockLiteralExpr.class).forEach(expr -> expr.setValue("STR"));
        normalized.findAll(CharLiteralExpr.class).forEach(expr -> expr.setValue("C"));
        normalized.findAll(IntegerLiteralExpr.class).forEach(expr -> expr.setValue("0"));
        normalized.findAll(LongLiteralExpr.class).forEach(expr -> expr.setValue("0L"));
        normalized.findAll(DoubleLiteralExpr.class).forEach(expr -> expr.setValue("0.0"));
        normalized.findAll(BooleanLiteralExpr.class).forEach(expr -> expr.setValue(true));
        normalized.findAll(NameExpr.class).forEach(expr -> expr.setName("ID"));
        return normalized.toString().replaceAll("\\s+", " ").trim();
    }

    private void normalizeNode(Node node) {
        node.findAll(SimpleName.class).forEach(name -> name.setIdentifier("ID"));
        node.findAll(ClassOrInterfaceType.class).forEach(type -> type.setName("TYPE"));
        node.findAll(PrimitiveType.class).forEach(type -> type.setType(PrimitiveType.Primitive.INT));
        node.findAll(TypeParameter.class).forEach(type -> type.setName("TYPE"));
        node.findAll(StringLiteralExpr.class).forEach(expr -> expr.setString("STR"));
        node.findAll(TextBlockLiteralExpr.class).forEach(expr -> expr.setValue("STR"));
        node.findAll(CharLiteralExpr.class).forEach(expr -> expr.setValue("C"));
        node.findAll(IntegerLiteralExpr.class).forEach(expr -> expr.setValue("0"));
        node.findAll(LongLiteralExpr.class).forEach(expr -> expr.setValue("0L"));
        node.findAll(DoubleLiteralExpr.class).forEach(expr -> expr.setValue("0.0"));
        node.findAll(BooleanLiteralExpr.class).forEach(expr -> expr.setValue(true));
        node.findAll(NameExpr.class).forEach(expr -> expr.setName("ID"));
    }

    private String buildSummary(String kind, Node node) {
        if (node instanceof MethodDeclaration methodDeclaration) {
            return methodDeclaration.getNameAsString() + "() 方法逻辑";
        }
        return switch (kind) {
            case "IF_THEN" -> "if 分支逻辑";
            case "IF_ELSE" -> "if-else 分支逻辑";
            case "ELSE_IF" -> "else-if 分支逻辑";
            case "FOR" -> "for 循环逻辑";
            case "FOREACH" -> "foreach 循环逻辑";
            case "WHILE" -> "while 循环逻辑";
            case "DO_WHILE" -> "do-while 循环逻辑";
            case "TRY" -> "异常处理逻辑";
            case "CATCH" -> "catch 异常处理逻辑";
            case "SWITCH" -> "switch 分支逻辑";
            case "CASE_GROUP" -> "case 分组逻辑";
            default -> "相似代码片段";
        };
    }

    private int resolveRelativeDepth(Node node) {
        int depth = 0;
        Node cursor = node.getParentNode().orElse(null);
        while (cursor != null && !(cursor instanceof MethodDeclaration)) {
            if (cursor instanceof Statement || cursor instanceof CatchClause) {
                depth++;
            }
            cursor = cursor.getParentNode().orElse(null);
        }
        return depth;
    }

    private int toRelativeDepthBucket(Node root, Node node) {
        int depth = 0;
        Node cursor = node;
        while (cursor != null && cursor != root) {
            cursor = cursor.getParentNode().orElse(null);
            if (cursor != null && cursor != root) {
                depth++;
            }
        }
        if (depth <= 1) {
            return 1;
        }
        if (depth <= 3) {
            return 2;
        }
        return 3;
    }

    private String buildParentPathSignature(Node root, Node node, String signature) {
        Node cursor = node.getParentNode().orElse(null);
        while (cursor != null && cursor != root) {
            String parentSignature = signatureExtractor.signatureOf(cursor);
            if (parentSignature != null) {
                return parentSignature + "->" + signature;
            }
            cursor = cursor.getParentNode().orElse(null);
        }
        return null;
    }

    private String buildControlSkeleton(Node node) {
        List<String> skeleton = new ArrayList<>();
        for (Statement statement : node.findAll(Statement.class)) {
            if (statement instanceof BlockStmt) {
                continue;
            }
            String tag = controlTag(statement);
            if (tag != null) {
                skeleton.add(tag);
            }
        }
        if (skeleton.isEmpty()) {
            return "";
        }
        return String.join(">", skeleton.subList(0, Math.min(6, skeleton.size()))).toUpperCase(Locale.ROOT);
    }

    private String controlTag(Statement statement) {
        if (statement instanceof IfStmt) {
            return "IF";
        }
        if (statement instanceof ForStmt) {
            return "FOR";
        }
        if (statement instanceof ForEachStmt) {
            return "FOREACH";
        }
        if (statement instanceof WhileStmt) {
            return "WHILE";
        }
        if (statement instanceof DoStmt) {
            return "DO_WHILE";
        }
        if (statement instanceof SwitchStmt) {
            return "SWITCH";
        }
        if (statement instanceof TryStmt) {
            return "TRY";
        }
        return null;
    }

    private void mergeSignature(Map<String, Integer> signatureCounts, String signature) {
        signatureCounts.merge(signature, 1, Integer::sum);
    }

    private record StructuralProfile(
            int totalNodes,
            Map<String, Integer> signatureCounts
    ) {
    }
}
