package com.ast.back.modules.plagiarism.domain;

import com.ast.back.modules.submission.domain.CProjectSourceIndex;

import java.util.List;

public interface CProjectAstDumper {

    List<CClangAstDump> dump(CProjectSourceIndex sourceIndex);
}
