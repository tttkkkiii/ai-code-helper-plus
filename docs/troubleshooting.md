# ai-code-helper 踩坑与解决方案

> 30 个真实问题的记录，每条按「现象 → 根因 → 方案 → 实测效果」组织。
> 覆盖环境构建、Spring 装配、性能定位、工具调用、SSE 传输与前端渲染六个环节。
> 本文为脱敏版：数据库口令、内网地址等已替换为占位符。

# ai-code-helper 项目踩坑与解决方案

> AI 编程小助手 —— Spring Boot + LangChain4j + Vue3 全栈实践
> 记录时间：2026-10

---

## 一、项目简介

| 项 | 内容 |
|---|---|
| 后端 | Spring Boot 3.2.5 + LangChain4j 1.19.0 + 阿里云 DashScope（qwen-max） |
| 前端 | Vue 3 + Vite 6 + Axios，SSE 流式渲染 |
| 能力 | RAG 知识库、Function Calling 工具、输入护轨、流式对话 |
| 编译 JDK | 21 |
| 本机 JDK | 26（踩坑重灾区，见 1.5） |

目录结构（后端）：

```
com.itheima.aicodehelper
├── ai
│   ├── AiCodeHelperService.java        # AI Service 接口
│   ├── AiCodeHelperServiceFactory.java # AiServices 构建
│   ├── controller/Aicontroller.java    # SSE 接口
│   ├── guardrail/SafeInputGuardrail.java
│   ├── listener/ChatModelLoggingListener.java
│   ├── rag/RagConfig.java
│   └── tools/InterviewQuestionTool.java
```

---

<a id="toc"></a>

## 二、问题总览（速查表）

