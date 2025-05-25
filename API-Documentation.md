# 医疗问答评测系统接口文档

## 1. 用户管理接口
基础路径：`/api/users`

### 1.1 用户注册
- 路径：`POST /api/users/register`
- 功能：注册新用户
- 请求体：
```json
{
    "username": "string",     // 用户名（3-50字符）
    "password": "string",     // 密码（最少6字符）
    "name": "string",         // 姓名（可选）
    "contactInfo": "string",  // 联系方式
    "role": "CROWDSOURCE_USER" // 用户角色（默认为众包用户）
}
```
- 返回体：
```json
{
    "id": "number",
    "username": "string",
    "name": "string",
    "contactInfo": "string",
    "role": "string"
}
```

### 1.2 用户登录
- 路径：`POST /api/users/login`
- 功能：用户登录
- 请求体：
```json
{
    "username": "string",
    "password": "string"
}
```
- 返回体：
```json
{
    "id": "number",
    "username": "string",
    "name": "string",
    "contactInfo": "string",
    "role": "string"
}
```

### 1.3 用户登出
- 路径：`POST /api/users/logout`
- 功能：用户登出
- 请求体：
```json
{
    "id": "number",
    "username": "string"
}
```
- 返回体：
```json
{
    "message": "登出成功"
}
```

### 1.4 获取用户信息
- 路径：`GET /api/users/{id}`
- 功能：获取指定用户信息
- 返回体：
```json
{
    "id": "number",
    "username": "string",
    "name": "string",
    "contactInfo": "string",
    "role": "string",
    "createdAt": "datetime",
    "updatedAt": "datetime"
}
```

### 1.5 获取用户档案
- 路径：`GET /api/users/profile/{userId}`
- 功能：获取用户详细档案
- 返回体：
```json
{
    "id": "number",
    "username": "string",
    "name": "string",
    "role": "string",
    "contactInfo": "string",
    "createdAt": "datetime",
    "updatedAt": "datetime"
}
```

## 2. 标准问题管理
基础路径：`/api/standard-questions`

### 2.1 创建标准问题
- 路径：`POST /api/standard-questions`
- 功能：创建新的标准问题
- 请求体：
```json
{
    "userId": "number",
    "originalRawQuestionId": "number",  // 原始问题ID（可选）
    "questionText": "string",           // 问题文本（必填）
    "questionType": "string",           // 问题类型（必填）：SINGLE_CHOICE/MULTIPLE_CHOICE/SIMPLE_FACT/SUBJECTIVE
    "difficulty": "string",             // 难度级别（可选）
    "parentStandardQuestionId": "number", // 父问题ID（可选）
    "commitMessage": "string",          // 提交说明（可选）
    "tags": ["string"]                  // 标签列表（可选）
}
```
- 返回体：返回创建的标准问题详情

### 2.2 更新标准问题
- 路径：`PUT /api/standard-questions/{questionId}`
- 功能：更新已有标准问题
- 请求体：与创建接口相同
- 返回体：返回更新后的问题详情

## 3. 众包答案管理
基础路径：`/api/crowdsourced-answers`

### 3.1 获取问题的众包答案
- 路径：`GET /api/crowdsourced-answers/by-question/{questionId}`
- 功能：获取指定问题的所有众包答案
- 参数：
  - `page`：页码（默认0）
  - `size`：每页数量（默认10）
  - `sort`：排序字段（默认submissionTime）
- 返回体：分页的众包答案列表

### 3.2 获取用户的众包答案
- 路径：`GET /api/crowdsourced-answers/by-user/{userId}`
- 功能：获取指定用户提交的所有众包答案
- 参数：同上
- 返回体：分页的众包答案列表

### 3.3 根据状态获取众包答案
- 路径：`GET /api/crowdsourced-answers/by-status/{status}`
- 功能：获取指定状态的众包答案
- 参数：
  - `status`：答案状态（PENDING/ACCEPTED/REJECTED/FLAGGED）
  - 分页参数同上
- 返回体：分页的众包答案列表

### 3.4 提交众包答案
- 路径：`POST /api/crowdsourced-answers`
- 功能：提交新的众包答案
- 请求体：
```json
{
    "standardQuestionId": "number",
    "userId": "number",
    "answerText": "string",
    "taskBatchId": "number"
}
```
- 返回体：创建的众包答案详情

