export function mergeAssignmentRosterContext(summary = {}, detail = {}) {
  const hasSummaryClassIds = Array.isArray(summary.classIds) && summary.classIds.length > 0
  const hasSummaryClassNames = Array.isArray(summary.classNames) && summary.classNames.length > 0
  const detailClasses = Array.isArray(detail.classes) ? detail.classes : []

  return {
    ...summary,
    classIds: hasSummaryClassIds
      ? summary.classIds
      : detailClasses.map((item) => Number(item.classId)).filter(Boolean),
    classNames: hasSummaryClassNames
      ? summary.classNames
      : detailClasses.map((item) => item.className).filter(Boolean),
    classNamesText: summary.classNamesText || detail.classNamesText || ''
  }
}