| # | 分类 | 现象关键字 | 一句话方案 |
|---|---|---|---|
| [1](#q1) | 环境 | [`npm` 不是内部或外部命令](#q1) | 装 Node.js LTS |
| [2](#q2) | 环境 | [`npm error path C:\Users\xxx\package.json`](#q2) | `cd /d` 真正的项目目录 |
| [3](#q3) | 环境 | [IDEA「Maven wrapper: DistributionUrl is empty」](#q3) | IDEA 改用系统 Maven，或补 distributionUrl |
| [4](#q4) | 构建 | [`Unrecognised tag: 'mirror'`](#q4) | `<mirror>` 挪进 `<mirrors>` |
| [5](#q5) | 构建 | [`TypeTag :: UNKNOWN`](#q5) | Lombok 升到 1.18.48 |
| [6](#q6) | 构建 | [`annotationProcessorPath ... version can neither be null`](#q6) | 补 `<version>${lombok.version}</version>` |
| [7](#q7) | Spring | [`No qualifying bean of type EmbeddingStore`](#q7) | 手工声明 `InMemoryEmbeddingStore` Bean |
| [8](#q8) | Spring | [`No qualifying bean of type StreamingChatModel`](#q8) | yml 补 `streaming-chat-model` |
| [9](#q9) | Spring | [`must be annotated with @UserMessage / @MemoryId ...`](#q9) | 多参数方法逐个标注角色 |
| [10](#q10) | 性能 | [启动 35 秒](#q10) | 定位到 RAG 灌库，改持久化/懒加载 |
| [11](#q11) | 性能 | [单次对话 101 秒 + 输出乱码](#q11) | 工具不要返回原始 HTML |
| [12](#q12) | 工具 | [偶发卡 15 秒](#q12) | 别让模型自己猜 URL 路径 |
| [13](#q13) | 传输 | [英文单词粘连 `assistyou`](#q13) | SSE 用 JSON 包装数据 |
| [14](#q14) | 联调 | [永远回答英文、不调工具](#q14) | Controller 补 `@RequestParam` |
| [15](#q15) | 护轨 | [`skill` 被敏感词误伤](#q15) | 改用 `\bkill\b` 单词边界 |
| [16](#q16) | 联调 | [访问 8081 根路径 404](#q16) | 路径要带 context-path 和接口路径 |
| [17](#q17) | 前端 | [Axios 拿不到流式数据](#q17) | 改用 EventSource |
| [18](#q18) | 前端 | [localhost:5173 打不开](#q18) | 开发服务器没启动 |
| [19](#q19) | 前端 | [模型输出被当 HTML 执行](#q19) | `v-html` 前必须转义 |
| [20](#q20) | 前端 | [流式追加内容不刷新](#q20) | 用 `reactive()` 包装对象 |
| [21](#q21) | 后端 | [新建会话 `createdAt` 返回 null](#q21) | `@JsonInclude(NON_NULL)` 省略空字段 |
| [22](#q22) | 环境 | [`Injection of resource dependencies failed`](#q22) | 顺 `Caused by` 找到数据库连接被拒 |
| [23](#q23) | 环境 | [`Unit postgresql.service could not be found`](#q23) | Docker 用 `docker start`，否则 `sudo systemctl` |
| [24](#q24) | 后端 | [`从 long 转换到 int 可能会有损失`](#q24) | `@MemoryId` 参数统一改 `long` |
| [25](#q25) | 联调 | [Apifox 调会话详情 404](#q25) | Path 参数的**值**没填 |
| [26](#q26) | 联调 | [DELETE 返回 200 却显示"失败"](#q26) | 响应体为空，校验规则要求 JSON |
| [27](#q27) | 前端 | [`EBUSY: watch '...tmpdir'`](#q27) | Vite `watch.ignored` 忽略临时目录 |
| [28](#q28) | 前端 | [`###`、`-` 原样显示](#q28) | 补标题/列表/链接的块级解析 |
| [29](#q29) | 前端 | [流式输出卡顿掉帧](#q29) | 批量刷新 + 去 smooth/动画/毛玻璃 |
| [30](#q30) | 前端 | [滚轮不同步、有延迟](#q30) | 跟随状态机 + 移除 `backdrop-filter` |

---

## 三、详细记录

### 1. 环境与构建

<a id="q1"></a>

#### 问题 1：`npm` 命令找不到

**现象**

```
C:\Users\小陈>npm install
'npm' 不是内部或外部命令
```

**根因**

机器上根本没装 Node.js。排查结果：

```
node / npm / npx / corepack  -> 全部未找到
C:\Program Files\nodejs      -> 不存在
系统 PATH 里没有任何 node/npm 条目
```

> 注意：开发工具里自带的 Node 运行时（如果有）不属于系统 PATH，你自己的终端用不到。

**解决方案**

```powershell
# 管理员身份运行
winget install --id OpenJS.NodeJS.LTS -e --accept-package-agreements --accept-source-agreements
```

装完 **必须关掉所有终端和 IDEA 重新打开**，PATH 是进程启动时读取的。

验证：

```powershell
node -v
npm -v
```

国内建议先换镜像：

```powershell
npm config set registry https://registry.npmmirror.com
```

[↑ 返回速查表](#toc)

---

<a id="q2"></a>

#### 问题 2：`npm install` 报 ENOENT，找不到 package.json

**现象**

```
npm error code ENOENT
npm error path C:\Users\小陈\package.json
npm error enoent Could not read package.json: Error: ENOENT
```

**根因**

不是权限问题、不是盘符问题，而是**当前目录不对**。

诡异之处在于这条命令：

```cmd
C:\Users\小陈>cd D:\JAVAcode\ai-code-helper-frontend

C:\Users\小陈>npm install      ← 提示符还是 C 盘！
```

**cmd 的 `cd` 跨盘不会切换**：它只改了 D 盘记录的“当前目录”，但人还站在 C 盘。于是 npm 在 `C:\Users\小陈\` 下找 `package.json`，自然找不到。

**解决方案**

```cmd
cd /d D:\JAVAcode\ai-code-helper-frontend
```

`/d` 才是“连盘符一起切”。

或者分两步：

```cmd
D:
cd \JAVAcode\ai-code-helper-frontend
```

> **心智模型**：`npm install` 只认一件事 —— **当前目录下有没有 `package.json`**。跟 C 盘 D 盘毫无关系，放哪个盘都能装。

[↑ 返回速查表](#toc)

---

<a id="q3"></a>

#### 问题 3：IDEA 提示 Maven wrapper 的 DistributionUrl 为空，无法下载依赖

**现象**

IDEA 弹窗：

> Maven wrapper: DistributionUrl is empty. The Maven bundled version will be used instead.

之后依赖无法下载。

**根因**

`.idea/workspace.xml` 里：

```xml
<option name="mavenHomeTypeForPersistence" value="WRAPPER" />
```

IDEA 被设置成用 **Maven Wrapper**，而 wrapper 的配置文件是空的：

```
.mvn\wrapper\maven-wrapper.properties   大小 1 字节（只有一个换行）
```

wrapper 不知道去哪下载 Maven → Maven 起不来 → 依赖下载中断。

**解决方案 A（推荐，不改文件）**

IDEA → `Settings` → `Build, Execution, Deployment` → `Build Tools` → `Maven`，
把 **Maven home path** 从 "Use Maven wrapper" 改成已安装的 Maven 路径，例如：

```
D:\Software\apache-maven-3.9.16-bin\apache-maven-3.9.16
```

**解决方案 B：补全 wrapper 配置**

`.mvn\wrapper\maven-wrapper.properties` 写入（对应 mvnw 3.3.4）：

```properties
wrapperVersion=3.3.4
distributionType=only-script
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.16/apache-maven-3.9.16-bin.zip
```

[↑ 返回速查表](#toc)

---

<a id="q4"></a>

#### 问题 4：Maven 镜像不生效，下载龟速

**现象**

```
[WARNING] Unrecognised tag: 'mirror' (position: START_TAG seen ... @ settings.xml, line 147, column 12)
```

下载速度只有 12～200 kB/s，全走国外中央仓库。

**根因**

`settings.xml` 里镜像放错了位置 —— 写在了 `<mirrors>` **外面**：

```xml
   |-->
   <mirror>                      <!-- ❌ 漏在 <mirrors> 之外 -->
      <id>alimaven</id>
      <url>https://maven.aliyun.com/nexus/content/groups/public/</url>
      <mirrorOf>central</mirrorOf>
   </mirror>
  <mirrors>                      <!-- <mirrors> 从这里才开始 -->
```

**解决方案**

把 `<mirror>` 移进 `<mirrors>` 内部：

```xml
  <mirrors>
    <mirror>
      <id>alimaven</id>
      <name>aliyun maven</name>
      <url>https://maven.aliyun.com/nexus/content/groups/public/</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
    ...
  </mirrors>
```

[↑ 返回速查表](#toc)

---

<a id="q5"></a>

#### 问题 5：Lombok 不兼容 JDK 26

**现象**

```
[ERROR] Fatal error compiling: java.lang.ExceptionInInitializerError:
        com.sun.tools.javac.code.TypeTag :: UNKNOWN
```

**根因**

本机 `JAVA_HOME` 指向 **JDK 26**，而 Spring Boot 3.2.5 依赖管理里的 Lombok 是 **1.18.32**，这个版本不认识 JDK 26 的编译器内部结构。

IDEA 里没事，是因为 IDEA 用的是项目 SDK（JDK 21）+ 自己的编译流程，不走这条链路。

**解决方案**

任选其一：

**A. 换 JDK（最稳，和 `java.version=21` 一致）**

把 `JAVA_HOME` 指到 JDK 21。

**B. 升级 Lombok**

在 `pom.xml` 的 `<properties>` 里覆盖版本：

```xml
<properties>
    <java.version>21</java.version>
    <lombok.version>1.18.48</lombok.version>
</properties>
```

或者命令行临时指定：

```powershell
mvn -Dlombok.version=1.18.48 compile
```

> 注意 PowerShell 的坑：`-Dlombok.version=1.18.48` 会被拆开，必须用引号包起来：`'-Dlombok.version=1.18.48'`。

[↑ 返回速查表](#toc)

---

<a id="q6"></a>

#### 问题 6：annotationProcessorPaths 缺 version

**现象**

```
[ERROR] Resolution of annotationProcessorPath dependencies failed:
        version can neither be null, empty nor blank
```

**根因**

`maven-compiler-plugin:3.11.0` 要求 `annotationProcessorPaths` 里的依赖**必须显式写版本**，不能依赖 `dependencyManagement` 补全：

```xml
<annotationProcessorPaths>
    <path>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <!-- 缺 version -->
    </path>
</annotationProcessorPaths>
```

**解决方案**

```xml
<annotationProcessorPaths>
    <path>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>${lombok.version}</version>
    </path>
</annotationProcessorPaths>
```

[↑ 返回速查表](#toc)

---

### 2. Spring 装配

<a id="q7"></a>

#### 问题 7：找不到 `EmbeddingStore<TextSegment>` Bean

**现象**

```
org.springframework.beans.factory.NoSuchBeanDefinitionException:
No qualifying bean of type 'dev.langchain4j.store.embedding.EmbeddingStore
<dev.langchain4j.data.segment.TextSegment>' available
```

调用链：`aiCodeHelperServiceFactory` → `ragConfig` → `@Resource EmbeddingStore` 注入失败 → 整个上下文启动失败 → `@SpringBootTest` 报 `Failed to load ApplicationContext`。

**根因**

`RagConfig` 里用 `@Resource` 注入 `EmbeddingStore<TextSegment>`，但**没有任何地方提供这个 Bean**。

关键认知：

- DashScope starter 会根据 `langchain4j.community.dashscope.*` 配置**自动创建** `ChatModel` 和 `EmbeddingModel`
- 但它**不会**自动创建 `EmbeddingStore`（向量库）—— 这个必须自己声明

这也解释了为什么只有 `EmbeddingStore` 报错，而 `ChatModel` / `EmbeddingModel` 都注入成功。

**解决方案**

```java
@Bean
public EmbeddingStore<TextSegment> embeddingStore() {
    return new InMemoryEmbeddingStore<>();
}
```

> 另一种可能：老师的项目里多一个 `langchain4j-easy-rag` 依赖（"极简版 RAG"那节加的，讲标准版时没删）。easy-rag 的定位就是“你不用自己选向量库”。如果代码一模一样但老师能跑，先对比 `pom.xml`。

[↑ 返回速查表](#toc)

---

<a id="q8"></a>

#### 问题 8：找不到 `StreamingChatModel` Bean

**现象**

```
No qualifying bean of type 'dev.langchain4j.model.chat.StreamingChatModel' available
  → 在 aiCodeHelperServiceFactory 上 @Resource 注入失败
```

**根因**

想让 AI Service 返回 `Flux<String>` 做流式输出，于是加了：

```java
@Resource
private StreamingChatModel qwenStreamingChatModel;
```

但 `application.yml` 里只配了 `chat-model`，**没有 `streaming-chat-model`**，所以 starter 没创建流式模型的 Bean。

**解决方案**

```yaml
langchain4j:
  community:
    dashscope:
      chat-model:
        model-name: qwen-max
        api-key: ${DASHSCOPE_API_KEY}
      streaming-chat-model:          # ← 补上这一段
        model-name: qwen-max
        api-key: ${DASHSCOPE_API_KEY}
```

> 记忆点：**`ChatModel` 和 `StreamingChatModel` 是两个独立 Bean**，配了前者不等于有后者。

[↑ 返回速查表](#toc)

---

<a id="q9"></a>

#### 问题 9：AI Service 方法参数必须标注角色

**现象**

```
dev.langchain4j.service.IllegalConfigurationException:
AiCodeHelperService must be annotated with either @UserMessage, @V, @MemoryId,
or @UserName, or it should be of type InvocationParameters or ChatRequestParameters
```

**根因**

```java
String chat(@MemoryId int memoryId, String userMessage);   // userMessage 没角色
```

LangChain4j 的规则：

- 方法**只有 1 个参数** → 默认当作用户消息，不用注解
- 方法**有多个参数** → 每个参数都必须显式标角色（`@UserMessage` / `@V` / `@MemoryId` / `@UserName`），或者类型是 `ChatRequestParameters` / `InvocationParameters`

一旦加了 `@MemoryId` 变成两个参数，就不再享受“单参数默认”。

**解决方案**

```java
String chat(@MemoryId int memoryId, @UserMessage String userMessage);
Report chatForReport(@MemoryId int memoryId, @UserMessage String userMessage);
```

> 注意：`chatForReport` 有一模一样的毛病，只改一个会在下一个测试里再报一次。

[↑ 返回速查表](#toc)

---

### 3. AI 能力与性能

<a id="q10"></a>

#### 问题 10：每次启动都要 35 秒

**现象**

启动日志：

```
Started AiCodeHelperApplication in 35.043 seconds
```

**定位过程**

打开 LangChain4j 的 DEBUG 日志（`--logging.level.dev.langchain4j=DEBUG`），拿到确切耗时：

```
20:12:43.028  Starting to ingest 4 documents
20:12:43.053  Documents were split into 154 text segments
20:12:43.054  Starting to embed 154 text segments
20:13:16.546  Finished embedding 154 text segments      ← 33.5 秒
20:13:17.200  Started AiCodeHelperApplication in 35.043 seconds
```

**154 个文本段的 Embedding 花了 33.5 秒，占启动时间 95%。**

**根因**

```java
@Bean
public ContentRetriever contentRetriever() {
    List<Document> documents = FileSystemDocumentLoader.loadDocuments("src/main/resources/docs");
    ...
    ingestor.ingest(documents);   // 启动即把 154 段全部调接口向量化
}
```

1. 这是个 `@Bean`，Spring 启动时必定执行
2. `docs` 目录一共 **124,447 字**，被切成 154 段
3. 每段都要发 HTTP 请求到 DashScope 的 `text-embedding-v4` 拿向量
4. 用的是 `InMemoryEmbeddingStore`，进程一关数据就没了 → **每次启动都要重灌**

**解决方案**

| 方案 | 做法 | 代价 |
|---|---|---|
| A 懒加载 | `spring.main.lazy-initialization: true` | 启动秒开，但第一次对话要等 30 秒 |
| B 持久化向量库（正解） | Redis / PGVector / Chroma，灌一次存盘，启动判空跳过 | 一次改造 |
| C 砍文档 | 临时把大文件移出 `docs` | 见效快，信息少 |
| D 先注释灌库 | `// ingestor.ingest(documents);` | RAG 失效，仅调试期用 |

> **阶段性验证**：先注释掉 `ingest` 验证瓶颈，启动 **35.043s → 1.148s**（这一步只是定位用，RAG 会失效）。

**最终落地方案（B）：PGVector 持久化 + 启动判空**

1. `pom.xml` 引入 `langchain4j-pgvector:1.19.0-beta29`（会传递带入 `postgresql` JDBC 驱动，不用单独加）
2. `embeddingStore()` 从 `InMemoryEmbeddingStore` 换成 `PgVectorEmbeddingStore.builder()`，指向虚拟机里的 PostgreSQL
3. 灌库前先查一次表里有没有数据，有就跳过：

```java
// 向量库已有数据就跳过灌库，避免每次启动重复向量化 + 数据重复堆积
if (countVectors() == 0) {
    ingestor.ingest(documents);
}
```

```java
private long countVectors() {
    String url = "jdbc:postgresql://your-db-host:5432/postgres";
    try (Connection conn = DriverManager.getConnection(url, "postgres", "your-db-password");
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery("SELECT count(*) FROM rag_vectors")) {
        rs.next();
        return rs.getLong(1);
    } catch (Exception e) {
        return 0;   // 表不存在或连不上，按首次灌库处理
    }
}
```

**最终实测（2026-10-19）**

| 场景 | 启动耗时 |
|---|---|
| 改造前（每次重灌 154 段） | **35.043 s** |
| 改造后 · 第 1 次启动 | **1.895 s** |
| 改造后 · 第 2 次启动 | **1.721 s** |

```
2026-10-19T00:47:38.031  Started AiCodeHelperApplication in 1.895 seconds
2026-10-19T00:47:42.519  Started AiCodeHelperApplication in 1.721 seconds
```

**提速约 19 倍**，两次结果都在 2 秒内且稳定，说明判空逻辑可靠。

> ⚠️ 对外描述时注意区分两个数字：
> `1.148s` 是 `@SpringBootTest` 上下文启动（不含 Tomcat 和数据库连接），
> `1.721s / 1.895s` 才是**真实应用启动**耗时，简历/汇报里应该用后者。

[↑ 返回速查表](#toc)

---

<a id="q11"></a>

#### 问题 11：工具返回原始 HTML，导致 101 秒 + 输出错乱

**现象**

单次对话耗时 101 秒，而且模型输出的都是不知所云的内容。

**定位过程**

先验证是不是接口本身慢 —— 直接打 DashScope 裸接口：

```
不带工具：HTTP 200，耗时 1081 ms，正常回答
带工具定义：HTTP 200，耗时 895 ms，返回 tool_calls
```

接口 1 秒就返回了，问题在工具链路。接着查工具抓的页面有多大：

| 地址 | 结果 |
|---|---|
| `https://www.mianshiya.com/questions/java` | 连接被中止（挂到超时） |
| `https://www.mianshiya.com` | HTTP 200，**954,824 字节** |
| `/search/all?searchText=java` | HTTP 200，**756,897 字节** |

**根因**

工具把 **70～90 万字符的原始 HTML** 整个塞回模型上下文，而 qwen-max 上下文只有 32k tokens，直接被撑爆。

一趟流程：模型猜错路径 → 卡 15 秒超时 → 换个能通的路径 → 上传 900KB HTML → 模型在噪音里胡言乱语。

**解决方案**

工具内部先解析，只返回需要的内容：

```java
@Tool(name = "interviewQuestionSearch", value = "...")
public String searchInterviewQuestions(@P(value = "the keyword to search") String keyword) {
    String url = "https://www.mianshiya.com/search/all?searchText="
               + URLEncoder.encode(keyword, StandardCharsets.UTF_8);
    Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(5000).get();

    // 只取结果表格每行第 2 列的题目链接，避免把标签也抓进来
    Elements questionElements = doc.select("tbody tr.ant-table-row td:nth-child(2) a");
    List<String> questions = new ArrayList<>();
    questionElements.forEach(el -> questions.add(el.text().trim()));
    return String.join("\n", questions);
}
```

**效果对比（同一页面实测）**：

| 返回方式 | 体积 | 内容质量 |
|---|---|---|
| `doc.outerHtml()` | **744,050 字符** | 几乎全是内联 JS/CSS，模型不能用 |
| `doc.text()`（整页纯文本） | 2,057 字符 | 能接受，但混了导航栏、SEO 文案 |
| `doc.select(...)` | **489 字符** | 只有 20 条题目，最干净 |

**单次对话耗时：101.0s → 11.72s。**

> **核心结论**：选择器不是目的，「把大 HTML 变成小文本」才是目的。
> 真正关键的一行是 `return doc.outerHtml();` —— 只要不返回原始 HTML，其它都好商量。

[↑ 返回速查表](#toc)

---

<a id="q12"></a>

#### 问题 12：让模型自己猜 URL 的坑

**现象**

偶发卡 15 秒。

**根因**

工具参数设计成“URL 的剩余部分”，让模型自己拼路径，结果模型编了一个 `/questions/java` —— 这个地址实测**连接被中止**，一直等到超时才失败。

**解决方案**

参数改成**业务语义**（关键词），URL 在代码内部拼死：

```java
// ❌ 让模型猜
String fetchPage(String path)
String url = BASE_URL + path;

// ✅ 内部拼
String search(String keyword)
String url = BASE_URL + "/search/all?searchText=" + encode(keyword);
```

**为什么这个更关键**

| 假设 | 结果 |
|---|---|
| 只修返回值（返回文本），不修 URL（模型仍猜路径） | 能用，但偶尔等 5～15 秒超时 |
| 只修 URL（固定路径），不修返回值（仍返回 HTML） | **依然 100 秒 + 输出错乱** |

所以：**返回体积决定“能不能用”，URL 决定“稳不稳、快不快”**。

[↑ 返回速查表](#toc)

---

<a id="q13"></a>

#### 问题 13：SSE 吃掉前导空格，英文单词粘连

**现象**

前端显示：

```
Hello! How can I assistyou withyour programming learning
              ↑ 少了空格      ↑ 少了空格
```

**根因**

SSE 规范规定：客户端解析 `data:` 行时，**会去掉字段值开头的一个空格**。

而 Spring 写出的是 `data:` 直接接原文：

```
模型 chunk : " an"          ← 以 1 个空格开头
Spring 写出: data: an        ← 网络上只有 1 个空格
浏览器解析: "an"             ← 规范吃掉这 1 个空格
结果      : 空格丢了
```

**解决方案 A：后端补一个空格（一行，前端不动）**

```java
.map(chunk -> ServerSentEvent.<String>builder()
        .data(chunk.startsWith(" ") ? " " + chunk : chunk)
        .build())
```

原理：SSE 吃掉的空格是**固定成本**，后端补一个就是**预付**，一进一出净值为零。

> 注意判断不能省：只有以空格开头时才补，否则会在每个词前凭空多一个空格。

**解决方案 B：JSON 包装（更规范，前后端各改几行）**

后端：

```java
@Resource
private ObjectMapper objectMapper;     // Spring Boot 自带，不用 new

.map(chunk -> {
    String data;
    try {
        data = objectMapper.writeValueAsString(Map.of("c", chunk));
    } catch (Exception e) {
        data = "{}";
    }
    return ServerSentEvent.<String>builder(data).build();
})
```

网络上传的就变成：

```
data:{"c":"以下"}
data:{"c":" Java"}        ← 前导空格被 JSON 保住
data:{"c":"题：\n\n1. Java"}  ← 换行也被正确转义
```

前端：

```js
source.onmessage = (event) => {
  if (!event.data) return
  let piece = event.data
  try {
    piece = JSON.parse(event.data).c ?? ''   // 新版后端
  } catch {
    /* 兼容旧后端：当纯文本处理 */
  }
  if (!piece) return
  onChunk?.(piece)
}
```

`try/catch` 让前端可以**先上线**，后端没改也不会坏。

**顺带澄清一个概念**

| | 结构化输出（Structured Output） | JSON 包装 |
|---|---|---|
| 谁在约束 | 大模型生成的内容 | 服务端发出的传输格式 |
| 解决的问题 | 让模型返回可解析的 JSON | 让 SSE 不吃空格 |
| 能否用于流式 | ❌ 流式时每块都是残缺 JSON | ✅ 每块都是完整 JSON |

**结论：JSON 包装只能在传输层做，不能靠结构化输出。**

[↑ 返回速查表](#toc)

---

<a id="q14"></a>

#### 问题 14：永远回答英文，且从不调用工具

**现象**

中文提问「Java 集合常见面试题有哪些？」，模型回答：

```
Hello! How can I assist you with your programming learning or job search today?...
```

而且是英文、像在打招呼、完全不调工具。

**定位过程**

直接 curl 后端，绕开前端：

```powershell
curl -N -G --data-urlencode "memoryId=your-db-password" `
     --data-urlencode "message=Java 集合常见面试题有哪些？" `
     http://localhost:8081/api/ai/chat
```

返回：

```
data:Hello
data:! How can
data: I assist you
data: with
```

**传的是中文，回的还是 Hello** —— 证明是后端问题，与前端、与模型无关。

**根因**

```java
@GetMapping("/chat")
public Flux<ServerSentEvent<String>> chat() {          // ← 一个参数都没有
    return aiCodeHelperService.chatStream(1, "hello")   // ← 写死
            .map(...);
}
```

前端传的 `memoryId` 和 `message` **被直接丢弃**，模型收到的永远是英文单词 `"hello"`。

三个现象一条根因：

| 现象 | 真实原因 |
|---|---|
| 一直回答英文 | 模型收到的输入是 `"hello"` |
| 答非所问 | `"hello"` 只能寒暄 |
| 不调工具 | `"hello"` 不需要面试题 |

**解决方案**

```java
@GetMapping("/chat")
public Flux<ServerSentEvent<String>> chat(@RequestParam int memoryId,
                                          @RequestParam String message) {
    return aiCodeHelperService.chatStream(memoryId, message).map(...);
}
```

**验证结果**

```
data:以下是一
data:些关于 Java
data: 集
data:合的常见面试题
data:：
```

中文、正经回答、工具正常触发。

[↑ 返回速查表](#toc)

---

<a id="q15"></a>

#### 问题 15：护轨把 `skill` 当成敏感词拦掉

**现象**

敏感词配置为 `kill`，结果 `skill` / `skills` 也被拦截 —— 对编程助手来说很致命（"Java skills" 直接被拒）。

**根因**

```java
text.toLowerCase().contains("kill")
```

`"skill"` 里第 2～5 个字符正好是 `kill`：

```
s k i l l
  └─ kill ─┘
```

**解决方案**

改用单词边界正则：

```java
private static final Pattern SENSITIVE_PATTERN =
        Pattern.compile("\\bkill\\b", Pattern.CASE_INSENSITIVE);

if (SENSITIVE_PATTERN.matcher(userMessage.singleText()).find()) {
    return failure("输入包含敏感词：kill，请换一种表达方式。");
}
```

`\bkill\b` 不会匹配 `skill`（前面是字母，没有边界），但会匹配 `kill the process`。

**顺带记住的护轨行为**

命中护轨时，LangChain4j 抛 **`InputGuardrailException`**（RuntimeException 子类），并且**根本不会请求大模型** —— 省 token 也省时间。调用方要 `try/catch`。

[↑ 返回速查表](#toc)

---

### 4. 接口联调

<a id="q16"></a>

#### 问题 16：访问 8081 根路径返回 404

**现象**

浏览器打开 `http://localhost:8081` → `HTTP Status 404 – Not Found`。

**根因**

两段路径都没带：

```yaml
server:
  port: 8081
  servlet:
    context-path: /api      # ← 所有接口都在这个前缀下
```

```java
@GetMapping("/chat")        // ← 实际路径是 /api/chat
```

**解决方案**

访问完整路径。实测：

```powershell
# 根路径：404
Invoke-WebRequest http://localhost:8081/         # 404

# 正确路径：200 + 流式输出
curl -N http://localhost:8081/api/chat
```

```
HTTP/1.1 200
Content-Type: text/event-stream
Transfer-Encoding: chunked

data:Hello
data:! How can
...
```

> **关键认知**：**404 恰恰说明服务是活的**。端口不通时浏览器显示的是"无法访问此网站"，不是 404。

[↑ 返回速查表](#toc)

---

<a id="q17"></a>

#### 问题 17：浏览器端消费 SSE 必须用 EventSource

**现象**

技术选型要求用 Axios，但流式数据拿不到。

**根因**

- SSE 是 `text/event-stream` 长连接
- **浏览器端的 Axios 不支持消费流式响应**（Node 端的 `responseType: 'stream'` 在浏览器里没有对应能力）

**解决方案**

流式用原生 `EventSource`，但地址复用 Axios 实例的 `baseURL`，保证接口前缀只有一处配置：

```js
import http from './http'
import { CHAT_PATH } from '@/config'

export function buildChatUrl({ memoryId, message }) {
  const base = (http.defaults.baseURL || '').replace(/\/+$/, '')
  const query = new URLSearchParams({ memoryId: String(memoryId), message })
  return `${base}${CHAT_PATH}?${query.toString()}`
}
```

**必须处理的坑：EventSource 会自动重连**

后端 Flux 正常结束时连接关闭，EventSource 会当成异常并**自动重连**，导致同一条消息被重复请求。所以必须主动 `close()`：

```js
source.onerror = () => {
  source.close()          // ← 阻止自动重连
  onFinish?.(received)
}
```

**EventSource 的固有限制**（选型时要知道）：

| 限制 | 影响 |
|---|---|
| 只能 GET | 不能传复杂 body |
| 不能自定义 header | 不能带 `Authorization` |
| 断线自动重连 | 需要主动 close |

要突破这些限制，就得改用 `fetch` + `ReadableStream` 手动解析 SSE。

[↑ 返回速查表](#toc)

---

<a id="q18"></a>

#### 问题 18：`localhost:5173` 打不开

**现象**

浏览器访问前端地址，连不上。

**定位**

```powershell
Get-NetTCPConnection -LocalPort 5173 -State Listen   # → 没有监听
Get-Process node                                      # → 没有进程
```

**根因**

Vite 开发服务器没在运行。前端不是静态文件，**必须有一个 `npm run dev` 的服务在跑**。

顺带验证工程本身没问题（用 `node_modules` 里的 vite 手动启动）：

```
VITE v6.4.4  ready in 7656 ms
➜  Local:   http://localhost:5173/
```

访问首页 **HTTP 200**。

**解决方案**

```cmd
cd /d D:\JAVAcode\ai-code-helper-frontend
npm run dev
```

**这个终端窗口必须一直开着**，关掉或 `Ctrl + C` 服务就停。

三个常见坑：

| 现象 | 原因 |
|---|---|
| 关掉终端后页面打不开 | 服务随终端退出 |
| 启动 2 秒就刷新 | 首次要预构建依赖，实测约 7.6 秒 |
| 地址不是 5173 | 端口被占用，Vite 会自动换到 5174+，看终端输出 |

[↑ 返回速查表](#toc)

---

### 5. 前端实现

<a id="q19"></a>

#### 问题 19：`v-html` 渲染 AI 输出 = XSS 风险

**根因**

AI 回答需要渲染 Markdown（代码块、加粗），只能用 `v-html`。而 **模型输出永远不可信**，如果原样插入，`<script>`、`<img onerror=...>` 都会被执行。

**解决方案**

所有内容先做 HTML 转义，再拼接 Markdown 标签：

```js
const ESCAPE_MAP = {
  '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
}

function escapeHtml(text) {
  return String(text).replace(/[&<>"']/g, (ch) => ESCAPE_MAP[ch])
}
```

处理顺序很关键：**先抽出代码块占位 → 整体转义 → 再还原代码块**，这样代码里的 `<` 不会被误当成标签。

[↑ 返回速查表](#toc)

---

<a id="q20"></a>

#### 问题 20：流式追加内容，视图不更新

**根因**

```js
const aiMessage = { id, role: 'ai', content: '', streaming: true }
messages.value.push(aiMessage)

// 后面在回调里：
aiMessage.content += chunk     // ❌ 改的是原始对象，不是响应式代理
```

Vue 3 的 `ref` 是**深层响应式代理**：`messages.value.push(obj)` 存的是原始对象，后续通过局部变量改原对象**不会触发更新**。

**解决方案**

用 `reactive()` 包装后再 push：

```js
import { reactive } from 'vue'

const aiMessage = reactive({
  id: createMessageId(),
  role: 'ai',
  content: '',
  streaming: true,
  error: false
})
messages.value.push(aiMessage)

// 之后
aiMessage.content += chunk     // ✅ 触发更新
```

> 另一种写法：`const aiMessage = messages.value[messages.value.length - 1]`（拿到代理）。
> 但 `reactive()` 更直观，不容易出错。

[↑ 返回速查表](#toc)

---

## 四、排查方法论（这次真正学到的）

### 1. 永远先看最内层的 `Caused by`

Spring 的异常栈很长，但**根因永远在最底部**。上面 20 个问题里，有一大半光看第一行是看不出来的：

```
Failed to load ApplicationContext        ← 噪音
  Caused by: BeanCreationException       ← 噪音
    Caused by: BeanCreationException     ← 噪音
      Caused by: NoSuchBeanDefinitionException: EmbeddingStore   ← 真凶
```

### 2. 用数据说话，别靠猜

「启动慢」这种描述没法修。必须量化：

```
❌ "启动很慢"
✅ "154 个文本段的 embedding 花了 33.5 秒，占总耗时 95%"
```

拿到 `33.5s / 154 段` 这个数字，方案自然就出来了。

### 3. 对照实验：只改一个变量

「是返回 HTML 的问题，还是 URL 的问题？」—— 列个二维表：

| 假设 | 结果 |
|---|---|
| 只修返回值 | 能用，偶发超时 |
| 只修 URL | 依然 100 秒 |

结论立刻清晰：**返回体积决定可用性，URL 决定稳定性**。

### 4. 抓原始 HTTP，别信 UI

前端显示"没有流式效果"，到底是前端不会渲染，还是后端没流式？

**直接 curl**：

```powershell
curl -N http://localhost:8081/api/chat
```

看到 `Content-Type: text/event-stream` 和一块块 `data:` —— 立刻确定后端没问题，问题在前端。

### 5. 分层验证

```
模型/接口层  →  curl 裸接口，确认 API 本身正常
服务端层     →  curl 业务接口，确认参数、返回
浏览器层     →  DevTools Network 看请求与响应
UI 层        →  Vue Devtools 看数据流
```

每层单独验证，问题就无处藏身。

### 6. 别把「能跑」当成「没问题」

Maven wrapper 空了几十天没人发现，因为依赖早就在本地仓库里；一旦加新依赖就立刻暴雷。**隐藏的配置缺陷，总是在最不方便的时候出问题。**

---

## 五、遗留问题 / 待优化

| # | 问题 | 建议 |
|---|---|---|
| 1 | RAG 用相对路径 `src/main/resources/docs` | 换成绝对路径或 `ClassPathDocumentLoader` |
| 2 | `api-key` 在 yml 里硬编码 4 次 | 抽成 `${DASHSCOPE_API_KEY}` 环境变量，并**吊销当前已泄露的 key** |
| 3 | ~~向量库每次启动重灌（35 秒）~~ | ✅ **已解决**：PGVector 持久化 + `countVectors() == 0` 判空，实测 35.043s → 1.721s |
| 4 | 护轨 `contains("kill")` 误伤 `skill` | 改 `\bkill\b` 正则 |
| 5 | 工具返回是纯文本，模型拿不到题目链接 | 返回结构化文本（题目 + 难度 + 链接） |
| 6 | 前端没有错误重试 | SSE 断开时给出"重试"按钮 |
| 7 | ~~没有多轮会话持久化~~ | ✅ **已解决**：`chat_session` + `chat_message` 两张表 + MyBatis，会话可持久、可切换、可删除 |
| 8 | 窄屏（<860px）侧边栏被隐藏，无法切换会话 | 做成抽屉式侧边栏（汉堡按钮滑出） |
| 9 | 流式期间删会话，AI 回复可能不落库 | 后端 `doOnComplete` 依赖连接存活，可改为前端流结束后单独调保存接口 |

---

## 六、命令速查

### 后端

```powershell
cd /d D:\JAVAcode\ai-code-helper

# 编译（JDK 26 环境需要覆盖 lombok 版本）
mvn -B '-Dlombok.version=1.18.48' compile

# 跑测试
mvn -B '-Dlombok.version=1.18.48' test

# 只跑某个测试方法
mvn -B '-Dlombok.version=1.18.48' '-Dtest=AiCodeHelperServiceTest#chatWithMassage' test

# 启动
mvn '-Dlombok.version=1.18.48' spring-boot:run

# 打印完整依赖树里某类库
mvn dependency:tree '-Dincludes=dev.langchain4j:*'
```

### 前端

```cmd
cd /d D:\JAVAcode\ai-code-helper-frontend

npm install
npm run dev        # 开发服务器，必须保持终端开启
npm run build      # 生产构建，产物在 dist/
npm run preview    # 预览构建产物
```

### 联调验证

```powershell
# 看 SSE 原始输出（-N 关缓冲）
curl -N "http://localhost:8081/api/chat"

# 带参数（中文要 URL 编码）
curl -N -G `
  --data-urlencode "memoryId=your-db-password" `
  --data-urlencode "message=Java 集合常见面试题有哪些？" `
  "http://localhost:8081/api/ai/chat"

# 只看状态码
curl -s -o NUL -w "HTTP %{http_code}`n" "http://localhost:8081/api/ai/chat"

# 端口是否在监听
Get-NetTCPConnection -LocalPort 8081 -State Listen
```

### 生产部署（Nginx）

```nginx
server {
    listen 80;
    server_name localhost;

    root  D:/web/ai-code-helper-frontend/dist;
    index index.html;

    # 单页应用：刷新子路径不会 404
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 反向代理后端（等价于开发时的 Vite proxy）
    location /api/ {
        proxy_pass http://127.0.0.1:8081;      # 不要加路径，直接透传 URI
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;

        # SSE 三件套，少一条流式就废
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 300s;
    }
}
```

三个必踩的坑：

1. `proxy_pass` 后面**不要**加 `/api/`，否则路径变成 `/api/api/ai/chat`
2. 不配 `proxy_buffering off` → 前端会"卡很久然后一次性全部出现"
3. 不配 `try_files` → 刷新页面 404

---

## 七、第二阶段：会话记忆 + 前端侧边栏（2026-10-19）

> 上一阶段做到"单会话能聊"；这一阶段把它变成"左侧有历史、点开能续聊"的完整形态。
> 新增 `chat_session` / `chat_message` 两张表、MyBatis 持久层、REST 接口，以及前端侧边栏。
> 结果：问题 21～30，其中前端性能相关占了 3 个。

### 先记录三个架构决策

#### 决策 1：记忆持久化**不用** `ChatMemoryStore`，改用"回放构造"

一开始我打算实现 `ChatMemoryStore`（LangChain4j 官方给的持久化接口），后来发现它有坑：

```java
public interface ChatMemoryStore {
    List<ChatMessage> getMessages(Object memoryId);
    void updateMessages(Object memoryId, List<ChatMessage> messages);   // ← 问题在这
    void deleteMessages(Object memoryId);
}
```

`MessageWindowChatMemory` **每 add 一条消息，就会调一次 `updateMessages(memoryId, 整个窗口)`**。

如果 store 实现成"全量覆盖写"，那么数据库里永远只有最近 10 条，**更早的历史会被覆盖掉**——侧边栏的历史会越聊越短。

所以最终方案：

| 职责 | 谁来做 |
|---|---|
| 写库（完整历史，append-only） | 业务层 `saveUserMessage` / `saveAiMessage` |
| 记忆回放（取最近 N 条塞进窗口） | `loadChatMemory()`，由 `chatMemoryProvider` 调用 |

```java
public ChatMemory loadChatMemory(Object memoryId) {
    long sessionId = ((Number) memoryId).longValue();
    ChatMemory memory = MessageWindowChatMemory.withMaxMessages(maxMessages);
    messageRecordMapper.selectRecent(sessionId, maxMessages)
            .forEach(record -> memory.add(toLangChainMessage(record)));
    return memory;
}
```

好处：**同一个 sessionId 只回放一次**（`ChatMemoryService` 内部会缓存），之后完全走内存，写库与回放职责分离。

#### 决策 2：两张表而不是一张

| | 两张表（session + message） | 一张表（只存消息） |
|---|---|---|
| 新建空会话 | ✅ 直接插一行 | ❌ 没法表示 |
| 会话列表查询 | ✅ `SELECT * FROM chat_session` | ❌ 要 `GROUP BY session_id` 扫全表 |
| 标题存储 | ✅ 存一次 | ❌ 每条消息冗余一份 |
| 删除会话 | ✅ `ON DELETE CASCADE` | ⚠️ 手动删 |

#### 决策 3：「新对话」懒创建

点「添加新对话」时**不调接口**，等用户真正发出第一句话才 `POST /chat/session`。
否则用户点几次就攒几条空会话，列表里全是「新会话」。

---

<a id="q21"></a>

#### 问题 21：新建会话返回的 `createdAt` / `updatedAt` 是 null

**现象**

```json
{ "id": 3, "title": "新会话", "createdAt": null, "updatedAt": null }
```

**根因**

```java
ChatSession session = ChatSession.builder().title("新会话").build();
chatSessionMapper.insert(session);   // 只回填了 id
return session;                      // 时间字段从来没被赋值
```

`created_at` 的值是数据库 `DEFAULT NOW()` 生成的——**只有 DB 那一行有值，Java 内存对象里是 null**。

**解决**

两个思路，最后选了"干脆不返回"：

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatSession { ... }
```

null 字段直接从 JSON 里消失，返回变成 `{ "id": 4, "title": "新会话" }`，前端建完会话本来就会重新拉列表拿时间，不需要这个字段。

> 也考虑过 `INSERT ... RETURNING id, title, created_at, updated_at` 一条语句拿全，但为了少一个字段增加 SQL 复杂度不划算。
> 另外**不要在 Java 里 `LocalDateTime.now()` 赋值**：虚拟机时区可能是 UTC，Java 是本机 +08:00，两处写时间会让 `ORDER BY updated_at` 排序错乱。**让数据库一个人说了算。**

[↑ 返回速查表](#toc)

---

<a id="q22"></a>

#### 问题 22：`Injection of resource dependencies failed`（其实是数据库没开）

**现象**

```
BeanCreationException: Error creating bean with name 'aiCodeHelperServiceFactory':
Injection of resource dependencies failed
```

栈很长，但完全看不出跟数据库有关。

**根因**

抓最内层 `Caused by` 才看到：

```
Connection refused: getsockopt
  ← Connection to your-db-host:5432 refused
Caused by: RuntimeException: Failed to execute 'init'      ← PgVectorEmbeddingStore 建表
Caused by: 创建 embeddingStore 失败
Caused by: 创建 contentRetriever 失败
Caused by: aiCodeHelperServiceFactory 的 @Resource 注入失败   ← 最外层看到的那条
```

**虚拟机里的 PostgreSQL 没启动**，`embeddingStore` 建连失败，异常一路冒上来，最外层就显示成"注入失败"。

**解决**

先验证连通性，再启动应用：

```powershell
Test-NetConnection your-db-host -Port 5432
```

`TcpTestSucceeded : True` 才继续。

**教训**：Spring 最外层的报错往往离根因隔着 4～5 层，**永远先看最底部的 `Caused by`**。

[↑ 返回速查表](#toc)

---

<a id="q23"></a>

#### 问题 23：`systemctl start postgresql` 报 Unit not found / Access denied

**现象**

```bash
[itheima@centos ~]$ systemctl status postgresql
Unit postgresql.service could not be found.
[itheima@centos ~]$ systemctl start postgresql
Failed to start postgresql.service: Access denied
```

**根因有两个**

1. **服务名不对**——CentOS 上 yum 装的通常带版本号（`postgresql-16`）；如果是 **Docker 跑的，压根没有 systemd 服务**
2. **权限不足**——当前是普通用户 `itheima`，启动服务要 `sudo`

**解决**

先确认是哪种装法：

```bash
docker ps -a | grep -i pg              # 情况 A：Docker
sudo systemctl list-unit-files | grep -i postgres   # 情况 B：本地安装
```

| 情况 | 启动命令 |
|---|---|
| Docker | `docker start pgvector`（容器名按实际） |
| 本地安装 | `sudo systemctl start postgresql-16` |

启动后**必须确认监听地址**：

```bash
ss -lntp | grep 5432
```

要看到 **`0.0.0.0:5432`**，如果是 `127.0.0.1:5432`，Windows 那边连不进来，需要改 `postgresql.conf` 的 `listen_addresses = '*'`。

[↑ 返回速查表](#toc)

---

<a id="q24"></a>

#### 问题 24：`不兼容的类型: 从 long 转换到 int 可能会有损失`

**现象**

```
Aicontroller.java:[40,47] 不兼容的类型: 从 long 转换到 int 可能会有损失
```

**根因**

会话 id 是 `bigserial`（Java 侧 `Long`），所以 Controller 参数改成了 `long memoryId`；
但 `AiCodeHelperService` 接口里还是 `@MemoryId int memoryId`，两边类型对不上。

**解决**

把接口里**所有**带 `@MemoryId` 的方法统一改成 `long`：

```java
String chat(@MemoryId long memoryId, @UserMessage String userMessage);
Report chatForReport(@MemoryId long memoryId, @UserMessage String userMessage);
Result<String> chatWithRag(@MemoryId long memoryId, @UserMessage String userMessage);
Flux<String> chatStream(@MemoryId long memoryId, @UserMessage String userMessage);
```

**教训**：改一个参数类型，要顺着调用链一路检查——**编译错误是好事，运行时的类型错误才要命**。

[↑ 返回速查表](#toc)

---

<a id="q25"></a>

#### 问题 25：Apifox 调会话详情接口一直 404

**现象**

```
GET http://localhost:8081/api/chat/session/{5}
→ 404，响应体里 "path": "/api/chat/session/"
```

**根因**

看响应里的 `path` 就明白了——**结尾是空的**。

Apifox 的 Path 参数表格有两列：`参数名` 和 `参数值`。把 `5` 填到了「参数名」列，而「参数值」列空着，所以拼出来是 `/api/chat/session/`。

**解决**

两种都行：

- URL 直接写死：`/api/chat/session/5`（不要花括号）
- 或者保留 `{id}`，在**参数值**列填 `5`

**顺带一个认知**：这个接口对"不存在的 id"返回的是 `200 + []`，不是 404。
所以真正的 404 只可能是**路径没匹配上**，不要被误导。

[↑ 返回速查表](#toc)

---

<a id="q26"></a>

#### 问题 26：DELETE 接口返回 200 却显示"失败"

**现象**

Apifox 里 `DELETE /api/chat/session/2` → 状态码 **200（绿色）**，但断言区红字：

> ❌ 返回的数据格式应当是 JSON

**根因**

接口返回 `void`，响应体是空的，Apifox 的「校验响应」规则判定它"不是 JSON"。

**接口其实完全成功**，是测试工具的断言在抱怨。

**解决**

| 方案 | 做法 | 适合 |
|---|---|---|
| A | 关掉 Apifox 那条校验 | 想快速继续测 |
| B | 让后端返回一段 JSON | **推荐**，前端好处理 |

```java
@DeleteMapping("/session/{id}")
public Map<String, Object> deleteSession(@PathVariable Long id) {
    chatHistoryService.deleteSession(id);
    return Map.of("id", id, "deleted", true);
}
```

**顺带发现的隐患**：删一个不存在的 id 也返回 200（影响行数 0 也算成功）。想严谨可以让 Service 返回 `deleteById(...) > 0`，把真实结果透出去。

[↑ 返回速查表](#toc)

---

<a id="q27"></a>

#### 问题 27：Vite 开发服务器突然崩溃 `EBUSY`

**现象**

```
Error: EBUSY: resource busy or locked, watch
'...\src\api\.chat.js.24028.xxx.tmpdir\chat.js.tmp'
Node.js v24.21.0
```

整个 dev server 直接退出。

**根因**

工具/编辑器保存文件时用的是**原子写**：先在目标目录里建一个 `.<文件名>.<pid>.xxxx.tmpdir` 临时目录，写完再重命名。

Vite 的文件监听器把 `src` 下所有新目录都纳入监听，而这个 `.tmpdir` 里的文件**还处于被 Windows 锁定状态**，`fs.watch` 就抛 `EBUSY`，未捕获 → 进程退出。

**解决**

在 `vite.config.js` 里忽略这类临时文件：

```js
server: {
  watch: {
    ignored: ['**/.*.tmpdir/**', '**/*.tmp']
  }
}
```

**教训**：Windows 上任何"文件监听 + 原子写"的组合都容易踩这个坑（Vite / Webpack / nodemon 都有类似问题），
**忽略临时文件是通用解法**。

[↑ 返回速查表](#toc)

---

<a id="q28"></a>

#### 问题 28：AI 回复里的 `###` 和 `-` 原样显示出来

**现象**

聊天区里看到的是：

```
### 阶段 4：企业开发进阶
- 目标：能够独立开发架构合理的系统。
- 软件工程
```

标题符号和列表符号全裸露着。

**根因**

自己写的轻量渲染器 [markdown.js](file:///D:/JAVAcode/ai-code-helper-frontend/src/utils/markdown.js) 只实现了 4 种语法：

| 已支持 | 未支持 |
|---|---|
| ` ```代码块``` ` | `#` 标题 |
| `` `行内代码` `` | `- ` 无序列表 |
| `**加粗**` | `1. ` 有序列表 |
| 换行 | `[文字](url)` 链接 |

模型输出结构化内容时，一半语法认不出来，就露出原始符号。

**解决**

把"整串正则替换"改成**逐行解析块级元素**：

```js
for (const rawLine of text.split('\n')) {
    const line = rawLine.trim()
    // 标题 / 分隔线 / 无序列表 / 有序列表 / 普通段落，逐条匹配
}
```

改为逐行解析后，才能正确处理"列表项要包在同一个 `<ul>` 里"这种跨行结构（这是整串正则做不到的）。

**安全没变**：所有文本仍然**先 `escapeHtml` 再套标签**，链接只放行 `http(s)://`，`javascript:` 之类会被当纯文本。

[↑ 返回速查表](#toc)

---

<a id="q29"></a>

#### 问题 29：流式输出时页面卡顿掉帧

**现象**

回答一长，输出过程中页面明显一顿一顿，滚动鼠标也发涩。

**根因（一共 5 个，叠加导致）**

| # | 原因 | 为什么卡 |
|---|---|---|
| 1 | **每个 chunk 都全量重算 Markdown** | 一次回答几百个 chunk，`renderMarkdown` 是 O(n)，总体 **O(n²)**，越到后面越慢 |
| 2 | `scroll-behavior: smooth` | 每个 chunk 都执行 `scrollTop = scrollHeight`，每次都会启动一段平滑滚动动画，互相打断 |
| 3 | 背景角色 `animation: infinite` | 680px 的大元素持续动画，长期占用合成线程 |
| 4 | `backdrop-filter: blur()` | 毛玻璃要实时重算背后内容，大面积元素上对集显很吃力 |
| 5 | 滚动不节流 | 一次内容更新里可能触发多次 layout |

**解决**

**① 批量刷新（最关键的优化）**

```js
// 攒 50ms 统一刷一次，渲染次数从"几百次"降到 ~20 次/秒
let chunkBuffer = ''
onChunk(chunk) {
  chunkBuffer += chunk
  if (!flushTimer) {
    flushTimer = setTimeout(() => {
      flushTimer = null
      aiMessage.content += chunkBuffer
      chunkBuffer = ''
    }, 50)
  }
}
```

**② 去掉 `scroll-behavior: smooth`**，滚动改为"同帧只滚一次"的节流实现。

**③ 去掉背景角色的无限动画**，改静态。

**④ `backdrop-filter` 移除**，背景从 `rgba(255,255,255,.84)` 提到 `.92`，视觉几乎无差。

**⑤ 渲染次数降下来后，`v-html` 的 DOM 重建次数同比减少一个数量级。**

> **通用原则**：流式渲染千万不要"收到一块就全量重算一次"。
> 正确的做法是**攒一小段时间批量刷新**，把渲染频率和 token 到达频率解耦。

[↑ 返回速查表](#toc)

---

<a id="q30"></a>

#### 问题 30：滚轮不同步、有延迟

**现象**

鼠标移动很顺，但用滚轮往上看历史时，"滚不动""被拉回去""跟手有延迟"。

**根因（两个独立问题）**

**① 自动滚动和用户滚动打架**

流式输出时每来一批内容就执行 `scrollTop = scrollHeight`。
用户往上滚一下，下一批内容立刻把视口拉回底部 —— **一拉一回，就是"不同步"**。

**② `backdrop-filter` 让滚动每帧重算模糊**

滚动容器在毛玻璃元素内部时，每滚动一帧浏览器都要重新计算背景模糊，滚轮跟手感变差。

**解决**

**① 加"跟随底部"状态机**

```js
const autoScroll = ref(true)

function handleScroll() {
  const el = bodyRef.value
  const distance = el.scrollHeight - el.scrollTop - el.clientHeight
  autoScroll.value = distance < 48        // 距底部 48px 内才算"跟随中"
}

function scrollToBottom(force = false) {
  if (!force && !autoScroll.value) return  // 用户上滚了，就别拽他
  ...
}
```

行为矩阵：

| 场景 | 表现 |
|---|---|
| 停在底部看输出 | 继续自动跟随 |
| 向上滚看历史 | **立即停止跟随**，不再被拽走 |
| 滚回底部 | 自动恢复跟随 |
| 自己发消息 | 强制滚到底（`force = true`） |
| 切换会话 | 强制滚到底 |

**② 移除两处 `backdrop-filter`**（`.chat-card` / `.sidebar`），背景改成更实的 `rgba(255,255,255,.9)`。

[↑ 返回速查表](#toc)

---

### 一段自我纠错

排查记忆功能时，我看到这行就下了结论"记忆是坏的"：

```java
.chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
```

理由：Lambda 每次都 `new` 一个，所以每轮对话都是空白记忆。

**这个判断是错的。** 反编译 `langchain4j` 才看到：

```
AiServiceContext.chatMemoryService  →  ChatMemoryService
ChatMemoryService {
    private Map<Object, ChatMemory> chatMemories;      ← 按 memoryId 缓存的 Map
    public ChatMemory getOrCreateChatMemory(Object id) ← 先查 Map，没有再调 provider
}
```

**provider 是"工厂"，不是"每次调用"**。同一个 memoryId 第二次进来直接拿缓存实例，记忆是正常工作的。

> **教训**：由 API 名字推断行为很危险。
> `xxxProvider` 这个命名暗示了"提供者"而不是"每次创建"，但真正的答案只能去源码/字节码里确认。
> 我当时应该先跑一个"两轮对话问名字"的实测，而不是直接下结论。

---

## 八、一句话总结

> 这个项目真正教会我的不是 LangChain4j 怎么用，而是：
> **任何「明明配了却不对」的问题，答案都在「你以为生效了、其实没生效」的地方。**
> 配置写错位置、注解漏了角色、Bean 没人提供、参数被丢弃、空格被规范吃掉 ——
> 全都是"看起来对，实际没生效"。**唯一的解法是分层验证 + 拿数据说话。**
