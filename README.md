# Spring Boot 后端项目

这是一个使用Gradle构建的Spring Boot后端项目，处理原始问题标准化、众包回答、专家回答等功能，并连接MySQL数据库。

## 技术栈

- Java 21
- Spring Boot 3.2.5
- Spring Data JPA
- MySQL
- Gradle 8.5

## 项目结构

```
src/main/java/com/example/demo/
├── DemoApplication.java            # 应用程序入口
├── controller/                     # 控制器层
├── service/                        # 服务层
│   └── impl/                       # 服务实现
├── repository/                     # 数据访问层
├── entity/                         # 实体类
├── dto/                            # 数据传输对象
├── config/                         # 配置类
└── exception/                      # 异常处理
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

# API接口文档

## 用户管理接口

### 用户注册
- **路径**：`POST /api/users/register`
- **请求体**：
```json
{
  "username": "user123",
  "password": "password123",
  "contactInfo": "email@example.com",
  "role": "EXPERT"
}
```
- **响应**：用户信息

### 用户登录
- **路径**：`POST /api/users/login`
- **请求体**：
```json
{
  "username": "user123",
  "password": "password123"
}
```
- **响应**：用户信息

### 获取所有用户
- **路径**：`GET /api/users`
- **响应**：用户列表

### 获取用户信息
- **路径**：`GET /api/users/{id}`
- **响应**：用户信息

### 更新用户信息
- **路径**：`PUT /api/users/{id}`
- **请求体**：用户信息
- **响应**：更新后的用户信息

## 原始数据接口

### 创建原始问题
- **路径**：`POST /api/raw-data/questions`
- **请求体**：原始问题数据
- **响应**：创建的原始问题

### 创建原始问题（DTO方式）
- **路径**：`POST /api/raw-data/questions-dto`
- **请求体**：原始问题DTO
- **响应**：创建的原始问题

### 创建原始回答
- **路径**：`POST /api/raw-data/answers`
- **请求体**：原始回答DTO
- **响应**：创建的原始回答

### 创建带回答的原始问题
- **路径**：`POST /api/raw-data/questions-with-answers`
- **请求体**：包含问题和回答的DTO
- **响应**：创建的原始问题

### 获取所有原始问题
- **路径**：`GET /api/raw-data/questions`
- **参数**：分页参数
- **响应**：分页的原始问题列表

### 根据标准化状态获取原始问题
- **路径**：`GET /api/raw-data/questions/by-status`
- **参数**：`standardized`(布尔值)，分页参数
- **响应**：分页的原始问题列表

### 根据来源网站获取原始问题
- **路径**：`GET /api/raw-data/questions/by-source`
- **参数**：`sourceSite`，分页参数
- **响应**：分页的原始问题列表

### 搜索原始问题
- **路径**：`GET /api/raw-data/questions/search`
- **参数**：`keyword`，分页参数
- **响应**：分页的原始问题列表

### 根据标签获取原始问题
- **路径**：`GET /api/raw-data/questions/by-tags`
- **参数**：`tags`(列表)，分页参数
- **响应**：分页的原始问题列表

### 删除原始问题
- **路径**：`DELETE /api/raw-data/questions/{questionId}`
- **响应**：操作结果

### 删除原始回答
- **路径**：`DELETE /api/raw-data/answers/{answerId}`
- **响应**：操作结果

## 标准问题接口

### 创建标准问题
- **路径**：`POST /api/standard-questions`
- **请求体**：标准问题DTO
- **响应**：创建的标准问题

### 更新标准问题
- **路径**：`PUT /api/standard-questions/{questionId}`
- **请求体**：标准问题DTO
- **响应**：更新后的标准问题

## 标准答案接口

### 创建或更新标准答案
- **路径**：`POST /api/standard/standard-answers`
- **请求体**：标准答案DTO
- **响应**：创建或更新的标准答案

### 获取标准答案
- **路径**：`GET /api/standard/standard-answers/{standardQuestionId}`
- **响应**：标准答案

## 众包回答接口

### 提交众包回答
- **路径**：`POST /api/crowdsourced-answers`
- **请求体**：
```json
{
  "standardQuestionId": 123,
  "userId": 456,
  "answerText": "众包回答内容",
  "taskBatchId": 1
}
```
- **响应**：提交的众包回答

### 修改众包回答
- **路径**：`PUT /api/crowdsourced-answers/{answerId}`
- **请求体**：
```json
{
  "userId": 456,
  "answerText": "修改后的回答内容"
}
```
- **响应**：修改后的众包回答
- **说明**：
  - 只有创建者可以修改自己的回答
  - 只有PENDING状态的回答可以修改

### 删除众包回答
- **路径**：`DELETE /api/crowdsourced-answers/{answerId}?userId=456`
- **参数**：`userId` - 操作用户ID
- **响应**：
```json
{
  "status": "success",
  "message": "众包回答删除成功"
}
```
- **说明**：
  - 只有创建者可以删除自己的回答
  - 只有PENDING状态的回答可以删除

### 审核众包回答
- **路径**：`PUT /api/crowdsourced-answers/{answerId}/review`
- **请求体**：
```json
{
  "reviewerUserId": 789,
  "status": "ACCEPTED",
  "feedback": "审核反馈"
}
```
- **响应**：审核后的众包回答

### 按问题ID获取众包回答
- **路径**：`GET /api/crowdsourced-answers/by-question/{questionId}`
- **参数**：分页参数
- **响应**：分页的众包回答列表

### 按用户ID获取众包回答
- **路径**：`GET /api/crowdsourced-answers/by-user/{userId}`
- **参数**：分页参数
- **响应**：分页的众包回答列表

### 按审核状态获取众包回答
- **路径**：`GET /api/crowdsourced-answers/by-status/{status}`
- **参数**：分页参数
- **响应**：分页的众包回答列表

### 按问题ID和审核状态获取众包回答
- **路径**：`GET /api/crowdsourced-answers/by-question/{questionId}/status/{status}`
- **参数**：分页参数
- **响应**：分页的众包回答列表

## 专家候选回答接口

### 提交专家候选回答
- **路径**：`POST /api/expert-candidate-answers`
- **请求体**：
```json
{
  "standardQuestionId": 123,
  "userId": 456,
  "candidateAnswerText": "专家回答内容"
}
```
- **响应**：提交的专家候选回答

### 修改专家候选回答
- **路径**：`PUT /api/expert-candidate-answers/{answerId}`
- **请求体**：
```json
{
  "userId": 456,
  "answerText": "修改后的专家回答内容"
}
```
- **响应**：修改后的专家候选回答
- **说明**：
  - 只有创建者可以修改自己的回答

### 删除专家候选回答
- **路径**：`DELETE /api/expert-candidate-answers/{answerId}?userId=456`
- **参数**：`userId` - 操作用户ID
- **响应**：
```json
{
  "status": "success",
  "message": "专家候选回答删除成功"
}
```
- **说明**：
  - 只有创建者可以删除自己的回答

### 更新质量评分和反馈
- **路径**：`PUT /api/expert-candidate-answers/{answerId}/quality`
- **参数**：
  - `qualityScore` - 质量评分
  - `feedback` - 反馈内容(可选)
- **响应**：更新后的专家候选回答

### 按问题ID获取专家候选回答
- **路径**：`GET /api/expert-candidate-answers/by-question/{questionId}`
- **参数**：分页参数
- **响应**：分页的专家候选回答列表

### 按用户ID获取专家候选回答
- **路径**：`GET /api/expert-candidate-answers/by-user/{userId}`
- **参数**：分页参数
- **响应**：分页的专家候选回答列表

## LLM API代理

### 发送请求到LLM API
- **路径**：`POST /api/llm/chat`
- **请求体**：
```json
{
  "api": "https://api.openai.com/v1/chat/completions",
  "apiKey": "your-api-key",
  "model": "gpt-3.5-turbo",
  "message": "你好，请介绍一下自己",
  "temperature": 0.7,
  "maxTokens": 2000,
  "systemPrompts": [
    {
      "role": "system",
      "content": "你是一个有用的AI助手。"
    }
  ]
}
```
- **响应**：LLM API的响应结果

### 获取可用模型列表
- **路径**：`POST /api/llm/models`
- **请求体**：
```json
{
  "apiUrl": "https://api.openai.com",
  "apiKey": "your-api-key"
}
```
- **响应**：可用模型列表 