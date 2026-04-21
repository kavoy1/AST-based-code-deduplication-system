package com.ast.back.modules.plagiarism.domain;

import com.ast.back.modules.submission.domain.CProjectSourceIndex;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class CDeepAstSignatureExtractor {

    private final CProjectAstDumper astDumper;
    private final CAstSignatureExtractor baseExtractor;

    public CDeepAstSignatureExtractor() {
        this(new CClangProjectAstDumper(), new ObjectMapper());
    }

    public CDeepAstSignatureExtractor(CProjectAstDumper astDumper) {
        this(astDumper, new ObjectMapper());
    }

    CDeepAstSignatureExtractor(CProjectAstDumper astDumper, ObjectMapper objectMapper) {
        this.astDumper = astDumper;
        this.baseExtractor = new CAstSignatureExtractor(astDumper, objectMapper);
    }

    public DeepAstProfile extract(CProjectSourceIndex sourceIndex) {
        List<CClangAstDump> dumps = astDumper.dump(sourceIndex);
        var coarseCounts = new HashMap<String, Integer>();
        var deepCounts = new HashMap<String, Integer>();
        var methodProfiles = new ArrayList<DeepAstMethodProfile>();
        int coarseTotal = 0;
        int deepTotal = 0;

        for (CClangAstDump dump : dumps) {
            DeepAstProfile profile = baseExtractor.collectDeepProfile(dump);
            coarseTotal += profile.coarseTotalNodes();
            deepTotal += profile.deepTotalNodes();
            profile.coarseSignatureCounts().forEach((key, value) -> coarseCounts.merge(key, value, Integer::sum));
            profile.deepSignatureCounts().forEach((key, value) -> deepCounts.merge(key, value, Integer::sum));
            methodProfiles.addAll(profile.methodProfiles());
        }

        methodProfiles.sort(Comparator.comparing(DeepAstMethodProfile::methodKey));
        return new DeepAstProfile(coarseTotal, coarseCounts, deepTotal, deepCounts, List.copyOf(methodProfiles));
    }
}
