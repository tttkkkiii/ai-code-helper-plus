<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import ChatRoom from '@/components/ChatRoom.vue'
import SessionSidebar from '@/components/SessionSidebar.vue'
import { createChatStream, createSession, deleteSession, listMessages, listSessions } from '@/api/chat'
import { createMessageId } from '@/utils/id'
import assistantSvg from '@/assets/assistant.svg'

/** 左侧会话列表 */
const sessions = ref([])
/** 当前会话 id；null 表示"还没创建的新对话" */
const currentSessionId = ref(null)
/** 当前会话的消息 */
const messages = ref([])
/** 是否正在流式输出 */
const streaming = ref(false)
/** 聊天区组件引用，用于"发送后强制滚到底部" */
const chatRoomRef = ref(null)

let currentStream = null

/* =========================================================
   打字机插值（按时间匀速吐字）
   ---------------------------------------------------------
   模型的 token 是"突发式"到达的，如果收到多少就立刻画多少，
   视觉上就是"猛吐一阵 → 停顿 → 再猛吐"。

   这里把"网络到达"和"屏幕显示"拆开：
     - onChunk 只往 target 里堆，不动界面
     - requestAnimationFrame 每帧按【固定字/秒】往界面上吐
     - 只有落后太多时才加速追赶，正常情况下速度恒定
   ========================================================= */

/** 基准速度：每秒显示多少字（模型一般 20~30 字/秒，这里略低于它，保证不会吐空） */
const DISPLAY_CHARS_PER_SECOND = 24
/** 积压超过这个数就加速追赶，避免被网络越甩越远 */
const MAX_LAG_CHARS = 40
/** 流结束后，剩余字数按这个系数加速吐完（越快收尾越好） */
const DRAIN_FACTOR = 4
/** 单帧最大时间步长，防止切到后台再回来时一次性吐一大段 */
const MAX_FRAME_MS = 100

let typing = null // { message, target, done, ok, lastTime }
let typingRaf = null
/** 小数形式的"待显示字数"，累积到 1 才真正输出一个字符 */
let typingCarry = 0

function startTyping(message) {
  stopTyping()
  typing = {
    message,
    target: '',
    done: false,
    ok: false,
    lastTime: 0
  }
  typingCarry = 0
  typingRaf = requestAnimationFrame(stepTyping)
}

function stepTyping(now) {
  typingRaf = null
  if (!typing) return

  if (!typing.lastTime) {
    typing.lastTime = now
  }
  const dt = Math.min(now - typing.lastTime, MAX_FRAME_MS)
  typing.lastTime = now

  const { message, target, done } = typing
  const shown = message.content.length
  const backlog = target.length - shown

  // 已经全部显示完 + 流也结束了 → 收尾
  if (done && backlog === 0) {
    const { ok } = typing
    typing = null
    typingCarry = 0
    finishStream(message, ok)
    return
  }

  let speed = DISPLAY_CHARS_PER_SECOND
  if (done) {
    // 收尾阶段：剩余越多吐得越快
    speed = Math.max(speed, backlog * DRAIN_FACTOR)
  } else if (backlog > MAX_LAG_CHARS) {
    // 落后太多：按比例加速，但要平滑，不能突然跳变
    speed = speed * (backlog / MAX_LAG_CHARS)
  }

  if (backlog > 0 && speed > 0) {
    typingCarry += (speed * dt) / 1000
    const step = Math.floor(typingCarry)
    if (step > 0) {
      typingCarry -= step
      message.content = target.slice(0, shown + Math.min(step, backlog))
    }
  }

  typingRaf = requestAnimationFrame(stepTyping)
}

function stopTyping() {
  if (typingRaf) {
    cancelAnimationFrame(typingRaf)
    typingRaf = null
  }
  typing = null
  typingCarry = 0
}

/** 流结束后（且缓冲区吐完）统一收尾 */
function finishStream(message, ok) {
  message.streaming = false
  streaming.value = false

  if (!ok) {
    message.error = true
    if (!message.content) {
      message.content =
        '连接后端失败了。请确认：\n1. 后端已在 8081 端口启动\n2. 接口路径与 src/config.js 中的 CHAT_PATH 一致'
    }
  }

  currentStream = null

  // 刷新列表：标题（首次提问后自动生成）和"刚刚"时间会同步更新
  refreshSessions()
}

