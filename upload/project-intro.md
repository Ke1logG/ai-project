# AI 学习项目介绍

## 项目概览

这是一个用于学习 Spring AI 的 Java demo 项目，包名为 com.kl.aiproject。项目没有业务逻辑，每个功能对应一个 HTTP 测试接口，覆盖同步对话、流式输出、提示词模板、结构化输出、对话记忆、工具调用、文本向量化、相似度检索，以及基于文档的 RAG 问答。代码按 git 提交逐步演进，目前处于学习阶段。

启动前必须设置环境变量 ALI-API-KEY，application.yaml 通过 ${ALI-API-KEY} 引用它。变量缺失时，应用和 @SpringBootTest 都会在上下文初始化阶段失败。

## 技术栈与依赖

项目使用 JDK 21 和 Maven 构建。父工程是 spring-boot-starter-parent，版本 4.1.1；Spring AI 版本是 2.0.1，通过 spring-ai-bom 统一管理。

pom.xml 中直接引入的依赖有五个：

- spring-boot-starter-webmvc：提供 Web 和 REST 能力
- spring-ai-starter-model-openai：接入 OpenAI 兼容协议的对话和向量模型
- spring-ai-vector-store：提供 SimpleVectorStore 和相似度检索能力
- spring-ai-vector-store-advisor：提供 QuestionAnswerAdvisor，用于 RAG
- spring-ai-markdown-document-reader：提供 MarkdownDocumentReader

Document、TextReader、TokenTextSplitter 等类由 spring-ai-model 和 spring-ai-commons 传递引入，不需要单独声明。项目没有引入 spring-ai-tika-document-reader，因此不能解析 pdf、doc、docx 文件。

## 模型与配置

配置文件是 src/main/resources/application.yaml。

- API Key 来自环境变量 ALI-API-KEY，没有硬编码在代码里
- base-url 是 https://maas.qianwenaiapi.com/compatible-mode/v1
- 对话模型是 qwen3.8-flash
- 向量模型是 qwen3.7-text-embedding-flash
- spring.ai.model 下的 image、moderation、audio.speech、audio.transcription 都设置为 none，避免创建用不到的模型 Bean
- 应用名是 ai-project
- org.springframework.ai 包的日志级别设置为 debug

服务没有配置 server.port，使用 Spring Boot 默认的 8080 端口。

## 代码结构

com.kl.aiproject 包下分为以下几部分：

- AiProjectApplication：Spring Boot 启动类
- config/AiConfiguration：集中定义 ChatClient、ChatClient2、ragChatClient、ChatMemory、VectorStore 五个 Bean
- config/TextSplitterConfiguration：定义 TokenTextSplitter Bean
- controller/HelloController：对话、提示词、结构化输出、记忆、工具调用的测试接口
- controller/MyEmbController：向量化、向量入库、相似度检索的测试接口
- controller/RagController：文档入库和 RAG 问答接口
- util/DocumentParseUtil：按文件后缀选择 DocumentReader
- advisor/MyLoggerAdvisor：自定义日志 Advisor，目前没有注册到任何 ChatClient
- tool/DateTimeTools：提供获取当前时间的工具

## AiConfiguration 中的 Bean

chatClient：基础 ChatClient，注册了 SimpleLoggerAdvisor。Bean 方法参数里注入了 ChatMemory，但 MessageChatMemoryAdvisor 的注册代码被注释掉，因此这个 ChatMemory 实际没有生效。

chatClient2：带系统提示词的 ChatClient，系统提示词要求扮演资深 Java 开发工程师，只回答 Java 相关问题，回答要简洁易懂、不能太长。它也注册了 SimpleLoggerAdvisor。

ragChatClient：RAG 专用 ChatClient，注册了 QuestionAnswerAdvisor。检索参数 topK 为 5；similarityThreshold 被注释掉，使用默认值 0.0，即不过滤低分结果。

chatMemory：MessageWindowChatMemory，窗口大小为 10 条消息，底层使用 InMemoryChatMemoryRepository。

vectorStore：SimpleVectorStore，使用注入的 EmbeddingModel 构建，数据保存在内存中。

三个 ChatClient 类型相同，注入时字段名必须与 Bean 名一致，或者显式使用 @Qualifier。

## HelloController 的接口

- /ai：同步阻塞调用，返回完整回答字符串
- /ai2：流式输出，返回 Flux，响应类型为 text/html，适合在浏览器里看逐字输出
- /ask：使用 PromptTemplate 构造“介绍下{topic}”提示词，再流式输出
- /ask2：使用链式 user API 和 system 提示词，流式输出，system 是“你是一个专业的书评助手”
- /ask3：调用 chatClient2，测试系统提示词对回答范围的限制
- /ask4：结构化输出，把回答映射为 TopicBook 对象
- /ask5：结构化输出，把回答映射为 BookReview 列表，使用 ParameterizedTypeReference
- /ai3：通过 advisors 传入 conversationId，用于测试多用户会话隔离；参数名是大写的 Id
- /ai4：通过 tools 把 DateTimeTools 交给模型，让模型自己决定何时调用获取时间的工具