### 3.5 修改众包答案
- 路径：`PUT /api/crowdsourced-answers/{answerId}`
- 功能：修改已提交的众包答案
- 请求体：同提交接口
- 返回体：更新后的众包答案详情

### 3.6 审核众包答案
- 路径：`PUT /api/crowdsourced-answers/{answerId}/review`
- 功能：审核众包答案
- 请求体：
```json
{
    "reviewerUserId": "number",
    "status": "string",      // ACCEPTED/REJECTED/FLAGGED
    "feedback": "string"     // 审核反馈
}
```
- 返回体：审核后的众包答案详情

### 3.7 删除众包答案
- 路径：`DELETE /api/crowdsourced-answers/{answerId}`
- 功能：删除众包答案
- 参数：
  - `userId`：操作用户ID
- 返回体：操作结果

## 4. 原始数据管理
基础路径：`/api/raw-data`

### 4.1 创建原始问题
- 路径：`POST /api/raw-data/questions`
- 功能：创建新的原始问题
- 请求体：原始问题信息

### 4.2 创建原始答案
- 路径：`POST /api/raw-data/answers`
- 功能：创建新的原始答案
- 请求体：原始答案信息

### 4.3 创建问题及答案
- 路径：`POST /api/raw-data/questions-with-answers`
- 功能：同时创建问题和答案
- 请求体：问题和答案信息

### 4.4 获取原始问题列表
- 路径：`GET /api/raw-data/questions`
- 功能：获取所有原始问题
- 参数：分页参数
- 返回体：分页的原始问题列表

### 4.5 按状态查询原始问题
- 路径：`GET /api/raw-data/questions/by-status`
- 功能：根据标准化状态查询原始问题
- 参数：
  - `standardized`：是否已标准化（默认false）
  - 分页参数
- 返回体：分页的原始问题列表

### 4.6 按来源查询原始问题
- 路径：`GET /api/raw-data/questions/by-source`
- 功能：根据来源网站查询原始问题
- 参数：
  - `sourceSite`：来源网站
  - 分页参数
- 返回体：分页的原始问题列表

### 4.7 搜索原始问题
- 路径：`GET /api/raw-data/questions/search`
- 功能：搜索原始问题
- 参数：
  - `keyword`：搜索关键词
  - 分页参数
- 返回体：分页的原始问题列表

## 5. 数据集管理
基础路径：`/api/datasets`

### 5.1 创建数据集版本
- 路径：`POST /api/datasets/versions`
- 功能：创建新的数据集版本
- 请求体：
```json
{
    "userId": "number",
    "name": "string",
    "description": "string"
}
```
- 返回体：创建的数据集版本信息

### 5.2 获取数据集版本列表
- 路径：`GET /api/datasets/versions`
- 功能：获取所有数据集版本
- 参数：
  - `name`：按名称筛选（可选）
- 返回体：数据集版本列表

### 5.3 获取数据集版本详情
- 路径：`GET /api/datasets/versions/{id}`
- 功能：获取指定数据集版本的详细信息
- 返回体：数据集版本详情

### 5.4 更新数据集版本
- 路径：`PUT /api/datasets/versions/{id}`
- 功能：更新数据集版本信息
- 请求体：与创建接口相同
- 返回体：更新后的数据集版本信息

## 6. 标准答案管理
基础路径：`/api/standard/standard-answers`

### 6.1 获取标准答案
- 路径：`GET /api/standard/standard-answers/{id}`
- 功能：获取指定的标准答案
- 返回体：标准答案详情，根据问题类型返回不同格式：
```json
{
    "standardQuestionId": "number",
    "userId": "number",
    "questionType": "string",     // SINGLE_CHOICE/MULTIPLE_CHOICE/SIMPLE_FACT/SUBJECTIVE
    "answerText": "string",       // 所有类型都需要
    "options": "string",          // 客观题选项列表（JSON格式）
    "correctIds": "string",       // 客观题正确答案ID列表（JSON格式）
    "alternativeAnswers": "string", // 简单题可选答案列表（JSON格式）
    "scoringGuidance": "string",  // 主观题评分指导
    "commitMessage": "string"     // 提交说明
}
```

### 6.2 创建标准答案
- 路径：`POST /api/standard/standard-answers`
- 功能：创建新的标准答案
- 请求体：标准答案信息（格式同上）
- 返回体：创建的标准答案详情

