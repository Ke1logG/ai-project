# AI 学习项目介绍

## 项目概览

这是一个用于学习 Spring AI 的 Java demo 项目，包名为 com.kl.aiproject。项目不包含业务逻辑，每个功能都有一个独立的 HTTP 测试接口，覆盖同步对话、流式输出、提示词模板、结构化输出、对话记忆、工具调用、文本向量化和向量检索等基础能力。

项目当前处于学习阶段，代码按功能逐步演进，还没有接入真实业务数据。

## 技术栈与依赖

项目使用 JDK 21 和 Maven 构建，父工程是 spring-boot-starter-parent，版本 4.1.1。Spring AI 的版本是 2.0.1，通过 spring-ai-bom 统一管理。

pom.xml 中直接引入的依赖有四个：

- spring-boot-starter-webmvc：提供 Web 和 REST 能力
- spring-ai-starter-model-openai：接入 OpenAI 兼容协议的对话和向量模型
- spring-ai-vector-store：提供 SimpleVectorStore 和相似度检索能力
- spring-ai-markdown-document-reader：提供 MarkdownDocumentReader

另外，spring-ai-commons 通过 spring-ai-model 传递引入，TextReader、TokenTextSplitter、Document 等类都在这里面，不需要单独声明依赖。

项目没有引入 spring-ai-tika-document-reader，因此不能解析 pdf、doc、docx 文件。

## 模型与配置

配置文件是 src/main/resources/application.yaml。

- API Key 来自环境变量 ALI-API-KEY，没有硬编码在代码里
- base-url 是 https://maas.qianwenaiapi.com/compatible-mode/v1
- 对话模型是 qwen3.8-flash
- 向量模型是 qwen3.7-text-embedding-flash
- image、moderation、audio 三类自动配置都设置为 none，避免创建用不到的模型 Bean
- 应用名是 ai-project
- org.springframework.ai 包的日志级别设置为 debug

服务没有配置 server.port，使用 Spring Boot 默认的 8080 端口。

## 代码结构

com.kl.aiproject 包下分为以下几部分：

- AiProjectApplication：Spring Boot 启动类
- config/AiConfiguration：集中定义 ChatClient、ChatClient2、ChatMemory、VectorStore 四个 Bean
- controller/HelloController：对话、提示词、结构化输出、记忆、工具调用的测试接口
- controller/MyEmbController：向量化、向量入库、相似度检索的测试接口
- advisor/MyLoggerAdvisor：自定义日志 Advisor
- tool/DateTimeTools：提供获取当前时间的工具

## AiConfiguration 中的 Bean

chatClient：基础 ChatClient，注册了 SimpleLoggerAdvisor。MessageChatMemoryAdvisor 的注册代码目前被注释掉了。

chatClient2：带系统提示词的 ChatClient，系统提示词要求扮演资深 Java 开发工程师，只回答 Java 相关问题，回答要简洁易懂、不能太长。它也注册了 SimpleLoggerAdvisor。

chatMemory：MessageWindowChatMemory，窗口大小为 10 条消息，底层使用 InMemoryChatMemoryRepository。

vectorStore：SimpleVectorStore，使用注入的 EmbeddingModel 构建，数据保存在内存中。

## HelloController 的接口

- /ai：同步阻塞调用，返回完整回答字符串
- /ai2：流式输出，返回 Flux，响应类型为 text/html，适合在浏览器里看逐字输出
- /ask：使用 PromptTemplate 构造提示词，再流式输出
- /ask2：使用链式 user API，带 system 提示词，流式输出
- /ask3：调用 chatClient2，测试系统提示词对回答范围的限制
- /ask4：结构化输出，把回答映射为 TopicBook 对象
- /ask5：结构化输出，把回答映射为 BookReview 列表，使用 ParameterizedTypeReference
- /ai3：通过 advisors 传入 conversationId，用于测试多用户会话隔离
- /ai4：通过 tools 把 DateTimeTools 交给模型，让模型自己决定何时调用获取时间的工具

## MyEmbController 的接口

- /emb：直接调用 OpenAI 兼容的向量模型，对 Hello world 文本做向量化，打印向量维度和内容
- /addDoc：往 SimpleVectorStore 中写入三条测试文档，分别是《无职转生》、日本动画、去旅游
- /query：构造 SearchRequest，查询词是“好看的日本动画有什么”，topK 为 2，打印检索结果

## Advisor 与工具

MyLoggerAdvisor 同时实现 CallAdvisor 和 StreamAdvisor。同步调用时，它会在请求前后各打印一次；流式调用时，它会订阅响应流并逐条打印。它的名字是“日志advisor”，getOrder 返回 0。

DateTimeTools 标注了 @Component，方法 getCurrentTime 标注了 @Tool 注解，描述是获取用户在指定时区的当前日期和时间。方法内部通过 LocaleContextHolder 获取时区，返回格式为 yyyy-MM-dd HH:mm:ss。

## 已知状态与限制

- MessageChatMemoryAdvisor 被注释掉，所以 /ai3 虽然传了 conversationId，实际没有记忆效果
- MyLoggerAdvisor 已经写好，但没有注册到任何 ChatClient 上
- spring-ai-markdown-document-reader 依赖已引入，在 RAG 模块加入之前还没有使用
- SimpleVectorStore 是内存实现，应用重启后数据会丢失
- 项目追求最小可运行示例，没有做异常处理、鉴权和完整的测试用例