## MyEmbController 的接口

- /emb：直接调用 OpenAI 兼容的向量模型，对 Hello, world! 做向量化，在控制台打印向量维度和内容
- /addDoc：往 SimpleVectorStore 中写入三条测试文档，分别是《无职转生》、日本动画、去旅游
- /query：构造 SearchRequest，查询词是“好看的日本动画有什么”，topK 为 2，在控制台打印检索结果，接口本身返回 ok

## RagController 的接口

- /addDocs：把 upload/project-intro.md 和 upload/project-runbook.txt 依次解析、切块并写入向量库。文件路径硬编码为 D:/AI-project/ai-project/upload/...
- /query2：手动构造 SearchRequest，query 为用户问题，topK 为 5，调用 similaritySearch 并返回检索结果
- /ask_rag：调用 ragChatClient，由 QuestionAnswerAdvisor 自动检索并把上下文拼进用户消息，再让对话模型生成回答

## RAG 数据流

upload 下的 md 和 txt 文件先由 DocumentParseUtil 解析成 Document 列表，再由 TokenTextSplitter 切分成块，最后通过 SimpleVectorStore.add 逐块调用向量模型写入内存向量库。

用户调用 /ask_rag 时，QuestionAnswerAdvisor 用用户问题构造 SearchRequest，topK 为 5，similarityThreshold 使用默认值 0.0。检索到的 Document 文本用换行拼接成 question_answer_context，套进 Advisor 的默认提示词模板并替换原来的用户消息，再交给对话模型。默认模板要求模型只根据上下文回答，上下文里没有答案时说明无法回答。

Advisor 只拼接 Document 的文本，不拼接 metadata；MarkdownDocumentReader 写进 metadata 的 title 不会出现在上下文里。

/query2 是手动检索示例，直接调用 similaritySearch，只返回检索结果，不经过模型。

## 文档解析与文本切分

DocumentParseUtil 取文件路径最后一个点后面的部分作为后缀并转成小写。txt 和 text 使用 TextReader；md 使用 MarkdownDocumentReader，配置为 defaultConfig；其他后缀直接抛出 IllegalArgumentException，提示不支持的文件格式。注意后缀 markdown 并不在支持列表里。

TextReader 默认使用 UTF-8 编码，把整个文件读成一个 Document，metadata 中包含 charset 和 source，source 的值是文件名。如果遇到 GBK 编码的中文 txt，需要调用 setCharset 指定编码。

MarkdownDocumentReader 按标题切分，每个标题下的内容生成一个 Document。标题文字不会进入 Document 正文，而是写入 metadata 的 title 和 category（category 形如 header_1、header_2）。默认配置下，水平分割线不切分文档，代码块和引用块不并入正文段落，列表项和换行会被转换成空格。

TokenTextSplitter Bean 的 chunkSize 是 500 个 token，maxNumChunks 是 1000。其余参数使用默认值：minChunkSizeChars 为 350，minChunkLengthToEmbed 为 5 个字符，keepSeparator 为 true，标点只有英文句号、问号、感叹号和换行，编码是 cl100k_base。切分以 token 为单位，中文大致一个字对应一到两个 token；只有剩余 token 超过 chunkSize 时才会尝试按标点截断。

每个切出来的块都会生成新的 UUID，继承原 Document 的 metadata，并额外增加 parent_document_id、chunk_index、total_chunks 三个字段。

## Advisor 与工具

MyLoggerAdvisor 同时实现 CallAdvisor 和 StreamAdvisor。同步调用时，它会在请求前后各打印一次；流式调用时，它会订阅响应流并逐条打印。它的名字是“日志advisor”，getOrder 返回 0，目前没有注册到任何 ChatClient。

DateTimeTools 标注了 @Component，方法 getCurrentTime 标注了 @Tool 注解，描述是获取用户在指定时区的当前日期和时间。方法内部通过 LocaleContextHolder 获取时区，返回格式为 yyyy-MM-dd HH:mm:ss。只有 /ai4 显式传入了这个工具。

## 数据文件

upload/project-intro.md 和 upload/project-runbook.txt 是 RAG 演示用的语料，内容互补：md 偏接口和类说明，txt 偏运行方式、配置和已知问题。修改这两个文件后，需要重新调用 /addDocs 才会生效；应用重启后内存向量库清空，也需要重新入库。

## 已知状态与限制

- MessageChatMemoryAdvisor 被注释掉，/ai3 虽然传了 conversationId，实际没有记忆效果
- MyLoggerAdvisor 已经写好，但没有注册到任何 ChatClient
- SimpleVectorStore 是内存实现，应用重启后数据会丢失
- 重复调用 /addDocs 会重复入库，不会覆盖旧数据
- 项目追求最小可运行示例，没有做异常处理、鉴权和完整的测试用例
