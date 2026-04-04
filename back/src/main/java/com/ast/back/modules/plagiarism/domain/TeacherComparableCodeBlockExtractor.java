package com.ast.back.modules.plagiarism.domain;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
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
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.TypeParameter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TeacherComparableCodeBlockExtractor {

    private static final int MIN_NORMALIZED_LENGTH = 24;
    private static final int MIN_STATEMENT_COUNT = 2;

    public List<TeacherComparableCodeBlock> extract(String fileName, String source, int sourceStartLineInFullCode) {
        if (source == null || source.isBlank()) {
            return List.of();
        }

        Node root;
        try {
            root = StaticJavaParser.parse(source);
        } catch (ParseProblemException ex) {
            return List.of();
        }

        List<TeacherComparableCodeBlock> blocks = new ArrayList<>();
        collectBlocks(root.findAll(MethodDeclaration.class), "METHOD", fileName, sourceStartLineInFullCode, blocks);
        collectBlocks(root.findAll(ForStmt.class), "FOR", fileName, sourceStartLineInFullCode, blocks);
        collectBlocks(root.findAll(ForEachStmt.class), "FOREACH", fileName, sourceStartLineInFullCode, blocks);
        collectBlocks(root.findAll(WhileStmt.class), "WHILE", fileName, sourceStartLineInFullCode, blocks);
        collectBlocks(root.findAll(DoStmt.class), "DO_WHILE", fileName, sourceStartLineInFullCode, blocks);
        collectBlocks(root.findAll(TryStmt.class), "TRY", fileName, sourceStartLineInFullCode, blocks);
        collectBlocks(root.findAll(SwitchStmt.class), "SWITCH", fileName, sourceStartLineInFullCode, blocks);
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
            List<TeacherComparableCodeBlock> output
    ) {
        for (Node node : nodes) {
            TeacherComparableCodeBlock block = toBlock(kind, fileName, node, sourceStartLineInFullCode);
            if (block != null) {
                output.add(block);
            }
        }
    }

    private TeacherComparableCodeBlock toBlock(String kind, String fileName, Node node, int sourceStartLineInFullCode) {
        if (node.getRange().isEmpty()) {
            return null;
        }

        int localStartLine = node.getRange().get().begin.line;
        int localEndLine = node.getRange().get().end.line;
        if (localStartLine >= localEndLine) {
            return null;
        }

        int statementCount = countStatements(node);
        if (statementCount < MIN_STATEMENT_COUNT) {
            return null;
        }

        String normalizedSnippet = normalizeSnippet(node);
        if (normalizedSnippet == null || normalizedSnippet.length() < MIN_NORMALIZED_LENGTH) {
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
                statementCount
        );
    }

    private int countStatements(Node node) {
        return (int) node.findAll(Statement.class).stream()
                .filter(statement -> !(statement instanceof BlockStmt))
                .count();
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

    private String buildSummary(String kind, Node node) {
        if (node instanceof MethodDeclaration methodDeclaration) {
            return methodDeclaration.getNameAsString() + "() 方法逻辑";
        }
        return switch (kind) {
            case "FOR" -> "for 循环逻辑";
            case "FOREACH" -> "foreach 循环逻辑";
            case "WHILE" -> "while 循环逻辑";
            case "DO_WHILE" -> "do-while 循环逻辑";
            case "TRY" -> "异常处理逻辑";
            case "SWITCH" -> "switch 分支逻辑";
            default -> "相似代码片段";
        };
    }
}
