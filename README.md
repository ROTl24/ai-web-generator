# AI Web Generator

> 基于 AI 大模型的智能 Web 应用代码生成平台，通过自然语言对话即可生成、迭代和部署完整的 Web 应用。

## 项目简介

AI Web Generator 是一个全栈 Web 应用，用户只需通过自然语言描述需求，AI 即可自动生成对应的 Web 应用代码。系统集成了 LangChain4j 和 LangGraph4j，支持多种代码生成模式，并提供流式响应（SSE）、版本管理、一键部署等能力。

![b27a394ab1fc9034c25710605747e227](E:\WeChat\xwechat_files\wxid_4sl5dw1kwwv212_f364\temp\RWTemp\2026-02\9e20f478899dc29eb19741386f9343c8\b27a394ab1fc9034c25710605747e227.png)



![d2cee411f15b48280552c0a5d5dfee78](E:\WeChat\xwechat_files\wxid_4sl5dw1kwwv212_f364\temp\RWTemp\2026-02\9e20f478899dc29eb19741386f9343c8\d2cee411f15b48280552c0a5d5dfee78.png)

![26b9150b9785101b3bb91f99a8ebccf3](E:\WeChat\xwechat_files\wxid_4sl5dw1kwwv212_f364\temp\RWTemp\2026-02\9e20f478899dc29eb19741386f9343c8\26b9150b9785101b3bb91f99a8ebccf3.png)

### 核心特性

- **自然语言驱动**：通过对话式交互描述需求，AI 自动理解并生成代码
- **多种生成模式**：支持原生 HTML、原生多文件、Vue 工程三种代码生成类型
- **智能工作流**：基于 LangGraph4j 构建代码生成工作流，包含图片采集、提示词增强、代码生成、质量检查、工程构建等节点
- **流式响应**：支持 SSE（Server-Sent Events）流式输出，实时展示 AI 生成过程
- **版本管理**：每次代码生成自动创建版本快照，支持版本回溯
- **一键部署**：生成的应用可通过唯一部署标识直接访问
- **AI 工具调用**：支持文件读写、修改、删除等工具调用，实现 AI Agent 自主操作项目文件
- **安全防护**：内置 Prompt 安全输入防护栏和重试输出防护栏

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 21 | 编程语言 |
| Spring Boot | 3.4.1 | 应用框架 |
| LangChain4j | 1.11.0 | AI 大模型集成框架 |
| LangGraph4j | 1.6.0-rc2 | AI 工作流编排框架 |
| DashScope SDK | 2.21.1 | 阿里云 AI 大模型 SDK |
| MyBatis-Flex | 1.11.0 | ORM 框架 |
| MySQL | - | 关系型数据库 |
| Redis | - | 会话存储 / AI 记忆存储 |
| Redisson | 3.50.0 | 分布式锁 / 限流 |
| Selenium | 4.33.0 | 网页截图 |
| Tencent COS | 5.6.227 | 对象存储 |
| Knife4j | 4.4.0 | API 文档 |
| Caffeine | - | 本地缓存 |
| Hutool | 5.8.38 | Java 工具库 |

### 前端

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.5 | 前端框架 |
| TypeScript | 5.9 | 编程语言 |
| Vite | 7.3 | 构建工具 |
| Ant Design Vue | 4.2 | UI 组件库 |
| Pinia | 3.0 | 状态管理 |
| Vue Router | 4.6 | 路由管理 |
| Axios | 1.13 | HTTP 客户端 |
| Markdown-it | 14.1 | Markdown 渲染 |
| Highlight.js | 11.11 | 代码高亮 |

## 项目架构

