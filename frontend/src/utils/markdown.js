const ESCAPE_MAP = {
  '&': '&amp;',
  '<': '&lt;',
  '>': '&gt;',
  '"': '&quot;',
  "'": '&#39;'
}

function escapeHtml(text) {
  return String(text).replace(/[&<>"']/g, (ch) => ESCAPE_MAP[ch])
}

const CODE_PLACEHOLDER = /\u0000CODE(\d+)\u0000/g
const CODE_LINE = /^\u0000CODE\d+\u0000$/

/** 行内语法：行内代码、加粗、链接（先转义再套标签，保证 XSS 安全） */
function renderInline(text) {
  let html = escapeHtml(text)
  html = html.replace(/`([^`\n]+)`/g, '<code class="inline-code">$1</code>')
  html = html.replace(/\*\*([^*\n]+)\*\*/g, '<strong>$1</strong>')
  html = html.replace(
    /\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)/g,
    '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>'
  )
  return html
}

/**
 * 轻量 Markdown 渲染（不引第三方库）
 *
 * 支持：
 * - 块级：代码块、# 标题(1~6 级)、- 无序列表、1. 有序列表、--- 分隔线
 * - 行内：行内代码、加粗、[文字](链接)
 *
 * 所有文本都先做 HTML 转义，避免模型输出被当成标签执行（防 XSS）。
 */
export function renderMarkdown(raw = '') {
  let text = String(raw)

  // 流式输出时可能出现未闭合的代码块，先补一个闭合，避免整段退化成纯文本
  if ((text.match(/```/g) || []).length % 2 === 1) {
    text += '\n```'
  }

  // ① 先把代码块抽出来占位，避免内部内容被其它规则破坏
  const codeBlocks = []
  text = text.replace(/```([^\n`]*)\n?([\s\S]*?)```/g, (_, lang, code) => {
    codeBlocks.push({ lang: (lang || '').trim(), code })
    return `\u0000CODE${codeBlocks.length - 1}\u0000`
  })

  // ② 逐行解析块级元素
  const out = []
  let listType = null

  const closeList = () => {
    if (listType) {
      out.push(`</${listType}>`)
      listType = null
    }
  }

  const openList = (type) => {
    if (listType !== type) {
      closeList()
      out.push(`<${type}>`)
      listType = type
    }
  }

  for (const rawLine of text.split('\n')) {
    const line = rawLine.trim()

    if (!line) {
      closeList()
      continue
    }

    // 代码块占位符独占一行
    if (CODE_LINE.test(line)) {
      closeList()
      out.push(line)
      continue
    }

    // 标题
    const heading = /^(#{1,6})\s+(.*)$/.exec(line)
    if (heading) {
      closeList()
      const level = heading[1].length
      out.push(`<h${level}>${renderInline(heading[2])}</h${level}>`)
      continue
    }

    // 分隔线
    if (/^(-{3,}|\*{3,}|_{3,})$/.test(line)) {
      closeList()
      out.push('<hr/>')
      continue
    }

    // 无序列表
    const bullet = /^[-*+]\s+(.*)$/.exec(line)
    if (bullet) {
      openList('ul')
      out.push(`<li>${renderInline(bullet[1])}</li>`)
      continue
    }

    // 有序列表
    const ordered = /^\d+[.)]\s+(.*)$/.exec(line)
    if (ordered) {
      openList('ol')
      out.push(`<li>${renderInline(ordered[1])}</li>`)
      continue
    }

    // 普通段落
    closeList()
    out.push(`<p>${renderInline(line)}</p>`)
  }

  closeList()

  // ③ 还原代码块（内容整体转义）
  return out.join('\n').replace(CODE_PLACEHOLDER, (_, index) => {
    const block = codeBlocks[Number(index)]
    if (!block) return ''
    const lang = block.lang ? `<span class="code-lang">${escapeHtml(block.lang)}</span>` : ''
    const code = escapeHtml(block.code.replace(/\n+$/, ''))
    return `<pre class="code-block">${lang}<code>${code}</code></pre>`
  })
}
