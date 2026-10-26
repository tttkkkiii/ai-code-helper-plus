<script setup>
import { nextTick, ref, watch } from 'vue'

const props = defineProps({
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['send'])

const text = ref('')
const textareaRef = ref(null)

function resize() {
  const el = textareaRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = `${Math.min(el.scrollHeight, 140)}px`
}

function submit() {
  const value = text.value.trim()
  if (!value || props.disabled) return
  emit('send', value)
  text.value = ''
  nextTick(resize)
}

function onKeydown(event) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    submit()
  }
}

watch(text, () => nextTick(resize))
</script>

<template>
  <div class="input-wrap">
    <textarea
      ref="textareaRef"
      v-model="text"
      rows="1"
      placeholder="问问编程学习、项目经验、简历或面试题…"
      @keydown="onKeydown"
    ></textarea>

    <button
      class="send-btn"
      type="button"
      :disabled="disabled || !text.trim()"
      title="发送"
      @click="submit"
    >
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
           stroke-linecap="round" stroke-linejoin="round">
        <path d="M22 2 11 13" />
        <path d="M22 2 15 22l-4-9-9-4 20-7Z" />
      </svg>
    </button>
  </div>
</template>
