/**
 * 规范化资源链接（补齐协议）
 */
export const normalizeAssetUrl = (url?: string): string => {
  if (!url) {
    return ''
  }
  const trimmed = url.trim()
  if (!trimmed) {
    return ''
  }
  if (/^(https?:)?\/\//i.test(trimmed) || /^data:/i.test(trimmed)) {
    return trimmed
  }
  if (trimmed.startsWith('/')) {
    return trimmed
  }
  return `https://${trimmed}`
}
