SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. users (用户/专家表)
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `username` VARCHAR(255) UNIQUE NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `name` VARCHAR(255) NULL,
    `role` ENUM('ADMIN', 'CURATOR', 'EXPERT', 'ANNOTATOR', 'REFEREE', 'CROWDSOURCE_USER') NOT NULL COMMENT '用户角色，决定其系统权限',
    `contact_info` VARCHAR(255) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at` DATETIME NULL COMMENT '软删除标记，非空表示已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. raw_questions (原始问题表)
DROP TABLE IF EXISTS `raw_questions`;
CREATE TABLE `raw_questions` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `source_url` VARCHAR(255) UNIQUE NOT NULL COMMENT '原始来源URL',
    `source_site` VARCHAR(255) NULL COMMENT '原始站点名称',
    `title` VARCHAR(512) NOT NULL,
    `content` TEXT NULL COMMENT '问题描述或详细内容',
    `crawl_time` DATETIME NOT NULL,
    `tags` JSON NULL COMMENT '标签列表，例如: ["医学", "疾病", "治疗"]',
    `other_metadata` JSON NULL COMMENT '存储原始站点的其他信息 (e.g., 原始ID, 作者)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. raw_answers (原始回答表)
DROP TABLE IF EXISTS `raw_answers`;
CREATE TABLE `raw_answers` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `raw_question_id` BIGINT NOT NULL,
    `author_info` VARCHAR(255) NULL COMMENT '回答者信息',
    `content` TEXT NOT NULL,
    `publish_time` DATETIME NULL,
    `upvotes` INT DEFAULT 0,
    `is_accepted` BOOLEAN NULL COMMENT '是否被采纳为最佳答案',
    `other_metadata` JSON NULL COMMENT '存储原始站点的其他信息',
    FOREIGN KEY (`raw_question_id`) REFERENCES `raw_questions`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. change_log (变更日志主表)
DROP TABLE IF EXISTS `change_log`;
CREATE TABLE `change_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `change_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `changed_by_user_id` BIGINT NOT NULL,
    `change_type` VARCHAR(255) NOT NULL COMMENT '变更集的总体类型 (e.g., "Create Question and Answer", "Update Answer Content", "Add Checklist Items", "Update Criterion", "Create Dataset Version")',
    `commit_message` TEXT NULL COMMENT '类似Git的提交消息，描述本次变更的目的或内容',
    `associated_standard_question_id` BIGINT NULL COMMENT '本次变更集主要关联的标准问题ID (可以是新创建的或被修改的某个版本) - 非强制，但常用',
    FOREIGN KEY (`changed_by_user_id`) REFERENCES `users`(`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. change_log_details (变更日志详情表)
DROP TABLE IF EXISTS `change_log_details`;
CREATE TABLE `change_log_details` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `change_log_id` BIGINT NOT NULL COMMENT '关联到所属的变更日志主条目',
    `entity_type` ENUM('STANDARD_QUESTION', 'STD_OBJECTIVE_ANSWER', 'STD_SIMPLE_ANSWER', 'STD_SUBJECTIVE_ANSWER', 'CHECKLIST_ITEM', 'EVAL_CRITERION', 'AI_PROMPT', 'TAG', 'DATASET_VERSION', 'LLM_MODEL', 'EVALUATOR', 'STANDARD_QUESTION_TAGS', 'DATASET_QUESTION_MAPPING', 'AI_PROMPT_TAGS') NOT NULL COMMENT '发生变更的实体类型',
    `entity_id` BIGINT NOT NULL COMMENT '发生变更的具体实体记录的ID',
    `attribute_name` VARCHAR(255) NOT NULL COMMENT '发生变更的字段名称 (e.g., "question_text", "answer_text", "options", "item_text", "deleted_at", "version", "prompt_template", "tag_id", "standard_question_id", "ai_prompt_id", "tag_id")',
    `old_value` JSON NULL COMMENT '字段变更前的值 (创建时为NULL，软删除时是有效数据)',
    `new_value` JSON NULL COMMENT '字段变更后的值 (软删除时是删除时间戳，删除记录时可以是NULL)',
    FOREIGN KEY (`change_log_id`) REFERENCES `change_log`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. standard_questions (标准问题基表)
