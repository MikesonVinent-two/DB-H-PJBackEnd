# 智能问答系统数据库ER图

## 系统概述
这是一个专业的AI模型评测平台的数据库设计，支持完整的问答数据管理、模型评估和结果分析工作流程。

## ER图

```mermaid
erDiagram
    %% 用户和权限管理
    USERS {
        bigint ID PK
        varchar USERNAME UK
        varchar PASSWORD
        varchar NAME
        enum ROLE
        varchar CONTACT_INFO
        datetime CREATED_AT
        datetime UPDATED_AT
        datetime DELETED_AT
    }

    %% 原始数据采集
    RAW_QUESTIONS {
        bigint ID PK
        varchar SOURCE_URL UK
        varchar SOURCE_SITE
        varchar TITLE
        text CONTENT
        datetime CRAWL_TIME
        json TAGS
        json OTHER_METADATA
    }

    RAW_ANSWERS {
        bigint ID PK
        bigint RAW_QUESTION_ID FK
        varchar AUTHOR_INFO
        text CONTENT
        datetime PUBLISH_TIME
        int UPVOTES
        boolean IS_ACCEPTED
        json OTHER_METADATA
    }

    %% 变更日志系统
    CHANGE_LOG {
        bigint ID PK
        datetime CHANGE_TIME
        bigint CHANGED_BY_USER_ID FK
        varchar CHANGE_TYPE
        text COMMIT_MESSAGE
        bigint ASSOCIATED_STANDARD_QUESTION_ID FK
    }

    CHANGE_LOG_DETAILS {
        bigint ID PK
        bigint CHANGE_LOG_ID FK
        enum ENTITY_TYPE
        bigint ENTITY_ID
        varchar ATTRIBUTE_NAME
        json OLD_VALUE
        json NEW_VALUE
    }

    %% 标准问题体系
    STANDARD_QUESTIONS {
        bigint ID PK
        bigint ORIGINAL_RAW_QUESTION_ID FK
        text QUESTION_TEXT
        enum QUESTION_TYPE
        enum DIFFICULTY
        datetime CREATION_TIME
        bigint CREATED_BY_USER_ID FK
        bigint PARENT_STANDARD_QUESTION_ID FK
        bigint CREATED_CHANGE_LOG_ID FK
        datetime DELETED_AT
    }

    %% 专家和众包答案
    EXPERT_CANDIDATE_ANSWERS {
        bigint ID PK
        bigint STANDARD_QUESTION_ID FK
        bigint USER_ID FK
        text CANDIDATE_ANSWER_TEXT
        datetime SUBMISSION_TIME
        int QUALITY_SCORE
        text FEEDBACK
    }

    CROWDSOURCED_ANSWERS {
        bigint ID PK
        bigint STANDARD_QUESTION_ID FK
        bigint USER_ID FK
        text ANSWER_TEXT
        datetime SUBMISSION_TIME
        bigint TASK_BATCH_ID
        enum QUALITY_REVIEW_STATUS
        bigint REVIEWED_BY_USER_ID FK
        datetime REVIEW_TIME
        text REVIEW_FEEDBACK
        json OTHER_METADATA
    }

    %% 标准答案体系
    STANDARD_OBJECTIVE_ANSWERS {
        bigint ID PK
        bigint STANDARD_QUESTION_ID FK
        json OPTIONS
        json CORRECT_IDS
        bigint DETERMINED_BY_USER_ID FK
        datetime DETERMINED_TIME
        bigint CREATED_CHANGE_LOG_ID FK
        datetime DELETED_AT
    }

    STANDARD_SIMPLE_ANSWERS {
        bigint ID PK
        bigint STANDARD_QUESTION_ID FK
        text ANSWER_TEXT
        json ALTERNATIVE_ANSWERS
        bigint DETERMINED_BY_USER_ID FK
        datetime DETERMINED_TIME
        bigint CREATED_CHANGE_LOG_ID FK
        datetime DELETED_AT
    }

    STANDARD_SUBJECTIVE_ANSWERS {
        bigint ID PK
        bigint STANDARD_QUESTION_ID FK
        text ANSWER_TEXT
        text SCORING_GUIDANCE
        bigint DETERMINED_BY_USER_ID FK
        datetime DETERMINED_TIME
        bigint CREATED_CHANGE_LOG_ID FK
        datetime DELETED_AT
    }

    %% 标签系统
    TAGS {
        bigint ID PK
        varchar TAG_NAME UK
        varchar TAG_TYPE
        text DESCRIPTION
        datetime CREATED_AT
        bigint CREATED_BY_USER_ID FK
        bigint CREATED_CHANGE_LOG_ID FK
        datetime DELETED_AT
    }

    STANDARD_QUESTION_TAGS {
        bigint ID PK
        bigint STANDARD_QUESTION_ID FK
        bigint TAG_ID FK
        datetime CREATED_AT
        bigint CREATED_BY_USER_ID FK
        bigint CREATED_CHANGE_LOG_ID FK
    }

    RAW_QUESTION_TAGS {
        bigint ID PK
        bigint RAW_QUESTION_ID FK
        bigint TAG_ID FK
        datetime CREATED_AT
        bigint CREATED_BY_USER_ID FK
        bigint CREATED_CHANGE_LOG_ID FK
    }

    %% 提示词系统 - 回答场景
    ANSWER_TAG_PROMPTS {
        bigint ID PK
        bigint TAG_ID FK
        varchar NAME
        text PROMPT_TEMPLATE
        text DESCRIPTION
        boolean IS_ACTIVE
        int PROMPT_PRIORITY
        varchar VERSION
        datetime CREATED_AT
        datetime UPDATED_AT
        bigint CREATED_BY_USER_ID FK
        bigint PARENT_PROMPT_ID FK
        bigint CREATED_CHANGE_LOG_ID FK
        datetime DELETED_AT
    }

    ANSWER_QUESTION_TYPE_PROMPTS {
        bigint ID PK
        varchar NAME
        enum QUESTION_TYPE
        text PROMPT_TEMPLATE
        text DESCRIPTION
        boolean IS_ACTIVE
        text RESPONSE_FORMAT_INSTRUCTION
        text RESPONSE_EXAMPLE
        varchar VERSION
        datetime CREATED_AT
        datetime UPDATED_AT
        bigint CREATED_BY_USER_ID FK
        bigint PARENT_PROMPT_ID FK
        bigint CREATED_CHANGE_LOG_ID FK
        datetime DELETED_AT
    }

    ANSWER_PROMPT_ASSEMBLY_CONFIGS {
        bigint ID PK
        varchar NAME
        text DESCRIPTION
        text BASE_SYSTEM_PROMPT
        varchar TAG_PROMPTS_SECTION_HEADER
        varchar QUESTION_TYPE_SECTION_HEADER
        varchar TAG_PROMPT_SEPARATOR
        varchar SECTION_SEPARATOR
        text FINAL_INSTRUCTION
        boolean IS_ACTIVE
        datetime CREATED_AT
        datetime UPDATED_AT
        bigint CREATED_BY_USER_ID FK
        bigint CREATED_CHANGE_LOG_ID FK
    }

    %% 提示词系统 - 评估场景
    EVALUATION_TAG_PROMPTS {
        bigint ID PK
        bigint TAG_ID FK
        varchar NAME
        text PROMPT_TEMPLATE
        text DESCRIPTION
        boolean IS_ACTIVE
        int PROMPT_PRIORITY
        varchar VERSION
        datetime CREATED_AT
        datetime UPDATED_AT
        bigint CREATED_BY_USER_ID FK
        bigint PARENT_PROMPT_ID FK
        bigint CREATED_CHANGE_LOG_ID FK
        datetime DELETED_AT
    }

    EVALUATION_SUBJECTIVE_PROMPTS {
        bigint ID PK
        varchar NAME
        text PROMPT_TEMPLATE
        text DESCRIPTION
        json EVALUATION_CRITERIA_FOCUS
        text SCORING_INSTRUCTION
        text OUTPUT_FORMAT_INSTRUCTION
        boolean IS_ACTIVE
        varchar VERSION
        datetime CREATED_AT
        datetime UPDATED_AT
        bigint CREATED_BY_USER_ID FK
        bigint PARENT_PROMPT_ID FK
        bigint CREATED_CHANGE_LOG_ID FK
        datetime DELETED_AT
    }

    EVALUATION_PROMPT_ASSEMBLY_CONFIGS {
        bigint ID PK
        varchar NAME
        text DESCRIPTION
        text BASE_SYSTEM_PROMPT
        varchar TAG_PROMPTS_SECTION_HEADER
        varchar SUBJECTIVE_SECTION_HEADER
        varchar TAG_PROMPT_SEPARATOR
        varchar SECTION_SEPARATOR
        text FINAL_INSTRUCTION
        boolean IS_ACTIVE
        datetime CREATED_AT
        datetime UPDATED_AT
        bigint CREATED_BY_USER_ID FK
        bigint CREATED_CHANGE_LOG_ID FK
    }

    %% 数据集管理
    DATASET_VERSIONS {
        bigint ID PK
        varchar VERSION_NUMBER UK
        varchar NAME
        text DESCRIPTION
        datetime CREATION_TIME
        bigint CREATED_BY_USER_ID FK
        bigint CREATED_CHANGE_LOG_ID FK
        datetime DELETED_AT
    }

    DATASET_QUESTION_MAPPING {
        bigint ID PK
        bigint DATASET_VERSION_ID FK
        bigint STANDARD_QUESTION_ID FK
        int ORDER_IN_DATASET
        datetime CREATED_AT
        bigint CREATED_BY_USER_ID FK
        bigint CREATED_CHANGE_LOG_ID FK
    }

    %% LLM模型管理
    LLM_MODELS {
        bigint ID PK
        varchar NAME
        varchar PROVIDER
        varchar VERSION
        text DESCRIPTION
        varchar API_URL
        varchar API_KEY
        varchar API_TYPE
        json MODEL_PARAMETERS
        datetime CREATED_AT
        bigint CREATED_BY_USER_ID FK
        bigint CREATED_CHANGE_LOG_ID FK
        datetime DELETED_AT
    }

    %% 答案生成系统
    ANSWER_GENERATION_BATCHES {
        bigint ID PK
        varchar NAME
        text DESCRIPTION
        bigint DATASET_VERSION_ID FK
        datetime CREATION_TIME
        enum STATUS
        bigint ANSWER_ASSEMBLY_CONFIG_ID FK
        bigint SINGLE_CHOICE_PROMPT_ID FK
        bigint MULTIPLE_CHOICE_PROMPT_ID FK
        bigint SIMPLE_FACT_PROMPT_ID FK
        bigint SUBJECTIVE_PROMPT_ID FK
        json GLOBAL_PARAMETERS
        bigint CREATED_BY_USER_ID FK
        datetime COMPLETED_AT
        decimal PROGRESS_PERCENTAGE
        datetime LAST_ACTIVITY_TIME
        datetime LAST_CHECK_TIME
        int RESUME_COUNT
        datetime PAUSE_TIME
        text PAUSE_REASON
        int ANSWER_REPEAT_COUNT
        text ERROR_MESSAGE
        varchar PROCESSING_INSTANCE
        bigint LAST_PROCESSED_RUN_ID FK
    }

    MODEL_ANSWER_RUNS {
        bigint ID PK
        bigint ANSWER_GENERATION_BATCH_ID FK
        bigint LLM_MODEL_ID FK
        varchar RUN_NAME
        text RUN_DESCRIPTION
        int RUN_INDEX
        datetime RUN_TIME
        enum STATUS
        json PARAMETERS
        text ERROR_MESSAGE
        bigint CREATED_BY_USER_ID FK
        bigint LAST_PROCESSED_QUESTION_ID FK
        int LAST_PROCESSED_QUESTION_INDEX
        decimal PROGRESS_PERCENTAGE
        datetime LAST_ACTIVITY_TIME
        int RESUME_COUNT
        datetime PAUSE_TIME
        text PAUSE_REASON
        int COMPLETED_QUESTIONS_COUNT
        int TOTAL_QUESTIONS_COUNT
        int FAILED_QUESTIONS_COUNT
        json FAILED_QUESTIONS_IDS
    }

    LLM_ANSWERS {
        bigint ID PK
        bigint MODEL_ANSWER_RUN_ID FK
        bigint DATASET_QUESTION_MAPPING_ID FK
        text ANSWER_TEXT
        enum GENERATION_STATUS
        text ERROR_MESSAGE
        datetime GENERATION_TIME
        text PROMPT_USED
        text RAW_MODEL_RESPONSE
        json OTHER_METADATA
        int REPEAT_INDEX
    }

    %% 评估系统核心
    EVALUATION_CRITERIA {
        bigint ID PK
        varchar NAME
        varchar VERSION
        text DESCRIPTION
        enum DATA_TYPE
        varchar SCORE_RANGE
        json APPLICABLE_QUESTION_TYPES
        boolean IS_REQUIRED
        int ORDER_INDEX
        decimal WEIGHT
        enum QUESTION_TYPE
        json OPTIONS
        datetime CREATED_AT
        bigint CREATED_BY_USER_ID FK
        bigint PARENT_CRITERION_ID FK
        bigint CREATED_CHANGE_LOG_ID FK
        datetime DELETED_AT
    }

    EVALUATORS {
        bigint ID PK
        enum EVALUATOR_TYPE
        bigint USER_ID FK
        bigint LLM_MODEL_ID FK
        varchar NAME
        datetime CREATED_AT
        bigint CREATED_BY_USER_ID FK
        bigint CREATED_CHANGE_LOG_ID FK
        datetime DELETED_AT
    }

    EVALUATION_RUNS {
        bigint ID PK
        bigint MODEL_ANSWER_RUN_ID FK
        bigint EVALUATOR_ID FK
        varchar RUN_NAME
        text RUN_DESCRIPTION
        datetime RUN_TIME
        datetime START_TIME
        datetime END_TIME
        enum STATUS
        json PARAMETERS
        bigint EVALUATION_ASSEMBLY_CONFIG_ID FK
        bigint SUBJECTIVE_PROMPT_ID FK
        text ERROR_MESSAGE
        bigint CREATED_BY_USER_ID FK
        bigint LAST_PROCESSED_ANSWER_ID FK
        decimal PROGRESS_PERCENTAGE
        datetime LAST_ACTIVITY_TIME
        int COMPLETED_ANSWERS_COUNT
        int TOTAL_ANSWERS_COUNT
        int FAILED_EVALUATIONS_COUNT
        int RESUME_COUNT
        datetime COMPLETED_AT
        text PAUSE_REASON
        datetime PAUSE_TIME
        bigint PAUSED_BY_USER_ID FK
        int TIMEOUT_SECONDS
        boolean IS_AUTO_RESUME
        int AUTO_CHECKPOINT_INTERVAL
        bigint CURRENT_BATCH_START_ID
        bigint CURRENT_BATCH_END_ID
        int BATCH_SIZE
        int RETRY_COUNT
        int MAX_RETRIES
        datetime LAST_ERROR_TIME
        int CONSECUTIVE_ERRORS
        datetime LAST_UPDATED
    }

    EVALUATIONS {
        bigint ID PK
        bigint LLM_ANSWER_ID FK
        bigint EVALUATOR_ID FK
        bigint EVALUATION_RUN_ID FK
        enum EVALUATION_TYPE
        decimal OVERALL_SCORE
        datetime EVALUATION_TIME
        enum EVALUATION_STATUS
        text ERROR_MESSAGE
        json EVALUATION_RESULTS
        text PROMPT_USED
        text COMMENTS
        text RAW_EVALUATOR_RESPONSE
        bigint CREATED_BY_USER_ID FK
        bigint CREATED_CHANGE_LOG_ID FK
        datetime CREATION_TIME
        datetime COMPLETION_TIME
        decimal RAW_SCORE
        decimal NORMALIZED_SCORE
        decimal WEIGHTED_SCORE
        varchar SCORE_TYPE
        varchar SCORING_METHOD
    }

    EVALUATION_DETAILS {
        bigint ID PK
        bigint EVALUATION_ID FK
        bigint CRITERION_ID FK
        varchar CRITERION_NAME
        decimal SCORE
        text COMMENTS
        datetime CREATED_AT
    }

    MODEL_BATCH_SCORES {
        bigint ID PK
        bigint BATCH_ID FK
        bigint MODEL_ID FK
        bigint EVALUATOR_ID FK
        varchar SCORE_TYPE
        decimal AVERAGE_SCORE
        int TOTAL_ANSWERS
        int SCORED_ANSWERS
        decimal MAX_SCORE
        decimal MIN_SCORE
        json SCORE_DISTRIBUTION
        datetime CALCULATED_AT
        datetime UPDATED_AT
        bigint CREATED_BY_USER_ID FK
        int REPEAT_INDEX
    }

    %% 关系定义
    USERS ||--o{ CHANGE_LOG : "creates"
    USERS ||--o{ STANDARD_QUESTIONS : "creates"
    USERS ||--o{ EXPERT_CANDIDATE_ANSWERS : "submits"
    USERS ||--o{ CROWDSOURCED_ANSWERS : "submits"
    USERS ||--o{ CROWDSOURCED_ANSWERS : "reviews"
    USERS ||--o{ TAGS : "creates"
    USERS ||--o{ ANSWER_TAG_PROMPTS : "creates"
    USERS ||--o{ ANSWER_QUESTION_TYPE_PROMPTS : "creates"
    USERS ||--o{ ANSWER_PROMPT_ASSEMBLY_CONFIGS : "creates"
    USERS ||--o{ EVALUATION_TAG_PROMPTS : "creates"
    USERS ||--o{ EVALUATION_SUBJECTIVE_PROMPTS : "creates"
    USERS ||--o{ EVALUATION_PROMPT_ASSEMBLY_CONFIGS : "creates"
    USERS ||--o{ DATASET_VERSIONS : "creates"
    USERS ||--o{ LLM_MODELS : "creates"
    USERS ||--o{ ANSWER_GENERATION_BATCHES : "creates"
    USERS ||--o{ MODEL_ANSWER_RUNS : "creates"
    USERS ||--o{ EVALUATION_CRITERIA : "creates"
    USERS ||--o{ EVALUATORS : "creates"
    USERS ||--o{ EVALUATION_RUNS : "creates"
    USERS ||--o{ EVALUATIONS : "creates"

    RAW_QUESTIONS ||--o{ RAW_ANSWERS : "has"
    RAW_QUESTIONS ||--o{ STANDARD_QUESTIONS : "source"
    RAW_QUESTIONS ||--o{ RAW_QUESTION_TAGS : "tagged"

    CHANGE_LOG ||--o{ CHANGE_LOG_DETAILS : "contains"
    CHANGE_LOG ||--o{ STANDARD_QUESTIONS : "tracks"
    CHANGE_LOG ||--o{ TAGS : "tracks"
    CHANGE_LOG ||--o{ ANSWER_TAG_PROMPTS : "tracks"
    CHANGE_LOG ||--o{ EVALUATION_CRITERIA : "tracks"

    STANDARD_QUESTIONS ||--o{ EXPERT_CANDIDATE_ANSWERS : "has"
    STANDARD_QUESTIONS ||--o{ CROWDSOURCED_ANSWERS : "has"
    STANDARD_QUESTIONS ||--o{ STANDARD_OBJECTIVE_ANSWERS : "has"
    STANDARD_QUESTIONS ||--o{ STANDARD_SIMPLE_ANSWERS : "has"
    STANDARD_QUESTIONS ||--o{ STANDARD_SUBJECTIVE_ANSWERS : "has"
    STANDARD_QUESTIONS ||--o{ STANDARD_QUESTION_TAGS : "tagged"
    STANDARD_QUESTIONS ||--o{ DATASET_QUESTION_MAPPING : "included"

    TAGS ||--o{ STANDARD_QUESTION_TAGS : "tags"
    TAGS ||--o{ RAW_QUESTION_TAGS : "tags"
    TAGS ||--o{ ANSWER_TAG_PROMPTS : "prompts"
    TAGS ||--o{ EVALUATION_TAG_PROMPTS : "prompts"

    ANSWER_TAG_PROMPTS ||--o{ ANSWER_TAG_PROMPTS : "versions"
    ANSWER_QUESTION_TYPE_PROMPTS ||--o{ ANSWER_QUESTION_TYPE_PROMPTS : "versions"
    EVALUATION_TAG_PROMPTS ||--o{ EVALUATION_TAG_PROMPTS : "versions"
    EVALUATION_SUBJECTIVE_PROMPTS ||--o{ EVALUATION_SUBJECTIVE_PROMPTS : "versions"

    DATASET_VERSIONS ||--o{ DATASET_QUESTION_MAPPING : "contains"
    DATASET_VERSIONS ||--o{ ANSWER_GENERATION_BATCHES : "uses"

    LLM_MODELS ||--o{ MODEL_ANSWER_RUNS : "runs"
    LLM_MODELS ||--o{ EVALUATORS : "evaluates"

    ANSWER_PROMPT_ASSEMBLY_CONFIGS ||--o{ ANSWER_GENERATION_BATCHES : "configures"
    ANSWER_QUESTION_TYPE_PROMPTS ||--o{ ANSWER_GENERATION_BATCHES : "single_choice"
    ANSWER_QUESTION_TYPE_PROMPTS ||--o{ ANSWER_GENERATION_BATCHES : "multiple_choice"
    ANSWER_QUESTION_TYPE_PROMPTS ||--o{ ANSWER_GENERATION_BATCHES : "simple_fact"
    ANSWER_QUESTION_TYPE_PROMPTS ||--o{ ANSWER_GENERATION_BATCHES : "subjective"

    ANSWER_GENERATION_BATCHES ||--o{ MODEL_ANSWER_RUNS : "contains"
    MODEL_ANSWER_RUNS ||--o{ LLM_ANSWERS : "generates"
    MODEL_ANSWER_RUNS ||--o{ EVALUATION_RUNS : "evaluated"
    MODEL_ANSWER_RUNS ||--o{ ANSWER_GENERATION_BATCHES : "last_processed"

    DATASET_QUESTION_MAPPING ||--o{ LLM_ANSWERS : "answered"

    LLM_ANSWERS ||--o{ EVALUATIONS : "evaluated"

    EVALUATORS ||--o{ EVALUATION_RUNS : "runs"
    EVALUATORS ||--o{ EVALUATIONS : "evaluates"

    EVALUATION_PROMPT_ASSEMBLY_CONFIGS ||--o{ EVALUATION_RUNS : "configures"
    EVALUATION_SUBJECTIVE_PROMPTS ||--o{ EVALUATION_RUNS : "prompts"

    EVALUATION_RUNS ||--o{ EVALUATIONS : "contains"

    EVALUATIONS ||--o{ EVALUATION_DETAILS : "details"

    EVALUATION_CRITERIA ||--o{ EVALUATION_DETAILS : "criteria"
    EVALUATION_CRITERIA ||--o{ EVALUATION_CRITERIA : "versions"

    ANSWER_GENERATION_BATCHES ||--o{ MODEL_BATCH_SCORES : "scored"
    LLM_MODELS ||--o{ MODEL_BATCH_SCORES : "scored"
    EVALUATORS ||--o{ MODEL_BATCH_SCORES : "scores"
```

