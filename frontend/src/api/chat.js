import http from './http'
import { CHAT_PATH } from '@/config'

// ========== 会话相关的普通 REST 接口（走 Axios）==========

/** 新建会话，返回 { id, title } */
export function createSession() {
  return http.post('/chat/session')
}

/** 会话列表（按最后活跃时间倒序） */
export function listSessions() {
  return http.get('/chat/sessions')
}

/** 某个会话的历史消息 */
export function listMessages(sessionId) {
  return http.get(`/chat/session/${sessionId}`)
}

/** 删除会话（连同它的消息） */
export function deleteSession(sessionId) {
  return http.delete(`/chat/session/${sessionId}`)
}

/**
 * 拼接聊天接口地址
 *
 * 后端签名： @GetMapping("/chat") chat(int memoryId, String message)
 * 浏览器 EventSource 只能发 GET，正好匹配。
 */
export function buildChatUrl({ memoryId, message }) {
  const base = (http.defaults.baseURL || '').replace(/\/+$/, '')
  const query = new URLSearchParams({
    memoryId: String(memoryId),
    message
  })
  return `${base}${CHAT_PATH}?${query.toString()}`
}

/**
 * 建立一条 SSE 聊天流
 *
 * @param {object}   options
 * @param {number}   options.memoryId 会话 id（后端是 int，必须是数字）
 * @param {string}   options.message  用户输入
 * @param {Function} options.onChunk  收到一个片段
 * @param {Function} options.onFinish 流结束，参数 true 表示收到过内容
 * @returns {{ close: Function }}
 */
export function createChatStream({ memoryId, message, onChunk, onFinish }) {
  const url = buildChatUrl({ memoryId, message })
  const source = new EventSource(url)

  let settled = false
  let received = false

  const finish = (ok) => {
    if (settled) return
    settled = true
    source.close()
    onFinish?.(ok)
  }

  source.onmessage = (event) => {
    if (!event.data) return

    // 后端把每块包成了 {"c":"原文"}，先解出来；解不出来就当纯文本（兼容旧后端）
    let piece = event.data
    try {
      piece = JSON.parse(event.data).c ?? ''
    } catch {
      /* 保持原样 */
    }
    if (!piece) return

    received = true
    onChunk?.(piece)
  }

  // 后端 Flux 正常结束时会关闭连接，EventSource 会把它当成异常并自动重连。
  // 这里主动 close 掉，并按“是否收到过内容”判定成败。
  source.onerror = () => finish(received)

  return {
    close() {
      finish(false)
    }
  }
}
