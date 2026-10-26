<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import MessageItem from './MessageItem.vue'
import ChatInput from './ChatInput.vue'
import assistantSvg from '@/assets/assistant.svg'

const props = defineProps({
  messages: {
    type: Array,
    default: () => []
  },
  streaming: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: '新对话'
  }
})

const emit = defineEmits(['send'])

const suggestions = [
  '帮我制定一份 Java 后端学习路线',
  '程序员简历该怎么优化？',
  'Java 集合常见面试题有哪些？',
  '项目经历怎么写才加分？'
]

const bodyRef = ref(null)

const isEmpty = computed(() => props.messages.length === 0)

// 是否"跟随到底部"：用户一旦往上滚就停止自动滚动，避免内容不断把视口拽回去
const autoScroll = ref(true)

function handleScroll() {
  const el = bodyRef.value
  if (!el) return
  const distance = el.scrollHeight - el.scrollTop - el.clientHeight
  autoScroll.value = distance < 48
}

// 滚动节流：同一轮事件循环内只滚一次，避免每个 chunk 都触发一次 layout
let scrollScheduled = false

function scrollToBottom(force = false) {
  if (!force && !autoScroll.value) return
  if (scrollScheduled) return
  scrollScheduled = true
  nextTick(() => {
    scrollScheduled = false
    const el = bodyRef.value
    if (!el) return
    el.scrollTop = el.scrollHeight
    autoScroll.value = true
  })
}

defineExpose({ scrollToBottom })

// 最后一条消息的内容变化（流式追加）也要跟着滚到底
const lastContent = computed(() => {
  const last = props.messages[props.messages.length - 1]
  return last ? last.content : ''
})

watch(() => props.messages.length, () => scrollToBottom())

// 切换会话（messages 整个数组被替换）时强制回到底部
watch(
  () => props.messages,
  () => {
    autoScroll.value = true
    scrollToBottom(true)
  }
)

watch(lastContent, () => scrollToBottom())
</script>

<template>
  <section class="chat-card">
    <header class="chat-header">
      <div class="brand">
        <div class="brand-logo">
          <img :src="assistantSvg" alt="" />
        </div>
        <div class="brand-text">
          <h1 class="brand-title">{{ title }}</h1>
          <p class="brand-sub">编程学习路线 · 项目建议 · 求职面试</p>
        </div>
      </div>
    </header>

    <div ref="bodyRef" class="chat-body" @scroll.passive="handleScroll">
      <div v-if="isEmpty" class="empty">
        <img class="empty-avatar" :src="assistantSvg" alt="AI 小助手" />
        <h2 class="empty-title">你好呀，我是你的编程小助手</h2>
        <p class="empty-desc">
          可以问我编程学习路线、项目经历怎么准备、简历怎么优化，或者直接来几道高频面试题。选个话题开始吧 👇
        </p>
        <div class="suggestions">
          <button
            v-for="item in suggestions"
            :key="item"
            class="suggestion"
            type="button"
            @click="emit('send', item)"
          >
            {{ item }}
          </button>
        </div>
      </div>

      <MessageItem v-for="msg in messages" :key="msg.id" :message="msg" />
    </div>

    <footer class="chat-footer">
      <ChatInput :disabled="streaming" @send="emit('send', $event)" />
      <p class="footer-hint">Enter 发送 · Shift + Enter 换行 · 内容由 AI 生成，仅供参考</p>
    </footer>
  </section>
</template>
