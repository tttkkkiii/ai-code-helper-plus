/**
 * 相对时间：刚刚 / 12分钟 / 3小时 / 6天 / 具体日期
 */
export function timeAgo(value) {
  if (!value) return ''

  // 后端返回 "2026-10-19T01:35:12.456"（无时区），JS 会按本地时间解析
  const time = new Date(value).getTime()
  if (Number.isNaN(time)) return ''

  const diff = (Date.now() - time) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)}分钟`
  if (diff < 86400) return `${Math.floor(diff / 3600)}小时`
  if (diff < 86400 * 7) return `${Math.floor(diff / 86400)}天`

  return new Date(value).toLocaleDateString('zh-CN')
}
