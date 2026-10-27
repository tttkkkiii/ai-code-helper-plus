# AI 编程小助手（ai-code-helper-plus）

> 面向「编程学习 + 求职面试」场景的对话式 AI 助手。后端 Spring Boot 3 + LangChain4j 接入通义千问，PostgreSQL + pgvector 做知识库检索；前端 Vue 3 + Vite，SSE 流式输出。

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen)
![LangChain4j](https://img.shields.io/badge/LangChain4j-1.19.0-blue)
![Vue](https://img.shields.io/badge/Vue-3.5-42b883)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-pgvector-336791)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

> 📄 **[30 个真实问题的排障记录（1800 余行）→](docs/troubleshooting.md)**
> 从环境构建、Spring 装配到工具调用与 SSE 传输，每条按「现象 → 根因 → 方案 → 实测效果」记录，含 101s → 11.7s、35s → 1.7s 等实测数据。

---

## 项目简介

一个「能记住上下文、能查自己的知识库、能实时打字输出」的编程问答助手，覆盖四个方向：

1. 编程学习路线规划
2. 项目学习建议
3. 程序员求职全流程（简历、投递）
4. 高频面试题与面试技巧

> **说明**：项目起步于「程序员鱼皮的 AI 编程助手」教程，在此基础上扩展了**多轮会话持久化、会话记忆回放、RAG 向量检索、输入护轨与工具调用**，并按生产可运维的方式重构了配置与分层。

技术上有两条主线：

- **对话链路**：LangChain4j `AiServices` 声明式接口 → 通义千问 `qwen-max` → Project Reactor `Flux` → SSE 推送到浏览器
- **检索链路（RAG）**：`docs/` 下的学习资料 → 段落切分 → `text-embedding-v4` 向量化 → 存入 pgvector → 按相似度召回 TopK 注入上下文

## 功能特性

| 能力 | 实现要点 |
|---|---|
| 多轮对话 | `@MemoryId` 区分会话，`MessageWindowChatMemory` 管理窗口 |
| 流式输出 | `StreamingChatModel` + `Flux<ServerSentEvent>`，前端 `EventSource` 逐字渲染 |
| 会话持久化 | MyBatis + PostgreSQL，两张表：`chat_session`（目录）、`chat_message`（流水） |
| 记忆回放 | 启动时从库里取最近 N 条塞回记忆窗口，进程重启不丢上下文 |
| RAG 知识库 | pgvector 向量库，`topK=5`、相似度阈值 `0.75`；首次启动自动灌库，之后跳过避免重复向量化 |
| 输入护轨 | LangChain4j `InputGuardrail` 敏感词拦截，命中时**不发起真实模型请求** |
| 工具调用 | `@Tool` 抓取「面试鸭」搜索结果，模型可自主决定何时调用 |
| 结构化输出 | `Report(name, suggestionList)` 演示把模型输出直接映射成 Java record |
| 调用观测 | `ChatModelListener` 打印完整请求 / 响应 / 异常 |

## 技术栈

**后端**

| 用途 | 选型 |
|---|---|
| 运行时 | Java 21、Spring Boot 3.2.5 |
| AI 框架 | LangChain4j 1.19.0（`langchain4j`、`langchain4j-reactor`、`langchain4j-pgvector`） |
| 模型 | 阿里云百炼 DashScope：`qwen-max`（对话 / 打分）、`text-embedding-v4`（1024 维向量） |
| 向量库 | PostgreSQL + pgvector |
| 持久层 | MyBatis 3.0.3 |
| 网页解析 | Jsoup（工具调用抓取面试题） |

**前端**

| 用途 | 选型 |
|---|---|
| 框架 | Vue 3.5（Composition API + `<script setup>`） |
| 构建 | Vite 6 |
| 请求 | Axios（REST）+ 原生 `EventSource`（SSE） |

## 系统架构

```
┌──────────────┐   GET /api/ai/chat (SSE)   ┌────────────────────────────────────┐
│  Vue 3 前端  │ ─────────────────────────► │  Spring Boot 3 (`/api`)            │
│  :5173       │                            │                                    │
│              │ ◄───── text/event-stream ── │  Aicontroller                      │
└──────────────┘                            │    └─ AiCodeHelperService           │
                                            │         (LangChain4j AiServices)    │
                                            │         ├─ ChatModel       qwen-max │
                                            │         ├─ ContentRetriever ──┐     │
                                            │         ├─ ChatMemoryProvider │     │
                                            │         └─ Tools: 面试题搜索   │     │
                                            └───────────────────────────────┼─────┘
                                                                            │
                              ┌─────────────────────────────────────────────┴──┐
                              │                                                │
                     ┌────────▼────────┐                            ┌──────────▼────────┐
                     │ PostgreSQL      │                            │ 百炼 DashScope    │
                     │ chat_session    │                            │ qwen-max          │
                     │ chat_message    │                            │ text-embedding-v4 │
                     │ rag_vectors     │                            └───────────────────┘
                     │ (pgvector)      │
                     └─────────────────┘
```

**三张表的分工**

| 表 | 作用 |
|---|---|
| `chat_session` | 会话目录：标题、创建 / 活跃时间，侧边栏列表用 |
| `chat_message` | 消息流水：角色 + 内容，按 `session_id` 顺序回放 |
| `rag_vectors` | 文档片段的向量，语义检索用（首次启动自动建表并灌库） |

## 目录结构

```
ai-code-helper-plus/
├── backend/                          # Spring Boot 后端
│   ├── src/main/java/com/itheima/aicodehelper/
│   │   ├── AiCodeHelperApplication.java
│   │   └── ai/
│   │       ├── AiCodeHelperService.java          # AiServices 声明式接口
│   │       ├── AiCodeHelperServiceFactory.java   # 装配记忆、RAG、工具
│   │       ├── controller/                       # 聊天 SSE + 会话 REST 接口
│   │       ├── service/ChatHistoryService.java   # 落库与记忆回放
│   │       ├── rag/RagConfig.java                # 向量库、检索器、灌库
│   │       ├── guardrail/SafeInputGuardrail.java # 输入护轨
│   │       ├── tools/InterviewQuestionTool.java  # 工具调用
│   │       ├── listener/                         # 模型调用日志
│   │       └── mapper/  pojo/                    # MyBatis 映射与实体
│   ├── src/main/resources/
│   │   ├── application.yml           # 不含任何密钥，全部走占位符
│   │   ├── system-prompt.txt         # 助手人设与回答范围
│   │   └── docs/                     # 知识库原始文档（RAG 数据源）
│   └── pom.xml
└── frontend/                         # Vue 3 前端（详见 frontend/README.md）
    ├── src/components/               # ChatRoom / MessageItem / ChatInput / SessionSidebar
    ├── src/api/                      # Axios 实例 + SSE 封装
    └── vite.config.js                # 开发代理 /api -> localhost:8081
```

## 快速开始

### 前置条件

- JDK 21
- Maven（或直接使用仓库内的 `mvnw`）
- Node.js 18+
- PostgreSQL 14+，并已安装 **pgvector** 扩展

### 1. 准备数据库

```sql
CREATE DATABASE "ai-code-helper";
\c ai-code-helper
CREATE EXTENSION IF NOT EXISTS vector;
```

> `rag_vectors` 表由程序首次启动时自动创建；`chat_session` / `chat_message` 两张表需要先建好（建表语句见下方「数据库结构」）。

### 2. 配置密钥（不进版本库）

`application.yml` 里只有占位符，真实配置放在 **`backend/application-local.yml`**（已被 `.gitignore` 排除），或改用环境变量。

```yaml
# backend/application-local.yml —— 不要提交
db:
  host: localhost
  port: 5432
  database: ai-code-helper
  user: postgres
  password: "your-password"

langchain4j:
  community:
    dashscope:
      chat-model:
        api-key: sk-your-key
      streaming-chat-model:
        api-key: sk-your-key
      embedding-model:
        api-key: sk-your-key
      scoring-model:
        api-key: sk-your-key
```

等价的**全环境变量**方式（部署时推荐）：

```bash
export DASHSCOPE_API_KEY=sk-your-key
export DB_HOST=localhost DB_PORT=5432 DB_DATABASE=ai-code-helper
export DB_USER=postgres DB_PASSWORD=your-password
```

### 3. 启动后端

```bash
cd backend
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

> ⚠️ 工作目录必须是 `backend/`：`RagConfig` 用相对路径 `src/main/resources/docs` 加载知识库，根目录的 `application-local.yml` 也是按「当前工作目录」查找的。

服务地址 `http://localhost:8081/api`。首次启动会把 `docs/` 里的文档向量化写入 pgvector，日志可见灌库耗时。

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

浏览器自动打开 <http://localhost:5173>，`/api` 请求由 Vite 代理到 `8081`，无需处理跨域。

## 接口一览

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/ai/chat?memoryId={id}&message={text}` | 聊天，返回 `text/event-stream` |
| POST | `/api/chat/session` | 新建会话 |
| GET | `/api/chat/sessions` | 会话列表（按活跃时间倒序） |
| GET | `/api/chat/session/{id}` | 某会话的历史消息 |
| DELETE | `/api/chat/session/{id}` | 删除会话及其消息 |

SSE 每个数据块是 `{"c":"片段"}` 形式的 JSON，而不是裸文本 —— 这样可以避免 SSE 规范吃掉文本开头的空格。

## 数据库结构

```sql
CREATE TABLE chat_session (
    id         BIGSERIAL PRIMARY KEY,
    title      VARCHAR(64) NOT NULL DEFAULT '新会话',
    created_at TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE TABLE chat_message (
    id         BIGSERIAL PRIMARY KEY,
    session_id BIGINT      NOT NULL,
    role       VARCHAR(16) NOT NULL,      -- user / ai
    content    TEXT        NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX idx_message_session ON chat_message (session_id, id);
```

## 设计说明

**为什么流式用 `EventSource` 而不是 Axios？**
浏览器端 Axios 基于 XHR，无法消费 `text/event-stream`。所以流式请求交给原生 `EventSource`，但它的 URL 复用 Axios 实例的 `baseURL`，保证接口前缀只有一处配置。

**为什么向量库选 pgvector 而不是 ES / Milvus？**
业务数据本来就在 PostgreSQL（会话、消息），向量只占其中一小块。用 pgvector 可以少维护一套中间件，且同一事务里能同时读写业务表和向量表；千万级以下规模完全够用。

**会话记忆怎么控制长度？**
库里是 append-only 全量保存，但回放给模型时只取最近 `chat-memory.max-messages`（默认 10）条塞进 `MessageWindowChatMemory`，兼顾成本与上下文连贯性。

**为什么敏感词用单词边界正则？**
`text.contains("kill")` 会把 `skill` 一起拦掉 —— 对编程助手来说致命（"Java skills" 直接被拒）。改用 `\bkill\b` 后，`skill` 不匹配、`kill the process` 能匹配，中英混排也正常。

**灌库为什么只做一次？**
启动时先 `count(*)` 统计 `rag_vectors`，非空就跳过。否则每次重启都会重复向量化，既浪费 embedding 额度，又会把同一片段重复写进去污染检索结果。

## 已知限制

- 没有鉴权：接口是裸的，仅适合本地 / 内网演示
- `memoryId` 由前端自行生成，后端不校验归属
- 记忆是单机内存态缓存，多实例部署需改为共享存储
- 未做请求限流与并发控制，也没有 Token 用量统计

## 配套文档

- [踩坑与解决方案](docs/troubleshooting.md) —— 30 个真实问题的完整记录，每条按「现象 → 根因 → 方案 → 实测效果」组织，覆盖环境构建、Spring 装配、性能定位、工具调用、SSE 传输与前端渲染六个环节。

## License

[MIT](./LICENSE)
