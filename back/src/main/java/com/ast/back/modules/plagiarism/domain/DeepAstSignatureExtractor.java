package com.ast.back.modules.plagiarism.domain;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeepAstSignatureExtractor {

    private final JavaAstSignatureExtractor baseExtractor = new JavaAstSignatureExtractor();

    public DeepAstProfile extract(String source) {
        CompilationUnit compilationUnit;
        try {
            compilationUnit = StaticJavaParser.parse(source);
        } catch (ParseProblemException ex) {
            throw new IllegalArgumentException("Java source parse failed", ex);
        }

        baseExtractor.normalize(compilationUnit);

        Map<String, Integer> coarseCounts = new HashMap<>();
        Map<String, Integer> deepCounts = new HashMap<>();
        List<DeepAstMethodProfile> methodProfiles = new ArrayList<>();

        int coarseTotalNodes = 0;
        int deepTotalNodes = 0;

        for (Node node : compilationUnit.findAll(Node.class)) {
            String baseSignature = baseExtractor.signatureOf(node);
            if (baseSignature == null) {
                continue;
            }
            coarseCounts.merge(baseSignature, 1, Integer::sum);
            coarseTotalNodes++;

            int depthBucket = toDepthBucket(node);
            String deepSignature = baseSignature + "@depth=" + depthBucket;
            deepCounts.merge(deepSignature, 1, Integer::sum);
            deepTotalNodes++;

            String pathSignature = buildParentPathSignature(node, baseSignature);
            if (pathSignature != null) {
                deepCounts.merge(pathSignature, 1, Integer::sum);
                deepTotalNodes++;
            }
        }

        int methodIndex = 0;
        for (MethodDeclaration method : compilationUnit.findAll(MethodDeclaration.class)) {
            Map<String, Integer> methodCounts = new HashMap<>();
            if (method.getBody().isEmpty()) {
                continue;
            }
            for (Node node : method.getBody().get().findAll(Node.class)) {
                String baseSignature = baseExtractor.signatureOf(node);
                if (baseSignature == null) {
                    continue;
                }
                String deepSignature = baseSignature + "@depth=" + toDepthBucket(node);
                methodCounts.merge(deepSignature, 1, Integer::sum);
                String pathSignature = buildParentPathSignature(node, baseSignature);
                if (pathSignature != null) {
                    methodCounts.merge(pathSignature, 1, Integer::sum);
                }
            }
            if (!methodCounts.isEmpty()) {
                String methodKey = "方法#" + (++methodIndex) + "(参数" + method.getParameters().size() + ")";
                deepCounts.merge("Method:" + methodKey, 1, Integer::sum);
                deepTotalNodes++;
                methodProfiles.add(new DeepAstMethodProfile(
                        methodKey,
                        methodCounts.values().stream().mapToInt(Integer::intValue).sum(),
                        methodCounts
                ));
            }
        }

        methodProfiles.sort(Comparator.comparing(DeepAstMethodProfile::methodKey));

        return new DeepAstProfile(
                coarseTotalNodes,
                coarseCounts,
                deepTotalNodes,
                deepCounts,
                methodProfiles
        );
    }

    private int toDepthBucket(Node node) {
        int depth = 0;
        Node cursor = node.getParentNode().orElse(null);
        while (cursor != null) {
            depth++;
            cursor = cursor.getParentNode().orElse(null);
        }
        if (depth <= 3) {
            return 1;
        }
        if (depth <= 6) {
            return 2;
        }
        return 3;
    }

    private String buildParentPathSignature(Node node, String baseSignature) {
        Node parent = node.getParentNode().orElse(null);
        while (parent != null) {
            String parentSignature = baseExtractor.signatureOf(parent);
            if (parentSignature != null) {
                return "Path:" + parentSignature + "->" + baseSignature;
            }
            parent = parent.getParentNode().orElse(null);
        }
        return null;
    }
}