```
xiaolu_ai-code/
├── src/main/java/com/uloaix/xiaolu_aicode/
│   ├── ai/                          # AI 服务层
│   │   ├── AiCodeGeneratorService       # AI 代码生成服务
│   │   ├── AiVueProjectCreateService    # Vue 项目创建服务
│   │   ├── AiVueProjectModifyService    # Vue 项目修改服务
│   │   ├── guardrail/                   # AI 安全防护栏
│   │   ├── model/                       # AI 消息模型
│   │   └── tools/                       # AI 工具（文件读写、修改、删除等）
│   ├── langgraph4j/                 # LangGraph4j 工作流
│   │   ├── ai/                          # 工作流 AI 服务
│   │   ├── node/                        # 工作流节点
│   │   │   ├── CodeGeneratorNode        # 代码生成节点
│   │   │   ├── CodeQualityCheckNode     # 代码质量检查节点
│   │   │   ├── ImageCollectorNode       # 图片采集节点
│   │   │   ├── ProjectBuilderNode       # 工程构建节点
│   │   │   ├── PromptEnhancerNode       # 提示词增强节点
│   │   │   └── RouterNode               # 路由节点
│   │   ├── state/                       # 工作流状态
│   │   └── tools/                       # 工作流工具
│   ├── core/                        # 核心业务逻辑
│   │   ├── AiCodeGeneratorFacade        # AI 代码生成门面（统一入口）
│   │   ├── parser/                      # 代码解析器
│   │   ├── saver/                       # 代码保存器
│   │   └── builder/                     # 项目构建器
│   ├── controller/                  # API 控制器
│   ├── service/                     # 业务服务层
│   ├── model/                       # 数据模型
│   ├── config/                      # 配置类
│   ├── ratelimiter/                 # 限流模块
│   └── exception/                   # 异常处理
├── xiaolu_ai-code-frontend/         # 前端项目
│   └── src/
│       ├── api/                         # API 接口定义
│       ├── components/                  # 通用组件
│       ├── pages/                       # 页面
│       │   ├── HomePage.vue             # 首页
│       │   ├── app/                     # 应用相关页面
│       │   │   ├── AppChatPage.vue      # AI 对话生成页
│       │   │   └── AppEditPage.vue      # 应用编辑页
│       │   ├── admin/                   # 管理后台页面
│       │   └── user/                    # 用户相关页面
│       ├── layouts/                     # 布局组件
│       ├── router/                      # 路由配置
│       └── stores/                      # 状态管理
└── sql/                             # 数据库初始化脚本
```

## AI 工作流

系统基于 LangGraph4j 构建了完整的代码生成工作流：

```
┌─────────────┐
│    START     │
└──────┬──────┘
       │
       ▼
┌──────────────────┐
│  Image Collector │   采集相关图片资源
└──────┬───────────┘
       │
       ▼
┌──────────────────┐
│ Prompt Enhancer  │   增强用户提示词
└──────┬───────────┘
       │
       ▼
┌──────────────────┐
│     Router       │   路由选择生成策略
└──────┬───────────┘
       │
       ▼
┌──────────────────┐
│ Code Generator   │   AI 代码生成
└──────┬───────────┘
       │
       ▼
┌──────────────────────┐
│ Code Quality Check   │   代码质量检查
└──────┬───────────────┘
       │ (通过/不通过)
       ▼
┌──────────────────┐
│ Project Builder  │   工程构建与保存
└──────┬───────────┘
       │
       ▼
┌──────────────┐
│     END      │
└──────────────┘
```

## 快速开始

### 环境要求

- JDK 21+
- Node.js 20.19+ 或 22.12+
- MySQL 8.0+
- Redis 6.0+
- Maven 3.8+

### 1. 克隆项目

```bash
git clone https://github.com/ROTl24/ai-web-generator.git
cd ai-web-generator
```

### 2. 初始化数据库

```bash
# 执行 SQL 脚本创建数据库和表
mysql -u root -p < sql/creat_table.sql
```

### 3. 配置后端

在 `src/main/resources/` 下创建 `application-local.yml`（或修改 `application.yml`），配置以下信息：

```yaml
# 数据库配置
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/xiaolu_ai_code
    username: your_username
    password: your_password
  # Redis 配置
  data:
    redis:
      host: localhost
      port: 6379

# AI 大模型配置（OpenAI 兼容接口）
langchain4j:
  open-ai:
    chat-model:
      api-key: your_api_key
      base-url: your_base_url
      model-name: your_model_name
```

### 4. 启动后端

```bash
./mvnw spring-boot:run
```

或在 IDE 中运行 `XiaoluAiCodeApplication.java`。

### 5. 启动前端

```bash
cd xiaolu_ai-code-frontend
npm install
npm run dev
```

### 6. 访问应用

- 前端页面：http://localhost:5173
- API 文档：http://localhost:8123/api/doc.html

## API 模块

| 模块 | 路径 | 说明 |
|------|------|------|
| 用户管理 | `/api/user/*` | 注册、登录、用户信息管理 |
| 应用管理 | `/api/app/*` | 应用创建、编辑、删除、部署 |
| AI 对话生成 | `/api/app/chat/gen/code` | SSE 流式对话代码生成 |
| 对话历史 | `/api/chatHistory/*` | 对话记录查询 |
| 静态资源 | `/api/static/{deployKey}/**` | 已部署应用的静态资源服务 |
| 健康检查 | `/api/health/` | 服务健康状态检查 |

## 代码生成模式

| 模式 | 说明 |
|------|------|
| HTML | 生成单个完整的 HTML 文件，适合简单的静态页面 |
| Multi-File | 生成多文件项目（HTML + CSS + JS），适合中等复杂度的 Web 应用 |
| Vue Project | 生成完整的 Vue 3 工程项目，支持创建和迭代修改，适合复杂的 SPA 应用 |

## 作者

**小陆** - [@ROTl24](https://github.com/ROTl24)