## 核心模块说明

### 1. 用户和权限管理
- 支持6种用户角色的权限控制
- 完整的用户生命周期管理

### 2. 原始数据采集
- 多源数据采集和清洗
- JSON格式元数据存储

### 3. 变更日志系统
- 类似Git的版本控制机制
- 字段级变更追踪

### 4. 标准问题体系
- 4种问题类型支持
- 3级难度分类
- 版本控制和软删除

### 5. 提示词工程
- 分场景的提示词管理
- 版本控制和组装配置
- 支持标签和题型的精细化提示

### 6. 数据集管理
- 版本化数据集管理
- 问题映射和排序

### 7. LLM集成
- 多模型支持
- 批量答案生成
- 进度追踪和错误处理

### 8. 评估系统
- 多维度评估标准
- 人工和AI评估并存
- 详细的评分统计

## 设计特点

1. **模块化设计**: 8个独立但相互关联的功能模块
2. **版本控制**: 支持问题、提示词、评估标准的版本管理
3. **软删除**: 数据安全和可恢复性
4. **批量处理**: 支持大规模数据处理
5. **实时追踪**: 完整的进度和状态管理
6. **多维评估**: 灵活的评估标准和方法

## 数据流程

```
原始数据 → 标准化 → 数据集 → 答案生成 → 质量评估 → 结果分析
   ↓         ↓        ↓        ↓         ↓         ↓
众包标注 → 专家审核 → 版本管理 → LLM调用 → 多维评估 → 统计报告
``` 