DROP TABLE IF EXISTS `standard_questions`;
CREATE TABLE `standard_questions` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `original_raw_question_id` BIGINT NULL COMMENT '如果源自原始数据，链接到原始问题',
    `question_text` TEXT NOT NULL,
    `question_type` ENUM('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'SIMPLE_FACT', 'SUBJECTIVE') NOT NULL COMMENT '问题类型',
    `difficulty` ENUM('EASY', 'MEDIUM', 'HARD') NULL,
    `creation_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_by_user_id` BIGINT NOT NULL,
    `parent_standard_question_id` BIGINT NULL COMMENT '指向该问题的前一个版本，用于版本控制链',
    `created_change_log_id` BIGINT NULL COMMENT '关联到创建此特定版本标准问题的 change_log 条目',
    `deleted_at` DATETIME NULL COMMENT '软删除标记，非空表示已删除',
    FOREIGN KEY (`original_raw_question_id`) REFERENCES `raw_questions`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`created_by_user_id`) REFERENCES `users`(`id`) ON DELETE RESTRICT,
    FOREIGN KEY (`parent_standard_question_id`) REFERENCES `standard_questions`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`created_change_log_id`) REFERENCES `change_log`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add FK for associated_standard_question_id in change_log if you decide to enforce it.
ALTER TABLE `change_log` ADD CONSTRAINT `fk_change_log_assoc_sq` FOREIGN KEY (`associated_standard_question_id`) REFERENCES `standard_questions`(`id`) ON DELETE SET NULL;

-- 7. expert_candidate_answers (专家候选答案表)
DROP TABLE IF EXISTS `expert_candidate_answers`;
CREATE TABLE `expert_candidate_answers` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `standard_question_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL COMMENT '提交候选答案的用户',
    `candidate_answer_text` TEXT NOT NULL COMMENT '专家提交的候选答案文本',
    `submission_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `quality_score` INT NULL COMMENT '可选的质量评分',
    `feedback` TEXT NULL COMMENT '可选的反馈或备注',
    FOREIGN KEY (`standard_question_id`) REFERENCES `standard_questions`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. crowdsourced_answers (众包答案表)
DROP TABLE IF EXISTS `crowdsourced_answers`;
CREATE TABLE `crowdsourced_answers` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `standard_question_id` BIGINT NOT NULL COMMENT '问题ID',
    `user_id` BIGINT NOT NULL COMMENT '提供答案的用户ID (通常是 Annotator 或 CrowdsourceUser)',
    `answer_text` TEXT NOT NULL COMMENT '用户提交的答案文本',
    `submission_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    `task_batch_id` BIGINT NULL COMMENT '如果答案是作为某个众包任务的一部分收集的，可以关联任务批次ID (未来可加外键)',
    `quality_review_status` ENUM('PENDING', 'ACCEPTED', 'REJECTED', 'FLAGGED') DEFAULT 'PENDING' COMMENT '众包答案的质量审核状态',
    `reviewed_by_user_id` BIGINT NULL COMMENT '审核该众包答案的用户ID (如 Curator 或 Expert)',
    `review_time` DATETIME NULL COMMENT '审核时间',
    `review_feedback` TEXT NULL COMMENT '审核反馈',
    `other_metadata` JSON NULL COMMENT '存储其他众包相关元数据',
    UNIQUE (`standard_question_id`, `user_id`, `task_batch_id`) COMMENT '确保一个用户在一个任务批次中对同一个问题只提交一次答案',
    FOREIGN KEY (`standard_question_id`) REFERENCES `standard_questions`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`reviewed_by_user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. standard_objective_answers (标准客观题答案表)
DROP TABLE IF EXISTS `standard_objective_answers`;
CREATE TABLE `standard_objective_answers` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `standard_question_id` BIGINT UNIQUE NOT NULL COMMENT '与标准问题一对一/零关联 (当 question_type 是 single_choice 或 multiple_choice)',
    `options` JSON NOT NULL COMMENT '所有选项的列表，例如: [{"id": "A", "text": "..."}, {"id": "B", "text": "..."}]',
    `correct_ids` JSON NOT NULL COMMENT '正确选项的ID列表，例如: ["A"] 或 ["A", "C"]',
    `determined_by_user_id` BIGINT NOT NULL COMMENT '确定此标准答案的用户',
    `determined_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_change_log_id` BIGINT NULL COMMENT '关联到创建此答案记录的 change_log 条目',
    `deleted_at` DATETIME NULL COMMENT '软删除标记 (通常随 standard_question 版本的删除而删除)',
    FOREIGN KEY (`standard_question_id`) REFERENCES `standard_questions`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`determined_by_user_id`) REFERENCES `users`(`id`) ON DELETE RESTRICT,
    FOREIGN KEY (`created_change_log_id`) REFERENCES `change_log`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. standard_simple_answers (标准简单题答案表)
