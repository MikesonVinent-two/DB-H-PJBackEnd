# Spring Boot 后端项目

这是一个使用Gradle构建的Spring Boot后端项目，可以接收前端8080端口的请求，并连接MySQL数据库。

## 技术栈

- Java 21
- Spring Boot 3.2.5
- Spring Data JPA
- MySQL
- Gradle 8.5

## 项目结构

```
src/main/java/com/example/demo/
├── DemoApplication.java         # 应用程序入口
├── controller/                  # 控制器层
│   ├── UserController.java
│   └── LlmController.java
├── service/                     # 服务层
│   ├── UserService.java
│   ├── LlmService.java
│   └── impl/
│       ├── UserServiceImpl.java
│       └── LlmServiceImpl.java
├── repository/                  # 数据访问层
│   └── UserRepository.java
├── entity/                      # 实体类
│   └── User.java
├── dto/                         # 数据传输对象
│   ├── UserDTO.java
│   ├── LlmRequestDTO.java
│   └── LlmResponseDTO.java
├── config/                      # 配置类
│   ├── RestTemplateConfig.java
│   └── LlmConfig.java
└── exception/                   # 异常处理
    ├── GlobalExceptionHandler.java
    ├── ApiExceptionHandler.java
    ├── ResourceNotFoundException.java
    └── ErrorDetails.java
```

## 如何运行

1. 确保已安装JDK 21和MySQL
2. 在MySQL中创建名为`demo`的数据库
3. 修改`src/main/resources/application.yml`中的数据库连接信息
4. 在Windows系统中运行以下命令：

```bash
.\gradlew.bat bootRun
```

或者使用IDE导入项目后运行`DemoApplication.java`

## API接口

### 用户管理

- `GET /api/users` - 获取所有用户
- `GET /api/users/{id}` - 根据ID获取用户
- `POST /api/users` - 创建新用户
- `PUT /api/users/{id}` - 更新用户信息
- `DELETE /api/users/{id}` - 删除用户

### LLM API代理

- `POST /api/llm/chat` - 发送请求到LLM API并获取回答

#### 请求参数

```json
{
  "api": "https://api.openai.com/v1/chat/completions", // LLM API的URL
  "apiKey": "your-api-key", // API密钥
  "model": "gpt-3.5-turbo", // 模型名称
  "message": "你好，请介绍一下自己", // 用户消息
  "temperature": 0.7, // 可选：温度参数
  "maxTokens": 2000, // 可选：最大token数
  "systemPrompts": [ // 可选：系统提示
    {
      "role": "system",
      "content": "你是一个有用的AI助手。"
    }
  ],
  "additionalParams": { // 可选：其他API参数
    "top_p": 0.9,
    "frequency_penalty": 0.0
  }
}
```

#### 响应格式

```json
{
  "content": "我是一个AI助手，由OpenAI开发...", // LLM的回答内容
  "model": "gpt-3.5-turbo", // 使用的模型
  "tokenCount": 150, // 使用的token数量
  "responseTime": 1200, // 响应时间（毫秒）
  "success": true, // 是否成功
  "errorMessage": null, // 错误信息（如果有）
  "metadata": { // 原始API响应的元数据
    // API返回的完整响应
  }
}
``` 