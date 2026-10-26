<script setup>
import { timeAgo } from '@/utils/time'
import assistantSvg from '@/assets/assistant.svg'

defineProps({
  sessions: {
    type: Array,
    default: () => []
  },
  currentSessionId: {
    type: [Number, String],
    default: null
  }
})

defineEmits(['new-session', 'select', 'remove'])
</script>

<template>
  <aside class="sidebar">
    <div class="sidebar-brand">
      <img :src="assistantSvg" alt="" />
      <h1>AI 编程小助手</h1>
    </div>

    <button class="new-chat-btn" type="button" @click="$emit('new-session')">
      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor"
           stroke-width="2.4" stroke-linecap="round">
        <path d="M12 5v14M5 12h14" />
      </svg>
      添加新对话
    </button>

    <div class="session-list">
      <div
        v-for="item in sessions"
        :key="item.id"
        class="session-item"
        :class="{ active: item.id === currentSessionId }"
        role="button"
        tabindex="0"
        @click="$emit('select', item.id)"
        @keydown.enter="$emit('select', item.id)"
      >
        <div class="session-main">
          <span class="session-title">{{ item.title }}</span>
          <span class="session-time">{{ timeAgo(item.updatedAt) }}</span>
        </div>

        <button
          class="session-del"
          type="button"
          title="删除这个对话"
          @click.stop="$emit('remove', item.id)"
        >
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor"
               stroke-width="2.6" stroke-linecap="round">
            <path d="M6 6l12 12M18 6L6 18" />
          </svg>
        </button>
      </div>

      <p v-if="!sessions.length" class="sidebar-empty">
        还没有对话记录<br />
        在右侧输入问题就会自动创建
      </p>
    </div>
  </aside>
</template>