### 6.3 删除标准答案
- 路径：`DELETE /api/standard/standard-answers/{id}`
- 功能：删除指定的标准答案
- 返回体：操作结果

## 7. 批次回答生成接口
基础路径：`/api/answer-generation`

### 7.1 创建回答生成批次
- 路径：`POST /api/answer-generation/batches`
- 功能：创建新的回答生成批次
- 请求体：
```json
{
    "name": "string",                    // 批次名称（必填）
    "description": "string",             // 批次描述（可选）
    "datasetVersionId": "number",        // 数据集版本ID（必填）
    "answerAssemblyConfigId": "number",  // 回答Prompt组装配置ID（可选）
    "evaluationAssemblyConfigId": "number", // 评测Prompt组装配置ID（可选）
    "llmModelIds": ["number"],           // 要使用的LLM模型ID列表（必填）
    "globalParameters": {                // 全局参数（可选）
        "key": "value"
    },
    "modelSpecificParameters": {         // 模型特定参数（可选）
        "modelId": {
            "key": "value"
        }
    },
    "answerRepeatCount": "number",      // 每个问题的回答重复次数（可选，默认1）
    "userId": "number"                   // 创建用户ID（可选）
}
```
- 返回体：
```json
{
    "id": "number",
    "name": "string",
    "description": "string",
    "datasetVersionId": "number",
    "datasetVersionName": "string",
    "status": "PENDING",
    "creationTime": "datetime",
    "progressPercentage": "number",
    "totalRuns": "number",
    "pendingRuns": "number",
    "completedRuns": "number",
    "failedRuns": "number"
}
```

### 7.2 启动批次
- 路径：`POST /api/answer-generation/batches/{batchId}/start`
- 功能：启动指定的回答生成批次
- 参数：
  - `batchId`：批次ID
- 返回体：
```json
{
    "message": "批次启动成功"
}
```

### 7.3 暂停批次
- 路径：`POST /api/answer-generation/batches/{batchId}/pause`
- 功能：暂停正在执行的批次
- 参数：
  - `batchId`：批次ID
  - `reason`：暂停原因（query参数）
- 返回体：
```json
{
    "message": "批次已暂停"
}
```

### 7.4 恢复批次
- 路径：`POST /api/answer-generation/batches/{batchId}/resume`
- 功能：恢复已暂停的批次
- 参数：
  - `batchId`：批次ID
- 返回体：
```json
{
    "message": "批次已恢复"
}
```

### 7.5 获取批次状态
- 路径：`GET /api/answer-generation/batches/{batchId}`
- 功能：获取指定批次的详细状态
- 参数：
  - `batchId`：批次ID
- 返回体：
```json
{
    "id": "number",
    "name": "string",
    "description": "string",
    "datasetVersionId": "number",
    "datasetVersionName": "string",
    "status": "string",           // PENDING/GENERATING_ANSWERS/PAUSED/COMPLETED/FAILED
    "creationTime": "datetime",
    "completedAt": "datetime",
    "progressPercentage": "number",
    "lastActivityTime": "datetime",
    "resumeCount": "number",
    "pauseTime": "datetime",
    "pauseReason": "string",
    "answerRepeatCount": "number",
    "totalRuns": "number",
    "pendingRuns": "number",
    "completedRuns": "number",
    "failedRuns": "number"
}
```

### 7.6 获取运行状态
- 路径：`GET /api/answer-generation/runs/{runId}`
- 功能：获取指定运行的详细状态
- 参数：
  - `runId`：运行ID
- 返回体：运行的详细状态信息（ModelAnswerRunDTO）

### 7.7 获取用户批次列表
- 路径：`GET /api/answer-generation/batches/user/{userId}`
- 功能：获取指定用户创建的所有批次
- 参数：
  - `userId`：用户ID
- 返回体：批次DTO列表

### 7.8 批次相关说明
1. 批次状态（BatchStatus）包括：
   - PENDING：等待启动
   - GENERATING_ANSWERS：正在生成回答
   - PAUSED：已暂停
   - COMPLETED：已完成
   - FAILED：失败

2. 进度百分比（progressPercentage）为0-100的数值

3. 批次创建后需要显式调用启动接口才会开始执行

4. 支持WebSocket实时状态更新，消息类型包括：
   - TASK_STARTED：任务开始
   - TASK_COMPLETED：任务完成
   - STATUS_CHANGE：状态变更
   - ERROR：错误通知

