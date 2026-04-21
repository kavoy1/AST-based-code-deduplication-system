export const supportedTeacherAssignmentLanguages = ['JAVA', 'C']

export const teacherAssignmentLanguageHint =
  '当前系统支持 JAVA 与 C 解析和查重；C 作业建议按单个 .c 文件或 .zip 工程包组织提交。'

export function normalizeTeacherAssignmentLanguage(language) {
  return supportedTeacherAssignmentLanguages.includes(language) ? language : 'JAVA'
}
