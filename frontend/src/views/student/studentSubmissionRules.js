export function normalizeStudentAssignmentLanguage(language) {
  const normalized = String(language || '').trim().toUpperCase()
  return normalized === 'C' ? 'C' : 'JAVA'
}

export function buildStudentSubmissionPolicy(assignment = {}) {
  const language = normalizeStudentAssignmentLanguage(assignment.language)
  const allowResubmit = Boolean(assignment.allowResubmit)

  if (language === 'C') {
    return {
      language,
      supportsTextMode: false,
      effectiveFileLimit: 1,
      fileAccept: '.c,.zip',
      uploadTitle: '上传 C 源码或工程包',
      uploadTip: '单文件作业请提交 1 个 .c；多文件工程请提交 1 个 .zip，压缩包内仅保留 .c/.h。',
      uploadTriggerText: '拖拽到这里，或点击选择 1 个 `.c` 文件 / `.zip` 工程包',
      maxFilesSummary: '当前仅支持 1 个文件（单 .c 或单 .zip）',
      ruleSummary: allowResubmit ? '当前提交会直接覆盖旧内容' : '本作业仅允许提交一次',
      detailGuidance: 'C 作业支持单个 .c 文件，或 1 个包含 .c/.h 的 .zip 工程包。'
    }
  }

  return {
    language,
    supportsTextMode: true,
    effectiveFileLimit: Math.max(1, Number(assignment.maxFiles) || 1),
    fileAccept: '.java',
    uploadTitle: '上传 Java 文件',
    uploadTip: '只接收 .java 文件，新文件集合会覆盖当前提交。',
    uploadTriggerText: '拖拽到这里，或点击选择 `.java` 文件',
    maxFilesSummary: `最多 ${Math.max(1, Number(assignment.maxFiles) || 1)} 个文件`,
    ruleSummary: allowResubmit ? '当前提交会直接覆盖旧内容' : '本作业仅允许提交一次',
    detailGuidance: '你可以上传一个或多个 .java 文件，也可以直接在页面内编辑文本代码。'
  }
}
