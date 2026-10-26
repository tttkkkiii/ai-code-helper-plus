<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { renderMarkdown } from '@/utils/markdown'
import assistantSvg from '@/assets/assistant.svg'

const props = defineProps({
  message: {
    type: Object,
    required: true
  }
})

const html = computed(() => renderMarkdown(props.message.content))

const bubbleRef = ref(null)

/** 上一次渲染时的字符数，用来算"这次新加了几个字" */
let lastLength = 0

/**
 * 给"新增加的几个字符"套一个淡入动画。
 *
 * 为什么可以这么做：
 * 打字机的刷新间隔约 42ms（每秒 24 字），把动画时长设成 45ms，
 * 字符在同一个渲染周期内就从淡到实完成过渡；
 * 下一次整体重绘时它已经是普通文本了，不会出现"动画被打断反复闪"的问题。
 */
function fadeInNewChars(count) {
  const root = bubbleRef.value
  if (!root || count <= 0) return

  // 收集所有文本节点（DOM 顺序）
  const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT)
  const textNodes = []
  while (walker.nextNode()) {
    if (walker.currentNode.nodeValue) textNodes.push(walker.currentNode)
  }

  let remaining = count

  // 从后往前，把最新写入的字符包进 span
  for (let i = textNodes.length - 1; i >= 0 && remaining > 0; i--) {
    const node = textNodes[i]
    const text = node.nodeValue || ''
    const take = Math.min(remaining, text.length)
    const splitAt = text.length - take

    const parent = node.parentNode
    if (!parent) continue

    node.nodeValue = text.slice(0, splitAt)

    const span = document.createElement('span')
    span.className = 'char-fade'
    span.textContent = text.slice(splitAt)
    parent.insertBefore(span, node.nextSibling)

    remaining -= take
  }
}

onMounted(() => {
  // 历史消息直接整段渲染，不做动画
  lastLength = (props.message.content || '').length
})

watch(
  () => props.message.content,
  async (value) => {
    const current = (value || '').length
    const added = current - lastLength
    lastLength = current

    if (added <= 0) return

    await nextTick()
    fadeInNewChars(added)
  }
)
</script>

<template>
  <div class="msg" :class="[message.role, { error: message.error }]">
    <div class="avatar" :class="message.role">
      <img v-if="message.role === 'ai'" :src="assistantSvg" alt="AI" />
      <span v-else>我</span>
    </div>

    <div ref="bubbleRef" class="bubble">
      <template v-if="message.role === 'ai'">
        <!-- 还没收到任何内容：等待动画 -->
        <span v-if="!message.content && message.streaming" class="typing">
          <i></i><i></i><i></i>
        </span>

        <span v-else v-html="html"></span>
      </template>

      <template v-else>{{ message.content }}</template>
    </div>
  </div>
</template>