DROP TABLE IF EXISTS `standard_simple_answers`;
CREATE TABLE `standard_simple_answers` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `standard_question_id` BIGINT UNIQUE NOT NULL COMMENT '与标准问题一对一/零关联 (当 question_type 是 simple_fact)',
    `answer_text` TEXT NOT NULL COMMENT '主要的标准短回答文本',
    `alternative_answers` JSON NULL COMMENT '可接受的同义词列表或变体，例如: ["回答变体1", "回答变体2"]',
    `determined_by_user_id` BIGINT NOT NULL,
    `determined_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_change_log_id` BIGINT NULL COMMENT '关联到创建此答案记录的 change_log 条目',
    `deleted_at` DATETIME NULL COMMENT '软删除标记',
    FOREIGN KEY (`standard_question_id`) REFERENCES `standard_questions`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`determined_by_user_id`) REFERENCES `users`(`id`) ON DELETE RESTRICT,
    FOREIGN KEY (`created_change_log_id`) REFERENCES `change_log`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. standard_subjective_answers (标准主观题答案表)
DROP TABLE IF EXISTS `standard_subjective_answers`;
CREATE TABLE `standard_subjective_answers` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `standard_question_id` BIGINT UNIQUE NOT NULL COMMENT '与标准问题一对一/零关联 (当 question_type 是 subjective)',
    `answer_text` TEXT NOT NULL COMMENT '完整的标准长回答文本',
    `scoring_guidance` TEXT NULL COMMENT '对评测员或裁判AI的评分指导说明',
    `determined_by_user_id` BIGINT NOT NULL,
    `determined_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_change_log_id` BIGINT NULL COMMENT '关联到创建此答案记录的 change_log 条目',
    `deleted_at` DATETIME NULL COMMENT '软删除标记',
    FOREIGN KEY (`standard_question_id`) REFERENCES `standard_questions`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`determined_by_user_id`) REFERENCES `users`(`id`) ON DELETE RESTRICT,
    FOREIGN KEY (`created_change_log_id`) REFERENCES `change_log`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -- 12. standard_answer_checklist_items (标准答案检查项表)
