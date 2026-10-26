/**
 * 生成聊天室 id
 *
 * 后端参数是 int，所以必须返回数字；范围控制在 int32 以内。
 */
export function createMemoryId() {
  return Math.floor(Math.random() * 2_000_000_000) + 1
}

/** 生成用于前端渲染的稳定 key */
export function createMessageId() {
  return `m_${Date.now()}_${Math.random().toString(16).slice(2, 8)}`
}
