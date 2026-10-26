# AI 编程小助手 · 前端

基于 **Vue 3 + Vite** 的单页聊天应用：进入页面自动生成一个聊天室 id，通过 **SSE** 实时渲染大模型的流式回复。

## 技术栈

| 用途 | 选型 |
|---|---|
| 框架 | Vue 3（Composition API + `<script setup>`） |
| 构建 | Vite 6 |
| HTTP | Axios（统一实例 / 接口前缀管理） |
| 流式 | 原生 `EventSource` 消费 `text/event-stream` |

> 为什么流式不用 Axios：SSE 是 `text/event-stream` 长连接，**浏览器端的 Axios 不支持消费流式响应**。
> 所以流式请求交给原生 `EventSource`，但它的地址复用 Axios 实例的 `baseURL`，保证「接口前缀」只有一处配置。

## 目录结构

```
ai-code-helper-frontend/
├── index.html
├── package.json
├── vite.config.js          # 开发代理：/api -> http://localhost:8081
└── src/
    ├── main.js
    ├── App.vue             # 页面外壳 + 二次元角色背景
    ├── config.js           # 接口前缀 / 聊天接口路径
    ├── api/
    │   ├── http.js         # Axios 实例
    │   └── chat.js         # SSE 流封装
    ├── components/
    │   ├── ChatRoom.vue    # 聊天室主逻辑
    │   ├── MessageItem.vue # 单条消息（用户右 / AI 左）
    │   └── ChatInput.vue   # 输入框
    ├── utils/
    │   ├── id.js           # 聊天室 id / 消息 id
    │   └── markdown.js     # 轻量 Markdown 渲染（无三方依赖）
    ├── assets/
    │   └── assistant.svg   # 手绘二次元助手形象
    └── styles/
        └── main.css
```

## 启动

```bash
npm install
npm run dev
```

浏览器会自动打开 <http://localhost:5173>。

生产构建：

```bash
npm run build      # 产物在 dist/
npm run preview
```

## 接口对接

### 请求

```
GET /api/ai/chat?memoryId=<数字>&message=<用户输入>
Accept: text/event-stream
```

对应后端：

```java
@RestController
@RequestMapping("/ai")
public class AiController {
    @GetMapping("/chat")
    public Flux<ServerSentEvent<String>> chat(int memoryId, String message) { ... }
}
```

> ⚠️ **路径请确认**：如果你后端的 `AiController` **没有** `@RequestMapping("/ai")`，
> 实际路径是 `/api/chat`，请把 `src/config.js` 里的 `CHAT_PATH` 改成 `'/chat'`。

### 参数说明

| 参数 | 类型 | 说明 |
|---|---|---|
| `memoryId` | `int` | 聊天室 id，前端进入页面时随机生成一个 int32 范围内的正整数 |
| `message` | `string` | 用户输入，已做 URL 编码 |

### 响应

后端返回 `text/event-stream`，每个数据块形如：

```
data:你好，我是编程小助手

data:，可以帮你规划学习路线
```

前端把每个 `data:` 片段依次追加到同一条 AI 消息里，形成打字机效果。

## 跨域（CORS）

开发环境已通过 Vite 代理解决：`/api` → `http://localhost:8081`，浏览器只与 5173 通信，不触发跨域。

如果要直连后端，新建 `.env.local`：

```ini
VITE_API_BASE_URL=http://localhost:8081/api
```

同时后端需要放行跨域，例如在 Controller 上加 `@CrossOrigin`，或做全局 CORS 配置。

## 交互说明

- **Enter** 发送，**Shift + Enter** 换行
- 右上角「会话 #id」显示当前聊天室 id，点「新会话」会重置聊天记录并重新生成 id
- 流式生成过程中输入框禁用，避免并发请求串台
- 顶部空状态提供 4 个快捷提问
- 支持 `` `行内代码` ``、` ```代码块``` `、`**加粗**` 的轻量渲染，所有内容均做 HTML 转义

## 常见问题

| 现象 | 排查 |
|---|---|
| 页面一直显示「连接后端失败了」 | 后端是否已在 8081 启动；`server.servlet.context-path` 是否为 `/api` |
| 返回 404 | `CHAT_PATH` 与后端 `@RequestMapping` 是否一致 |
| 返回 403 / CORS 报错 | 说明绕过了代理直连后端，需后端配置 CORS |
| 内容一次性全部出现、没有逐字效果 | 后端返回的不是流式（确认用的是 `StreamingChatModel`） |
