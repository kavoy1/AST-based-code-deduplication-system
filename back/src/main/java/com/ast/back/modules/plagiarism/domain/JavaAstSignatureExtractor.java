package com.ast.back.modules.plagiarism.domain;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LiteralStringValueExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TextBlockLiteralExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.TypeParameter;

import java.util.HashMap;
import java.util.Map;

public class JavaAstSignatureExtractor {

    public AstSignatureProfile extract(String source) {
        CompilationUnit compilationUnit;
        try {
            compilationUnit = StaticJavaParser.parse(source);
        } catch (ParseProblemException ex) {
            throw new IllegalArgumentException("Java source parse failed", ex);
        }

        normalize(compilationUnit);

        Map<String, Integer> counts = new HashMap<>();
        int totalNodes = 0;
        for (Node node : compilationUnit.findAll(Node.class)) {
            String signature = signatureOf(node);
            if (signature != null) {
                counts.merge(signature, 1, Integer::sum);
                totalNodes++;
            }
        }
        return new AstSignatureProfile(totalNodes, counts);
    }

    void normalize(CompilationUnit compilationUnit) {
        compilationUnit.findAll(SimpleName.class).forEach(name -> name.setIdentifier("ID"));
        compilationUnit.findAll(ClassOrInterfaceType.class).forEach(type -> type.setName("TYPE"));
        compilationUnit.findAll(PrimitiveType.class).forEach(type -> type.setType(PrimitiveType.Primitive.INT));
        compilationUnit.findAll(TypeParameter.class).forEach(type -> type.setName("TYPE"));
        compilationUnit.findAll(StringLiteralExpr.class).forEach(expr -> expr.setString("STR"));
        compilationUnit.findAll(TextBlockLiteralExpr.class).forEach(expr -> expr.setValue("STR"));
        compilationUnit.findAll(CharLiteralExpr.class).forEach(expr -> expr.setValue("C"));
        compilationUnit.findAll(IntegerLiteralExpr.class).forEach(expr -> expr.setValue("0"));
        compilationUnit.findAll(LongLiteralExpr.class).forEach(expr -> expr.setValue("0L"));
        compilationUnit.findAll(DoubleLiteralExpr.class).forEach(expr -> expr.setValue("0.0"));
        compilationUnit.findAll(BooleanLiteralExpr.class).forEach(expr -> expr.setValue(true));
        compilationUnit.findAll(VariableDeclarator.class).forEach(var -> var.setName("ID"));
        compilationUnit.findAll(Parameter.class).forEach(var -> var.setName("ID"));
        compilationUnit.findAll(MethodDeclaration.class).forEach(method -> method.setName("ID"));
        compilationUnit.findAll(ClassOrInterfaceDeclaration.class).forEach(type -> type.setName("ID"));
        compilationUnit.findAll(EnumDeclaration.class).forEach(type -> type.setName("ID"));
        compilationUnit.findAll(FieldDeclaration.class).forEach(field ->
                field.getVariables().forEach(variable -> variable.setName("ID")));
        compilationUnit.findAll(NameExpr.class).forEach(expr -> expr.setName("ID"));
    }

    String signatureOf(Node node) {
        if (node instanceof BinaryExpr expr) {
            return "BinaryExpr:" + expr.getOperator().name();
        }
        if (node instanceof UnaryExpr expr) {
            return "UnaryExpr:" + expr.getOperator().name();
        }
        if (node instanceof AssignExpr expr) {
            return "Assign:" + expr.getOperator().name();
        }
        if (node instanceof MethodCallExpr expr) {
            return "Call:arity=" + expr.getArguments().size();
        }
        if (node instanceof ObjectCreationExpr expr) {
            return "Call:arity=" + expr.getArguments().size();
        }
        if (node instanceof IfStmt stmt) {
            return "If:else=" + (stmt.getElseStmt().isPresent() ? 1 : 0);
        }
        if (node instanceof ForStmt stmt) {
            String mask = (stmt.getInitialization().isEmpty() ? "0" : "1")
                    + (stmt.getCompare().isPresent() ? "1" : "0")
                    + (stmt.getUpdate().isEmpty() ? "0" : "1");
            return "For:mask=" + mask;
        }
        if (node instanceof ForEachStmt) {
            return "ForEach";
        }
        if (node instanceof WhileStmt) {
            return "While";
        }
        if (node instanceof DoStmt) {
            return "DoWhile";
        }
        if (node instanceof ReturnStmt) {
            return "Return";
        }
        if (node instanceof BreakStmt) {
            return "Break";
        }
        if (node instanceof ContinueStmt) {
            return "Continue";
        }
        if (node instanceof ThrowStmt) {
            return "Throw";
        }
        if (node instanceof TryStmt) {
            return "TryCatch";
        }
        if (node instanceof SwitchStmt) {
            return "Switch";
        }
        if (node instanceof IntegerLiteralExpr || node instanceof LongLiteralExpr || node instanceof DoubleLiteralExpr) {
            return "Literal:NUM";
        }
        if (node instanceof StringLiteralExpr || node instanceof TextBlockLiteralExpr) {
            return "Literal:STR";
        }
        if (node instanceof BooleanLiteralExpr) {
            return "Literal:BOOL";
        }
        if (node instanceof CharLiteralExpr) {
            return "Literal:CHAR";
        }
        if (node instanceof NullLiteralExpr) {
            return "Literal:NULL";
        }
        return null;
    }
}