5. 每个批次可以包含多个运行（每个模型一个运行）

6. 可以通过全局参数和模型特定参数来配置生成行为

7. 支持对每个问题生成多次回答（通过answerRepeatCount参数控制）

## 注意事项
1. 所有接口都支持跨域访问（CORS）
2. 错误响应格式统一为：
```json
{
    "error": "错误信息描述"
}
```
3. 大部分接口都需要用户认证
4. 分页接口默认每页10条记录
5. 时间格式统一使用ISO-8601标准
6. 所有ID字段类型均为Long
7. 文本内容（content、answerText等）均使用UTF-8编码

## 8. LLM模型管理接口
基础路径：`/api/llm-models`

### 8.1 注册LLM模型
- 路径：`POST /api/llm-models/register`
- 功能：注册新的LLM模型
- 请求体：
```json
{
    "userId": "number",           // 注册用户ID
    "apiUrl": "string",          // API接口地址
    "apiKey": "string",          // API密钥
    "apiType": "string",         // API类型（如OpenAI、Azure、Anthropic等）
    "provider": "string",        // 模型提供商
    "name": "string",            // 模型名称
    "version": "string",         // 模型版本
    "description": "string",     // 模型描述
    "modelParameters": {         // 默认模型参数
        "key": "value"
    }
}
```
- 返回体：
```json
{
    "success": "boolean",
    "message": "string",
    "registeredModels": [{
        "id": "number",
        "name": "string",
        "provider": "string",
        "version": "string",
        "description": "string",
        "apiType": "string"
    }]
}
```

### 8.2 模型表结构说明
LLM模型表（llm_models）包含以下字段：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 |
| name | VARCHAR(255) | 模型名称 |
| provider | VARCHAR(255) | 模型提供商 |
| version | VARCHAR(255) | 模型版本 |
| description | TEXT | 模型描述 |
| api_url | VARCHAR(512) | API接口地址 |
| api_key | VARCHAR(512) | API密钥 |
| api_type | VARCHAR(50) | 调用方式或接口类型 |
| model_parameters | JSON | 默认模型参数 |
| created_at | DATETIME | 创建时间 |
| created_by_user_id | BIGINT | 创建用户ID |
| created_change_log_id | BIGINT | 变更日志ID |
| deleted_at | DATETIME | 软删除时间 |

### 8.3 模型运行表结构说明
模型运行表（model_answer_runs）包含以下字段：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 |
| answer_generation_batch_id | BIGINT | 所属批次ID |
| llm_model_id | BIGINT | 模型ID |
| run_name | VARCHAR(255) | 运行名称 |
| run_description | TEXT | 运行描述 |
| run_index | INT | 运行索引 |
| run_time | DATETIME | 运行时间 |
| status | ENUM | 运行状态 |
| parameters | JSON | 运行参数 |
| error_message | TEXT | 错误信息 |
| progress_percentage | DECIMAL(5,2) | 完成百分比 |
| completed_questions_count | INT | 已完成问题数 |
| failed_questions_count | INT | 失败问题数 |
| total_questions_count | INT | 总问题数 |

### 8.4 模型回答表结构说明
模型回答表（llm_answers）包含以下字段：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 |
| model_answer_run_id | BIGINT | 运行ID |
| dataset_question_mapping_id | BIGINT | 数据集问题ID |
| answer_text | TEXT | 回答内容 |
| generation_status | ENUM | 生成状态 |
| error_message | TEXT | 错误信息 |
| generation_time | DATETIME | 生成时间 |
| prompt_used | TEXT | 使用的prompt |
| raw_model_response | TEXT | 原始响应 |
| other_metadata | JSON | 其他元数据 |
| repeat_index | INT | 重复索引 |

### 8.5 注意事项
1. 模型状态（RunStatus）包括：
   - PENDING：等待中
   - GENERATING_ANSWERS：生成答案中
   - ANSWER_GENERATION_FAILED：答案生成失败
   - READY_FOR_EVALUATION：准备评测
   - EVALUATING：评测中
   - COMPLETED：已完成
   - FAILED：失败
   - PAUSED：已暂停
   - RESUMING：正在恢复

2. 生成状态（GenerationStatus）包括：
   - SUCCESS：生成成功
   - FAILED：生成失败