-- DROP TABLE IF EXISTS `standard_answer_checklist_items`;
-- CREATE TABLE `standard_answer_checklist_items` (
--     `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
--     `standard_question_id` BIGINT NOT NULL COMMENT '关联到特定的标准问题版本',
--     `item_text` TEXT NOT NULL COMMENT '检查项的具体描述 (e.g., "是否提到了疾病的常见症状")',
--     `item_type` ENUM('POSITIVE_POINT', 'NEGATIVE_POINT', 'STYLE_POINT', 'FACTUAL_CHECK', 'OTHER') NOT NULL COMMENT '检查项的类型',
--     `weight_score` DECIMAL(10, 2) NOT NULL COMMENT '该检查项的分值或权重 (可以是正或负)',
--     `order_in_list` INT NULL COMMENT '在清单中的显示顺序',
--     `guidance` TEXT NULL COMMENT '针对此检查项给评测员的额外指导',
--     `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     `created_by_user_id` BIGINT NOT NULL,
--     `created_change_log_id` BIGINT NULL COMMENT '关联到创建此检查项的 change_log 条目',
--     `deleted_at` DATETIME NULL COMMENT '软删除标记',
--     FOREIGN KEY (`standard_question_id`) REFERENCES `standard_questions`(`id`) ON DELETE CASCADE,
--     FOREIGN KEY (`created_by_user_id`) REFERENCES `users`(`id`) ON DELETE RESTRICT,
--     FOREIGN KEY (`created_change_log_id`) REFERENCES `change_log`(`id`) ON DELETE SET NULL
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13. tags (标签表)
DROP TABLE IF EXISTS `tags`;
CREATE TABLE `tags` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `tag_name` VARCHAR(255) UNIQUE NOT NULL,
    `tag_type` VARCHAR(255) NULL COMMENT '标签类型 (e.g., 疾病, 症状, 治疗)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_by_user_id` BIGINT NULL COMMENT '谁创建的标签',
    `created_change_log_id` BIGINT NULL COMMENT '关联到创建此标签的 change_log 条目',
    `deleted_at` DATETIME NULL COMMENT '软删除标记',
    FOREIGN KEY (`created_by_user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`created_change_log_id`) REFERENCES `change_log`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 14. standard_question_tags (标准问题-标签关联表)
DROP TABLE IF EXISTS `standard_question_tags`;
CREATE TABLE `standard_question_tags` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `standard_question_id` BIGINT NOT NULL,
    `tag_id` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_by_user_id` BIGINT NULL,
    `created_change_log_id` BIGINT NULL COMMENT '关联到创建此关联的 change_log 条目',
    FOREIGN KEY (`standard_question_id`) REFERENCES `standard_questions`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`tag_id`) REFERENCES `tags`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`created_by_user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`created_change_log_id`) REFERENCES `change_log`(`id`) ON DELETE SET NULL,
    UNIQUE (`standard_question_id`, `tag_id`) COMMENT '防止重复标签'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 14.5 raw_question_tags (原始问题-标签关联表)
DROP TABLE IF EXISTS `raw_question_tags`;
CREATE TABLE `raw_question_tags` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `raw_question_id` BIGINT NOT NULL,
    `tag_id` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_by_user_id` BIGINT NULL,
    `created_change_log_id` BIGINT NULL COMMENT '关联到创建此关联的 change_log 条目',
    FOREIGN KEY (`raw_question_id`) REFERENCES `raw_questions`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`tag_id`) REFERENCES `tags`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`created_by_user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`created_change_log_id`) REFERENCES `change_log`(`id`) ON DELETE SET NULL,
    UNIQUE (`raw_question_id`, `tag_id`) COMMENT '防止重复标签'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 15. dataset_versions (数据集版本表)
DROP TABLE IF EXISTS `dataset_versions`;
CREATE TABLE `dataset_versions` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `version_number` VARCHAR(255) UNIQUE NOT NULL COMMENT '版本号 (e.g., "v1.0.0")',
    `name` VARCHAR(255) NOT NULL COMMENT '数据集名称',
    `description` TEXT NULL COMMENT '数据集描述',
    `creation_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_by_user_id` BIGINT NOT NULL,
    `created_change_log_id` BIGINT NULL COMMENT '关联到创建此数据集版本的 change_log 条目',
    `deleted_at` DATETIME NULL COMMENT '软删除标记',
    FOREIGN KEY (`created_by_user_id`) REFERENCES `users`(`id`) ON DELETE RESTRICT,
    FOREIGN KEY (`created_change_log_id`) REFERENCES `change_log`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 16. dataset_question_mapping (数据集-问题映射表)
DROP TABLE IF EXISTS `dataset_question_mapping`;
CREATE TABLE `dataset_question_mapping` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `dataset_version_id` BIGINT NOT NULL,
    `standard_question_id` BIGINT NOT NULL,
    `order_in_dataset` INT NULL COMMENT '在数据集中的顺序',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_by_user_id` BIGINT NULL,
    `created_change_log_id` BIGINT NULL COMMENT '关联到创建此映射的 change_log 条目',
    FOREIGN KEY (`dataset_version_id`) REFERENCES `dataset_versions`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`standard_question_id`) REFERENCES `standard_questions`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`created_by_user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`created_change_log_id`) REFERENCES `change_log`(`id`) ON DELETE SET NULL,
    UNIQUE (`dataset_version_id`, `standard_question_id`) COMMENT '防止同一问题在同一数据集版本中重复出现'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 17. llm_models (LLM模型表)
DROP TABLE IF EXISTS `llm_models`;
CREATE TABLE `llm_models` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL COMMENT '模型名称',
    `provider` VARCHAR(255) NULL COMMENT '模型提供商',
    `version` VARCHAR(255) NULL COMMENT '模型版本',
    `description` TEXT NULL COMMENT '模型描述',
    `api_url` VARCHAR(512) NULL COMMENT 'API接口地址',
    `api_key` VARCHAR(512) NULL COMMENT 'API密钥',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_by_user_id` BIGINT NULL,
    `created_change_log_id` BIGINT NULL COMMENT '关联到创建此模型记录的 change_log 条目',
    `deleted_at` DATETIME NULL COMMENT '软删除标记',
    FOREIGN KEY (`created_by_user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`created_change_log_id`) REFERENCES `change_log`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 18. evaluation_runs (评测运行表)
DROP TABLE IF EXISTS `evaluation_runs`;
CREATE TABLE `evaluation_runs` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `llm_model_id` BIGINT NOT NULL COMMENT '被评测的LLM模型',
    `dataset_version_id` BIGINT NOT NULL COMMENT '使用的数据集版本',
    `run_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '运行时间',
    `status` ENUM('PENDING', 'GENERATING_ANSWERS', 'ANSWER_GENERATION_FAILED', 'READY_FOR_EVALUATION', 'EVALUATING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'PENDING' COMMENT '运行状态',
    `run_description` TEXT NULL COMMENT '运行描述',
    `parameters` JSON NULL COMMENT '运行参数',
    `error_message` TEXT NULL COMMENT '如果失败，记录错误信息',
    `created_by_user_id` BIGINT NULL COMMENT '发起评测的用户',
    FOREIGN KEY (`llm_model_id`) REFERENCES `llm_models`(`id`) ON DELETE RESTRICT,
    FOREIGN KEY (`dataset_version_id`) REFERENCES `dataset_versions`(`id`) ON DELETE RESTRICT,
    FOREIGN KEY (`created_by_user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 19. llm_answers (LLM回答表)
DROP TABLE IF EXISTS `llm_answers`;
CREATE TABLE `llm_answers` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `evaluation_run_id` BIGINT NOT NULL COMMENT '所属的评测运行',
    `dataset_question_mapping_id` BIGINT NOT NULL COMMENT '对应的数据集问题',
    `answer_text` TEXT NULL COMMENT 'LLM生成的答案文本',
    `generation_status` ENUM('SUCCESS', 'FAILED') NOT NULL COMMENT '生成状态',
    `error_message` TEXT NULL COMMENT '如果生成失败，记录错误信息',
    `generation_time` DATETIME NULL COMMENT '答案生成时间',
    `prompt_used` TEXT NULL COMMENT '生成答案时使用的prompt',
    `other_metadata` JSON NULL COMMENT '其他元数据',
    FOREIGN KEY (`evaluation_run_id`) REFERENCES `evaluation_runs`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`dataset_question_mapping_id`) REFERENCES `dataset_question_mapping`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 20. evaluators (评测员/裁判模型表)
DROP TABLE IF EXISTS `evaluators`;
CREATE TABLE `evaluators` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `evaluator_type` ENUM('HUMAN', 'AI_MODEL') NOT NULL COMMENT '评测员类型',
    `user_id` BIGINT NULL COMMENT '如果是人类评测员，关联到用户表',
    `llm_model_id` BIGINT NULL COMMENT '如果是AI评测员，关联到模型表',
    `name` VARCHAR(255) NOT NULL COMMENT '评测员名称',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_by_user_id` BIGINT NULL,
    `created_change_log_id` BIGINT NULL COMMENT '关联到创建此评测员记录的 change_log 条目',
    `deleted_at` DATETIME NULL COMMENT '软删除标记',
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`llm_model_id`) REFERENCES `llm_models`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`created_by_user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`created_change_log_id`) REFERENCES `change_log`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 21. evaluation_criteria (评测标准表)
DROP TABLE IF EXISTS `evaluation_criteria`;
CREATE TABLE `evaluation_criteria` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL COMMENT '标准名称',
    `version` VARCHAR(255) NULL COMMENT '标准版本',
    `description` TEXT NULL COMMENT '标准描述',
    `data_type` ENUM('SCORE', 'BOOLEAN', 'TEXT', 'CATEGORICAL') NOT NULL COMMENT '评分数据类型',
    `score_range` VARCHAR(255) NULL COMMENT '如果是分值类型，定义分值范围 (e.g., "0-100", "1-5")',
    `applicable_question_types` JSON NULL COMMENT '适用的问题类型列表',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_by_user_id` BIGINT NOT NULL,
    `parent_criterion_id` BIGINT NULL COMMENT '父标准ID，用于版本控制',
    `created_change_log_id` BIGINT NULL COMMENT '关联到创建此标准的 change_log 条目',
    `deleted_at` DATETIME NULL COMMENT '软删除标记',
    FOREIGN KEY (`created_by_user_id`) REFERENCES `users`(`id`) ON DELETE RESTRICT,
    FOREIGN KEY (`parent_criterion_id`) REFERENCES `evaluation_criteria`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`created_change_log_id`) REFERENCES `change_log`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 22. checklist_item_criteria (检查项-评测标准关联表)
DROP TABLE IF EXISTS `checklist_item_criteria`;
CREATE TABLE `checklist_item_criteria` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `checklist_item_id` BIGINT NOT NULL,
    `criterion_id` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_by_user_id` BIGINT NULL,
    `created_change_log_id` BIGINT NULL COMMENT '关联到创建此关联的 change_log 条目',
    FOREIGN KEY (`checklist_item_id`) REFERENCES `standard_answer_checklist_items`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`criterion_id`) REFERENCES `evaluation_criteria`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`created_by_user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`created_change_log_id`) REFERENCES `change_log`(`id`) ON DELETE SET NULL,
    UNIQUE (`checklist_item_id`, `criterion_id`) COMMENT '防止重复关联'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 23. ai_evaluation_prompts (AI评测提示词表)
