export function normalizeAvatarSrc(value) {
  if (typeof value !== 'string') return undefined

  const trimmed = value.trim()
  if (!trimmed) return undefined

  const invalidValues = ['null', 'undefined', 'none', 'n/a', 'nan']
  if (invalidValues.includes(trimmed.toLowerCase())) return undefined

  return trimmed
}