3. API类型支持：
   - OpenAI
   - Azure OpenAI
   - Anthropic
   - 其他兼容OpenAI接口的模型

4. 安全注意事项：
   - API密钥等敏感信息使用加密存储
   - 支持软删除机制
   - 记录所有变更日志

## 9. LLM聊天接口
基础路径：`/api/llm`

### 9.1 发送聊天请求
- 路径：`POST /api/llm/chat`
- 功能：向LLM模型发送聊天请求并获取回答
- 请求体：
```json
{
    "apiUrl": "string",          // API接口地址（必填，如：https://chatgtp.vin）
    "apiKey": "string",          // API密钥（必填）
    "api": "string",             // API类型（必填，可选值：openai/azure/anthropic/openai_compatible）
    "model": "string",           // 模型名称（必填，如：gpt-3.5-turbo/deepseek-r1等）
    "message": "string",         // 用户消息内容（必填）
    "temperature": "number",     // 温度参数（可选，默认0.7，范围0-1）
    "maxTokens": "number",       // 最大token数（可选，默认2000）
    "systemPrompts": [           // 系统提示（可选）
        {
            "role": "system",
            "content": "string"
        }
    ],
    "additionalParams": {        // 其他参数（可选）
        "key": "value"
    }
}
```

- 返回体：
```json
{
    "success": "boolean",        // 是否成功
    "message": "string",         // 错误信息（如果失败）
    "data": {
        "content": "string",     // 模型回答内容
        "tokens": "number",      // 使用的token数量
        "finish_reason": "string" // 结束原因（如：stop/length等）
    }
}
```

### 调用示例

#### OpenAI兼容模式调用
对于使用OpenAI接口格式的第三方模型（如DeepSeek），可以使用`openai_compatible`模式：

```json
{
    "apiUrl": "https://chatgtp.vin",
    "apiKey": "sk-P2pSLjuCWtHZEU78nfPGCkbZtgesZppuVonLeM9Lms7WImyO",
    "api": "openai_compatible",
    "model": "deepseek-r1",
    "message": "你好，请介绍一下自己",
    "temperature": 0.7,
    "maxTokens": 2000,
    "systemPrompts": [
        {
            "role": "system",
            "content": "你是一个专业的助手，请用中文回答问题。"
        }
    ]
}
```

说明：
- 当`api`设置为`openai_compatible`时，系统会使用OpenAI的API格式发送请求
- 支持所有使用OpenAI接口格式的模型服务，如DeepSeek、Claude-2等
- `apiUrl`需要填写实际的模型服务地址
- `apiKey`需要填写对应服务的API密钥
- `model`填写实际可用的模型名称

### 9.2 获取可用模型列表
- 路径：`POST /api/llm/models`
- 功能：获取指定API提供商的可用模型列表
- 请求体：
```json
{
    "apiUrl": "string",         // API基础URL
    "apiKey": "string"          // API密钥
}
```
- 返回体：模型信息列表
```json
[
    {
        "id": "string",         // 模型ID
        "name": "string",       // 模型名称
        "provider": "string",   // 提供商
        "version": "string",    // 版本
        "description": "string" // 描述
    }
]
```

### 9.3 支持的API类型
1. OpenAI API
   - 支持gpt-3.5-turbo、gpt-4等模型
   - 使用标准的OpenAI chat completions格式

2. Azure OpenAI API
   - 支持Azure部署的OpenAI模型
   - 使用Azure特定的认证方式

3. Anthropic API
   - 支持Claude系列模型
   - 使用Anthropic特定的请求格式

4. 其他兼容OpenAI接口的模型
   - 支持任何兼容OpenAI chat completions格式的API

### 9.4 注意事项
1. API认证：
   - OpenAI/兼容API：使用Bearer Token认证
   - Azure：使用api-key认证
   - Anthropic：使用x-api-key认证

2. 请求格式：
   - 默认使用chat completions格式
   - 根据不同API类型自动调整请求格式
   - 支持自定义参数通过additionalParams传递

3. 响应处理：
   - 统一的响应格式
   - 包含原始响应元数据
   - 记录token使用情况和响应时间

4. 错误处理：
   - API调用失败返回详细错误信息
   - 支持请求重试机制
   - 记录详细的错误日志

5. 性能优化：
   - 支持模型特定的超时设置
   - 异步处理长响应
   - 结果缓存（可选） 