DROP TABLE IF EXISTS `ai_evaluation_prompts`;
CREATE TABLE `ai_evaluation_prompts` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL COMMENT '提示词名称',
    `version` VARCHAR(255) NULL COMMENT '提示词版本',
    `prompt_template` TEXT NOT NULL COMMENT '提示词模板',
    `description` TEXT NULL COMMENT '提示词描述',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_by_user_id` BIGINT NOT NULL,
    `parent_prompt_id` BIGINT NULL COMMENT '父提示词ID，用于版本控制',
    `applicable_question_types` JSON NULL COMMENT '适用的问题类型列表',
    `applicable_criteria_ids` JSON NULL COMMENT '适用的评测标准ID列表',
    `created_change_log_id` BIGINT NULL COMMENT '关联到创建此提示词的 change_log 条目',
    `deleted_at` DATETIME NULL COMMENT '软删除标记',
    CONSTRAINT `fk_ai_prompt_user` FOREIGN KEY (`created_by_user_id`) REFERENCES `users`(`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_ai_prompt_parent` FOREIGN KEY (`parent_prompt_id`) REFERENCES `ai_evaluation_prompts`(`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_ai_prompt_changelog` FOREIGN KEY (`created_change_log_id`) REFERENCES `change_log`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 24. ai_prompt_tags (AI提示词-标签关联表)
DROP TABLE IF EXISTS `ai_prompt_tags`;
CREATE TABLE `ai_prompt_tags` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `ai_prompt_id` BIGINT NOT NULL,
    `tag_id` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_by_user_id` BIGINT NULL,
    `created_change_log_id` BIGINT NULL COMMENT '关联到创建此关联的 change_log 条目',
    FOREIGN KEY (`ai_prompt_id`) REFERENCES `ai_evaluation_prompts`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`tag_id`) REFERENCES `tags`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`created_by_user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`created_change_log_id`) REFERENCES `change_log`(`id`) ON DELETE SET NULL,
    UNIQUE (`ai_prompt_id`, `tag_id`) COMMENT '防止重复标签'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 25. evaluations (评测表)
DROP TABLE IF EXISTS `evaluations`;
CREATE TABLE `evaluations` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `llm_answer_id` BIGINT NOT NULL COMMENT '被评测的LLM答案',
    `evaluator_id` BIGINT NOT NULL COMMENT '评测员',
    `evaluation_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评测时间',
    `overall_score` DECIMAL(10, 2) NULL COMMENT '总体评分',
    `feedback_text` TEXT NULL COMMENT '评测反馈文本',
    `status` ENUM('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'NEEDS_REVIEW') NOT NULL DEFAULT 'COMPLETED' COMMENT '评测状态',
    `raw_evaluator_output` JSON NULL COMMENT '原始评测输出',
    `error_message` TEXT NULL COMMENT '如果评测失败，记录错误信息',
    `ai_prompt_id_used` BIGINT NULL COMMENT '如果是AI评测，使用的提示词',
    `prompt_text_rendered` TEXT NULL COMMENT '如果是AI评测，实际渲染后的提示词文本',
    `created_by_user_id` BIGINT NULL COMMENT '创建此评测记录的用户',
    FOREIGN KEY (`llm_answer_id`) REFERENCES `llm_answers`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`evaluator_id`) REFERENCES `evaluators`(`id`) ON DELETE RESTRICT,
    FOREIGN KEY (`ai_prompt_id_used`) REFERENCES `ai_evaluation_prompts`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`created_by_user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 26. evaluation_scores (评测分数表)
DROP TABLE IF EXISTS `evaluation_scores`;
CREATE TABLE `evaluation_scores` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `evaluation_id` BIGINT NOT NULL,
    `criterion_id` BIGINT NOT NULL,
    `score_value` VARCHAR(255) NOT NULL COMMENT '评分值 (可能是数字分数、布尔值、文本或分类值，取决于criterion的data_type)',
    FOREIGN KEY (`evaluation_id`) REFERENCES `evaluations`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`criterion_id`) REFERENCES `evaluation_criteria`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