const currentTitle = computed(() => {
  const hit = sessions.value.find((item) => item.id === currentSessionId.value)
  return hit ? hit.title : '新对话'
})

/** 拉取会话列表 */
async function refreshSessions() {
  try {
    sessions.value = await listSessions()
  } catch (error) {
    console.error('加载会话列表失败:', error)
  }
}

onMounted(refreshSessions)

/** 点「添加新对话」：只是清空界面，真正建会话等用户发出第一句话 */
function handleNewSession() {
  stopTyping()
  currentStream?.close()
  currentStream = null
  streaming.value = false
  currentSessionId.value = null
  messages.value = []
}

/** 点历史会话：加载它的消息 */
async function handleSelect(id) {
  if (id === currentSessionId.value || streaming.value) return

  currentStream?.close()
  currentStream = null
  currentSessionId.value = id

  try {
    const rows = await listMessages(id)
    messages.value = rows.map((row) => ({
      id: row.id,
      role: row.role === 'USER' ? 'user' : 'ai',
      content: row.content
    }))
  } catch (error) {
    console.error('加载消息失败:', error)
    messages.value = []
  }
}

/** 删除会话 */
async function handleRemove(id) {
  if (!window.confirm('确定删除这个对话吗？删除后不可恢复。')) return

  try {
    await deleteSession(id)
  } catch (error) {
    console.error('删除会话失败:', error)
    return
  }

  // 删的正是当前会话：中断流、清空界面，回到"新对话"
  if (currentSessionId.value === id) {
    stopTyping()
    currentStream?.close()
    currentStream = null
    streaming.value = false
    currentSessionId.value = null
    messages.value = []
  }

  await refreshSessions()
}

/** 发送消息 */
async function handleSend(text) {
  const content = String(text || '').trim()
  if (!content || streaming.value) return

  // 懒创建：新对话的第一句话才真正建会话
  if (currentSessionId.value == null) {
    try {
      const session = await createSession()
      currentSessionId.value = session.id
      await refreshSessions()
    } catch (error) {
      console.error('创建会话失败:', error)
      return
    }
  }

  messages.value.push({
    id: createMessageId(),
    role: 'user',
    content
  })

  // reactive 包装：流式追加内容才能触发视图更新
  const aiMessage = reactive({
    id: createMessageId(),
    role: 'ai',
    content: '',
    streaming: true,
    error: false
  })
  messages.value.push(aiMessage)
  streaming.value = true

  // 用户主动发消息时，无视"用户已上滚"的状态，强制回到底部
  chatRoomRef.value?.scrollToBottom(true)

  // 启动打字机：网络收到的内容先堆进 target，再由 rAF 匀速吐到界面上
  startTyping(aiMessage)

  currentStream = createChatStream({
    memoryId: currentSessionId.value,
    message: content,

    onChunk(chunk) {
      if (typing) typing.target += chunk
    },

    onFinish(ok) {
      if (!typing) {
        // 兜底：没有进行中的打字机，直接收尾
        finishStream(aiMessage, ok)
        return
      }
      // 标记流已结束，等它把 target 里剩下的字吐完再收尾
      typing.done = true
      typing.ok = ok
      if (!typingRaf) {
        typingRaf = requestAnimationFrame(stepTyping)
      }
    }
  })
}

onBeforeUnmount(() => {
  stopTyping()
  currentStream?.close()
})
</script>

<template>
  <div class="app-shell">
    <!-- 二次元角色背景 -->
    <div class="bg-character">
      <img :src="assistantSvg" alt="" />
    </div>

    <div class="layout">
      <SessionSidebar
        :sessions="sessions"
        :current-session-id="currentSessionId"
        @new-session="handleNewSession"
        @select="handleSelect"
        @remove="handleRemove"
      />

      <ChatRoom
        ref="chatRoomRef"
        :messages="messages"
        :streaming="streaming"
        :title="currentTitle"
        @send="handleSend"
      />
    </div>
  </div>
</template>
