# 评测标签提示词API文档

## 功能简介

评测标签提示词API用于管理评测场景下的标签提示词，为不同标签的评测提供个性化的提示词模板。

## 接口列表

| 方法 | 路径 | 功能描述 |
| --- | --- | --- |
| POST | /prompts/evaluation/tags | 创建评测标签提示词 |
| PUT | /prompts/evaluation/tags/{id} | 更新评测标签提示词 |
| GET | /prompts/evaluation/tags/{id} | 获取单个评测标签提示词详情 |
| GET | /prompts/evaluation/tags | 获取所有评测标签提示词 |
| GET | /prompts/evaluation/tags/active/tag/{tagId} | 获取指定标签的所有激活状态的评测提示词 |
| DELETE | /prompts/evaluation/tags/{id} | 删除评测标签提示词（软删除） |

## 数据结构

### 请求体数据结构（EvaluationTagPromptDTO）

```json
{
  "userId": 1,                      // 必填，操作用户ID
  "tagId": 2,                       // 必填，标签ID
  "name": "评分严格模式",           // 必填，提示词名称
  "promptTemplate": "请对此回答进行严格评分，重点关注...", // 必填，提示词模板内容
  "description": "适用于需要严格评分的场景",  // 可选，描述
  "isActive": true,                 // 可选，是否激活，默认true
  "promptPriority": 80,             // 可选，优先级（1-100），默认50
  "version": "1.0",                 // 可选，版本号
  "parentPromptId": 1               // 可选，父提示词ID
}
```

### 响应数据结构（EvaluationTagPrompt）

```json
{
  "id": 1,
  "tag": {
    "id": 2,
    "tagName": "数学"
  },
  "name": "评分严格模式",
  "promptTemplate": "请对此回答进行严格评分，重点关注...",
  "description": "适用于需要严格评分的场景",
  "isActive": true,
  "promptPriority": 80,
  "version": "1.0",
  "createdAt": "2023-05-10T10:15:30",
  "updatedAt": "2023-05-10T10:15:30",
  "createdByUser": {
    "id": 1,
    "username": "admin"
  },
  "parentPrompt": null,
  "deletedAt": null
}
```

## 接口详细说明

### 1. 创建评测标签提示词

- **URL**: `/prompts/evaluation/tags`
- **方法**: `POST`
- **请求体**: `EvaluationTagPromptDTO`
- **响应**: `EvaluationTagPrompt`
- **状态码**: 201 Created

### 2. 更新评测标签提示词

- **URL**: `/prompts/evaluation/tags/{id}`
- **方法**: `PUT`
- **路径参数**: `id` - 提示词ID
- **请求体**: `EvaluationTagPromptDTO`
- **响应**: `EvaluationTagPrompt`
- **状态码**: 200 OK

### 3. 获取单个评测标签提示词详情

- **URL**: `/prompts/evaluation/tags/{id}`
- **方法**: `GET`
- **路径参数**: `id` - 提示词ID
- **响应**: `EvaluationTagPrompt`
- **状态码**: 200 OK / 404 Not Found

### 4. 获取所有评测标签提示词

- **URL**: `/prompts/evaluation/tags`
- **方法**: `GET`
- **响应**: `List<EvaluationTagPrompt>`
- **状态码**: 200 OK

### 5. 获取指定标签的所有激活状态的评测提示词

- **URL**: `/prompts/evaluation/tags/active/tag/{tagId}`
- **方法**: `GET`
- **路径参数**: `tagId` - 标签ID
- **响应**: `List<EvaluationTagPrompt>`
- **状态码**: 200 OK

### 6. 删除评测标签提示词

- **URL**: `/prompts/evaluation/tags/{id}`
- **方法**: `DELETE`
- **路径参数**: `id` - 提示词ID
- **请求体**: `{"userId": 1}`
- **响应**: 无
- **状态码**: 204 No Content

## 使用场景

1. 管理员可以为不同标签创建专门的评测提示词，如针对"数学"标签的严格评分模式
2. 在评测运行时，系统会根据问题的标签自动选择对应的评测提示词
3. 可以设置不同优先级的提示词，系统会优先使用优先级高的提示词
4. 可以通过版本控制，保留历史提示词版本，进行比较和回滚

## 注意事项

1. 创建和更新操作需要提供有效的用户ID
2. 删除操作为软删除，不会真正从数据库中删除记录
3. 优先级的范围为1-100，值越大优先级越高
4. 评测提示词与Tag是多对一关系，一个标签可以有多个不同场景的评测提示词 