SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================
-- 核心用户和权限管理
-- =============================================

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
    `deleted_at` DATETIME NULL COMMENT '软删除标记，非空表示已删除',
    INDEX `idx_users_role` (`role`),
    INDEX `idx_users_deleted` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- 原始数据采集
-- =============================================

-- 2. raw_questions (原始问题表)
DROP TABLE IF EXISTS `raw_questions`;
CREATE TABLE `raw_questions` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `source_url` VARCHAR(512) UNIQUE NOT NULL COMMENT '原始来源URL',
    `source_site` VARCHAR(255) NULL COMMENT '原始站点名称',
    `title` VARCHAR(512) NOT NULL,
    `content` TEXT NULL COMMENT '问题描述或详细内容',
    `crawl_time` DATETIME NOT NULL,
    `tags` JSON NULL COMMENT '标签列表，例如: ["医学", "疾病", "治疗"]',
    `other_metadata` JSON NULL COMMENT '存储原始站点的其他信息 (e.g., 原始ID, 作者)',
    INDEX `idx_raw_questions_site_time` (`source_site`, `crawl_time`),
    INDEX `idx_raw_questions_crawl_time` (`crawl_time`)
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
    FOREIGN KEY (`raw_question_id`) REFERENCES `raw_questions`(`id`) ON DELETE CASCADE,
    INDEX `idx_raw_answers_question` (`raw_question_id`),
    INDEX `idx_raw_answers_accepted` (`is_accepted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- 变更日志系统
-- =============================================

-- 4. change_log (变更日志主表)
DROP TABLE IF EXISTS `change_log`;
CREATE TABLE `change_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `change_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `changed_by_user_id` BIGINT NOT NULL,
    `change_type` VARCHAR(255) NOT NULL COMMENT '变更集的总体类型',
    `commit_message` TEXT NULL COMMENT '类似Git的提交消息，描述本次变更的目的或内容',
    `associated_standard_question_id` BIGINT NULL COMMENT '本次变更集主要关联的标准问题ID',
    FOREIGN KEY (`changed_by_user_id`) REFERENCES `users`(`id`) ON DELETE RESTRICT,
    INDEX `idx_change_log_user_time` (`changed_by_user_id`, `change_time`),
    INDEX `idx_change_log_question` (`associated_standard_question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. change_log_details (变更日志详情表)
DROP TABLE IF EXISTS `change_log_details`;
CREATE TABLE `change_log_details` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `change_log_id` BIGINT NOT NULL COMMENT '关联到所属的变更日志主条目',
    `entity_type` ENUM(
    'STANDARD_QUESTION', 
    'STD_OBJECTIVE_ANSWER', 
    'STD_SIMPLE_ANSWER', 
    'STD_SUBJECTIVE_ANSWER', 
    'TAG', 
    'DATASET_VERSION', 
    'LLM_MODEL', 
    'EVALUATOR', 
    'STANDARD_QUESTION_TAGS', 
    'DATASET_QUESTION_MAPPING', 
    'ANSWER_TAG_PROMPT', 
    'ANSWER_QUESTION_TYPE_PROMPT', 
    'EVALUATION_TAG_PROMPT', 
    'EVALUATION_SUBJECTIVE_PROMPT',
    'ANSWER_PROMPT_ASSEMBLY_CONFIG',
    'EVALUATION_PROMPT_ASSEMBLY_CONFIG'
) NOT NULL COMMENT '发生变更的实体类型',
    `entity_id` BIGINT NOT NULL COMMENT '发生变更的具体实体记录的ID',
    `attribute_name` VARCHAR(255) NOT NULL COMMENT '发生变更的字段名称',
    `old_value` JSON NULL COMMENT '字段变更前的值',
    `new_value` JSON NULL COMMENT '字段变更后的值',
    FOREIGN KEY (`change_log_id`) REFERENCES `change_log`(`id`) ON DELETE CASCADE,
    INDEX `idx_change_details_log` (`change_log_id`),
    INDEX `idx_change_details_entity` (`entity_type`, `entity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- 标准问题体系
-- =============================================

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
    FOREIGN KEY (`created_change_log_id`) REFERENCES `change_log`(`id`) ON DELETE SET NULL,
    INDEX `idx_standard_questions_type_difficulty` (`question_type`, `difficulty`),
    INDEX `idx_standard_questions_creator_time` (`created_by_user_id`, `creation_time`),
    INDEX `idx_standard_questions_deleted` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add FK for associated_standard_question_id in change_log
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
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    INDEX `idx_candidate_answers_question` (`standard_question_id`),
    INDEX `idx_candidate_answers_user_time` (`user_id`, `submission_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. crowdsourced_answers (众包答案表)
DROP TABLE IF EXISTS `crowdsourced_answers`;
CREATE TABLE `crowdsourced_answers` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `standard_question_id` BIGINT NOT NULL COMMENT '问题ID',
    `user_id` BIGINT NOT NULL COMMENT '提供答案的用户ID',
    `answer_text` TEXT NOT NULL COMMENT '用户提交的答案文本',
    `submission_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    `task_batch_id` BIGINT NULL COMMENT '众包任务批次ID',
    `quality_review_status` ENUM('PENDING', 'ACCEPTED', 'REJECTED', 'FLAGGED') DEFAULT 'PENDING' COMMENT '众包答案的质量审核状态',
    `reviewed_by_user_id` BIGINT NULL COMMENT '审核该众包答案的用户ID',
    `review_time` DATETIME NULL COMMENT '审核时间',
    `review_feedback` TEXT NULL COMMENT '审核反馈',
    `other_metadata` JSON NULL COMMENT '存储其他众包相关元数据',
    UNIQUE (`standard_question_id`, `user_id`, `task_batch_id`) COMMENT '确保一个用户在一个任务批次中对同一个问题只提交一次答案',
    FOREIGN KEY (`standard_question_id`) REFERENCES `standard_questions`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`reviewed_by_user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    INDEX `idx_crowdsourced_status` (`quality_review_status`),
    INDEX `idx_crowdsourced_reviewer_time` (`reviewed_by_user_id`, `review_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- 标准答案体系
-- =============================================

-- 9. standard_objective_answers (标准客观题答案表)
DROP TABLE IF EXISTS `standard_objective_answers`;
CREATE TABLE `standard_objective_answers` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `standard_question_id` BIGINT UNIQUE NOT NULL COMMENT '与标准问题一对一关联',
    `options` JSON NOT NULL COMMENT '所有选项的列表',
    `correct_ids` JSON NOT NULL COMMENT '正确选项的ID列表',
    `determined_by_user_id` BIGINT NOT NULL COMMENT '确定此标准答案的用户',
    `determined_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_change_log_id` BIGINT NULL COMMENT '关联到创建此答案记录的 change_log 条目',
    `deleted_at` DATETIME NULL COMMENT '软删除标记',
    FOREIGN KEY (`standard_question_id`) REFERENCES `standard_questions`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`determined_by_user_id`) REFERENCES `users`(`id`) ON DELETE RESTRICT,
    FOREIGN KEY (`created_change_log_id`) REFERENCES `change_log`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. standard_simple_answers (标准简单题答案表)
DROP TABLE IF EXISTS `standard_simple_answers`;
CREATE TABLE `standard_simple_answers` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `standard_question_id` BIGINT UNIQUE NOT NULL COMMENT '与标准问题一对一关联',
    `answer_text` TEXT NOT NULL COMMENT '主要的标准短回答文本',
    `alternative_answers` JSON NULL COMMENT '可接受的同义词列表或变体',
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
    `standard_question_id` BIGINT UNIQUE NOT NULL COMMENT '与标准问题一对一关联',
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

-- =============================================
-- 标签体系
-- =============================================

-- 12. tags (标签表)
DROP TABLE IF EXISTS `tags`;
CREATE TABLE `tags` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `tag_name` VARCHAR(255) UNIQUE NOT NULL,
    `tag_type` VARCHAR(255) NULL COMMENT '标签类型 (e.g., 疾病, 症状, 治疗)',
    `description` TEXT NULL COMMENT '标签描述',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_by_user_id` BIGINT NULL COMMENT '谁创建的标签',
    `created_change_log_id` BIGINT NULL COMMENT '关联到创建此标签的 change_log 条目',
    `deleted_at` DATETIME NULL COMMENT '软删除标记',
    FOREIGN KEY (`created_by_user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`created_change_log_id`) REFERENCES `change_log`(`id`) ON DELETE SET NULL,
    INDEX `idx_tags_type` (`tag_type`),
    INDEX `idx_tags_deleted` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13. standard_question_tags (标准问题-标签关联表)
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
    UNIQUE (`standard_question_id`, `tag_id`) COMMENT '防止重复标签',
    INDEX `idx_question_tags_question` (`standard_question_id`),
    INDEX `idx_question_tags_tag` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 14. raw_question_tags (原始问题-标签关联表)
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

-- =============================================
-- 回答场景的Prompt系统
-- =============================================

-- 15. answer_tag_prompts (回答场景的标签提示词表)
DROP TABLE IF EXISTS `answer_tag_prompts`;
CREATE TABLE `answer_tag_prompts` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `tag_id` BIGINT NOT NULL COMMENT '关联的标签ID',
    `name` VARCHAR(255) NOT NULL COMMENT '提示词名称',
    `prompt_template` TEXT NOT NULL COMMENT '标签相关的prompt片段',
    `description` TEXT NULL COMMENT '提示词描述',
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否激活使用',
    `prompt_priority` INT NOT NULL DEFAULT 50 COMMENT 'prompt优先级，数字越小越靠前',
    `version` VARCHAR(50) NULL COMMENT '版本号',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by_user_id` BIGINT NOT NULL,
    `parent_prompt_id` BIGINT NULL COMMENT '父版本ID',
    `created_change_log_id` BIGINT NULL,
    `deleted_at` DATETIME NULL,
    FOREIGN KEY (`tag_id`) REFERENCES `tags`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`created_by_user_id`) REFERENCES `users`(`id`) ON DELETE RESTRICT,
    FOREIGN KEY (`parent_prompt_id`) REFERENCES `answer_tag_prompts`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`created_change_log_id`) REFERENCES `change_log`(`id`) ON DELETE SET NULL,
    INDEX `idx_answer_tag_active_priority` (`tag_id`, `is_active`, `prompt_priority`),
    INDEX `idx_answer_tag_version` (`tag_id`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 16. answer_question_type_prompts (回答场景的题型提示词表)
DROP TABLE IF EXISTS `answer_question_type_prompts`;
CREATE TABLE `answer_question_type_prompts` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL COMMENT '提示词名称',
    `question_type` ENUM('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'SIMPLE_FACT', 'SUBJECTIVE') NOT NULL,
    `prompt_template` TEXT NOT NULL COMMENT '题型相关的prompt片段',
    `description` TEXT NULL,
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
    `response_format_instruction` TEXT NULL COMMENT '回答格式要求',
    `response_example` TEXT NULL COMMENT '回答示例',
    `version` VARCHAR(50) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by_user_id` BIGINT NOT NULL,
    `parent_prompt_id` BIGINT NULL,
    `created_change_log_id` BIGINT NULL,
    `deleted_at` DATETIME NULL,
    FOREIGN KEY (`created_by_user_id`) REFERENCES `users`(`id`) ON DELETE RESTRICT,
    FOREIGN KEY (`parent_prompt_id`) REFERENCES `answer_question_type_prompts`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`created_change_log_id`) REFERENCES `change_log`(`id`) ON DELETE SET NULL,
    INDEX `idx_answer_type_active` (`question_type`, `is_active`),
    INDEX `idx_answer_type_version` (`question_type`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 17. answer_prompt_assembly_configs (回答场景的prompt组装配置表)
DROP TABLE IF EXISTS `answer_prompt_assembly_configs`;
CREATE TABLE `answer_prompt_assembly_configs` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL COMMENT '配置名称',
    `description` TEXT NULL,
    `base_system_prompt` TEXT NULL COMMENT '基础系统prompt',
    `tag_prompts_section_header` VARCHAR(255) NULL DEFAULT '## 专业知识指导' COMMENT '标签prompts部分的标题',
    `question_type_section_header` VARCHAR(255) NULL DEFAULT '## 回答要求' COMMENT '题型prompt部分的标题',
    `tag_prompt_separator` VARCHAR(100) NULL DEFAULT '\n\n' COMMENT '标签prompts之间的分隔符',
    `section_separator` VARCHAR(100) NULL DEFAULT '\n\n' COMMENT '不同部分之间的分隔符',
    `final_instruction` TEXT NULL COMMENT '最终指令',
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by_user_id` BIGINT NOT NULL,
    `created_change_log_id` BIGINT NULL,
    FOREIGN KEY (`created_by_user_id`) REFERENCES `users`(`id`) ON DELETE RESTRICT,
    FOREIGN KEY (`created_change_log_id`) REFERENCES `change_log`(`id`) ON DELETE SET NULL,
    INDEX `idx_answer_config_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- 评测场景的Prompt系统
-- =============================================

-- 18. evaluation_tag_prompts (评测场景的标签提示词表)
DROP TABLE IF EXISTS `evaluation_tag_prompts`;
CREATE TABLE `evaluation_tag_prompts` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `tag_id` BIGINT NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `prompt_template` TEXT NOT NULL COMMENT '评测相关的标签prompt片段',
    `description` TEXT NULL,
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
    `prompt_priority` INT NOT NULL DEFAULT 50,
    `version` VARCHAR(50) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by_user_id` BIGINT NOT NULL,
    `parent_prompt_id` BIGINT NULL,
    `created_change_log_id` BIGINT NULL,
    `deleted_at` DATETIME NULL,
    FOREIGN KEY (`tag_id`) REFERENCES `tags`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`created_by_user_id`) REFERENCES `users`(`id`) ON DELETE RESTRICT,
    FOREIGN KEY (`parent_prompt_id`) REFERENCES `evaluation_tag_prompts`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`created_change_log_id`) REFERENCES `change_log`(`id`) ON DELETE SET NULL,
    INDEX `idx_eval_tag_active_priority` (`tag_id`, `is_active`, `prompt_priority`),
    INDEX `idx_eval_tag_version` (`tag_id`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 19. evaluation_subjective_prompts (评测场景的主观题提示词表)
DROP TABLE IF EXISTS `evaluation_subjective_prompts`;
CREATE TABLE `evaluation_subjective_prompts` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL,
    `prompt_template` TEXT NOT NULL COMMENT '主观题评测的prompt片段',
    `description` TEXT NULL,
    `evaluation_criteria_focus` JSON NULL COMMENT '重点关注的评测标准ID列表',
    `scoring_instruction` TEXT NULL COMMENT '评分指导',
    `output_format_instruction` TEXT NULL COMMENT '输出格式要求',
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
    `version` VARCHAR(50) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by_user_id` BIGINT NOT NULL,
    `parent_prompt_id` BIGINT NULL,
    `created_change_log_id` BIGINT NULL,
    `deleted_at` DATETIME NULL,
    FOREIGN KEY (`created_by_user_id`) REFERENCES `users`(`id`) ON DELETE RESTRICT,
    FOREIGN KEY (`parent_prompt_id`) REFERENCES `evaluation_subjective_prompts`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`created_change_log_id`) REFERENCES `change_log`(`id`) ON DELETE SET NULL,
    INDEX `idx_eval_subj_active` (`is_active`),
    INDEX `idx_eval_subj_version` (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 20. evaluation_prompt_assembly_configs (评测场景的prompt组装配置表)
DROP TABLE IF EXISTS `evaluation_prompt_assembly_configs`;
CREATE TABLE `evaluation_prompt_assembly_configs` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL,
    `description` TEXT NULL,
    `base_system_prompt` TEXT NULL COMMENT '评测基础系统prompt',
    `tag_prompts_section_header` VARCHAR(255) NULL DEFAULT '## 专业评测标准' COMMENT '标签prompts部分标题',
    `subjective_section_header` VARCHAR(255) NULL DEFAULT '## 主观题评测要求' COMMENT '主观题评测部分标题',
    `tag_prompt_separator` VARCHAR(100) NULL DEFAULT '\n\n',
    `section_separator` VARCHAR(100) NULL DEFAULT '\n\n',
    `final_instruction` TEXT NULL COMMENT '最终评测指令',
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by_user_id` BIGINT NOT NULL,
    `created_change_log_id` BIGINT NULL,
    FOREIGN KEY (`created_by_user_id`) REFERENCES `users`(`id`) ON DELETE RESTRICT,
    FOREIGN KEY (`created_change_log_id`) REFERENCES `change_log`(`id`) ON DELETE SET NULL,
    INDEX `idx_eval_config_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- 数据集版本管理
-- =============================================

-- 21. dataset_versions (数据集版本表)
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
    FOREIGN KEY (`created_change_log_id`) REFERENCES `change_log`(`id`) ON DELETE SET NULL,
    INDEX `idx_dataset_versions_time` (`creation_time`),
    INDEX `idx_dataset_versions_creator` (`created_by_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 22. dataset_question_mapping (数据集-问题映射表)
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
    UNIQUE (`dataset_version_id`, `standard_question_id`) COMMENT '防止同一问题在同一数据集版本中重复出现',
    INDEX `idx_dataset_mapping_dataset` (`dataset_version_id`, `order_in_dataset`),
    INDEX `idx_dataset_mapping_question` (`standard_question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- LLM模型和评测系统
-- =============================================

-- 23. llm_models (LLM模型表)
DROP TABLE IF EXISTS `llm_models`;
CREATE TABLE `llm_models` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL COMMENT '模型名称',
    `provider` VARCHAR(255) NULL COMMENT '模型提供商',
    `version` VARCHAR(255) NULL COMMENT '模型版本',
    `description` TEXT NULL COMMENT '模型描述',
    `api_url` VARCHAR(512) NULL COMMENT 'API接口地址',
    `api_key` VARCHAR(512) NULL COMMENT 'API密钥',
    `api_type` VARCHAR(50) NULL COMMENT '调用方式或接口类型，如OpenAI、Azure、Anthropic等',
    `model_parameters` JSON NULL COMMENT '默认模型参数',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_by_user_id` BIGINT NULL,
    `created_change_log_id` BIGINT NULL COMMENT '关联到创建此模型记录的 change_log 条目',
    `deleted_at` DATETIME NULL COMMENT '软删除标记',
    FOREIGN KEY (`created_by_user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`created_change_log_id`) REFERENCES `change_log`(`id`) ON DELETE SET NULL,
    INDEX `idx_llm_models_provider` (`provider`),
    INDEX `idx_llm_models_api_type` (`api_type`),
    INDEX `idx_llm_models_deleted` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 24.  _batches (回答生成批次表)
DROP TABLE IF EXISTS `answer_generation_batches`;
CREATE TABLE `answer_generation_batches` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL COMMENT '回答生成批次名称',
    `description` TEXT NULL COMMENT '回答生成批次描述',
    `dataset_version_id` BIGINT NOT NULL COMMENT '使用的数据集版本',
    `creation_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `status` ENUM('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'PAUSED', 'RESUMING') NOT NULL DEFAULT 'PENDING' COMMENT '整体状态',
    `answer_assembly_config_id` BIGINT NULL COMMENT '回答阶段使用的prompt组装配置',
    `evaluation_assembly_config_id` BIGINT NULL COMMENT '评测阶段使用的prompt组装配置',
    `global_parameters` JSON NULL COMMENT '应用于所有模型的全局参数',
    `created_by_user_id` BIGINT NULL COMMENT '创建者',
    `completed_at` DATETIME NULL COMMENT '完成时间',
    `last_processed_question_id` BIGINT NULL COMMENT '上次处理到的问题ID',
    `last_processed_run_id` BIGINT NULL COMMENT '上次处理到的运行ID',
    `progress_percentage` DECIMAL(5,2) NULL COMMENT '完成百分比',
    `last_activity_time` DATETIME NULL COMMENT '最后活动时间',
    `checkpoint_data` JSON NULL COMMENT '断点续传的检查点数据',
    `resume_count` INT NOT NULL DEFAULT 0 COMMENT '重启次数',
    `pause_time` DATETIME NULL COMMENT '暂停时间',
    `pause_reason` TEXT NULL COMMENT '暂停原因',
    `answer_repeat_count` INT NOT NULL DEFAULT 1 COMMENT '每个问题获取回答的次数',
    `error_message` TEXT NULL COMMENT '错误信息',
    FOREIGN KEY (`dataset_version_id`) REFERENCES `dataset_versions`(`id`) ON DELETE RESTRICT,
    FOREIGN KEY (`created_by_user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`answer_assembly_config_id`) REFERENCES `answer_prompt_assembly_configs`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`evaluation_assembly_config_id`) REFERENCES `evaluation_prompt_assembly_configs`(`id`) ON DELETE SET NULL,
    INDEX `idx_answer_gen_batches_status` (`status`),
    INDEX `idx_answer_gen_batches_dataset` (`dataset_version_id`),
    INDEX `idx_answer_gen_batches_time` (`creation_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 25. model_answer_runs (模型回答运行表)
DROP TABLE IF EXISTS `model_answer_runs`;
CREATE TABLE `model_answer_runs` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `answer_generation_batch_id` BIGINT NOT NULL COMMENT '所属的回答生成批次',
    `llm_model_id` BIGINT NOT NULL COMMENT '生成回答的LLM模型',
    `run_name` VARCHAR(255) NOT NULL COMMENT '运行名称',
    `run_description` TEXT NULL COMMENT '运行描述',
    `run_index` INT NOT NULL DEFAULT 0 COMMENT '同一模型在批次中的运行索引，0表示第一次',
    `run_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '运行时间',
    `status` ENUM('PENDING', 'GENERATING_ANSWERS', 'ANSWER_GENERATION_FAILED', 'READY_FOR_EVALUATION', 'EVALUATING', 'COMPLETED', 'FAILED', 'PAUSED', 'RESUMING') NOT NULL DEFAULT 'PENDING' COMMENT '运行状态',
    `parameters` JSON NULL COMMENT '运行参数，可覆盖批次的全局参数',
    `error_message` TEXT NULL COMMENT '如果失败，记录错误信息',
    `created_by_user_id` BIGINT NULL COMMENT '发起回答生成的用户',
    `last_processed_question_id` BIGINT NULL COMMENT '上次处理到的问题ID',
    `last_processed_question_index` INT NULL COMMENT '上次处理到的问题在数据集中的索引',
    `progress_percentage` DECIMAL(5,2) NULL COMMENT '完成百分比',
    `last_activity_time` DATETIME NULL COMMENT '最后活动时间',
    `checkpoint_data` JSON NULL COMMENT '断点续传的检查点数据',
    `resume_count` INT NOT NULL DEFAULT 0 COMMENT '重启次数',
    `pause_time` DATETIME NULL COMMENT '暂停时间',
    `pause_reason` TEXT NULL COMMENT '暂停原因',
    `completed_questions_count` INT NOT NULL DEFAULT 0 COMMENT '已完成问题数量',
    `total_questions_count` INT NULL COMMENT '总问题数量',
    `failed_questions_count` INT NOT NULL DEFAULT 0 COMMENT '失败的问题数量',
    `failed_questions_ids` JSON NULL COMMENT '失败的问题ID列表',
    FOREIGN KEY (`answer_generation_batch_id`) REFERENCES `answer_generation_batches`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`llm_model_id`) REFERENCES `llm_models`(`id`) ON DELETE RESTRICT,
    FOREIGN KEY (`created_by_user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    UNIQUE (`answer_generation_batch_id`, `llm_model_id`, `run_index`) COMMENT '确保同一个批次中同一个模型的运行索引唯一',
    INDEX `idx_model_answer_runs_status` (`status`),
    INDEX `idx_model_answer_runs_batch_model` (`answer_generation_batch_id`, `llm_model_id`),
    INDEX `idx_model_answer_runs_progress` (`progress_percentage`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 26. run_checkpoints (运行检查点表)
DROP TABLE IF EXISTS `run_checkpoints`;
CREATE TABLE `run_checkpoints` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `evaluation_run_id` BIGINT NOT NULL COMMENT '关联的评测运行',
    `checkpoint_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '检查点时间',
    `checkpoint_type` ENUM('AUTO', 'MANUAL', 'ERROR', 'PAUSE', 'RESUME') NOT NULL COMMENT '检查点类型',
    `last_processed_question_id` BIGINT NULL COMMENT '上次处理到的问题ID',
    `progress_percentage` DECIMAL(5,2) NULL COMMENT '当前进度百分比',
    `checkpoint_data` JSON NULL COMMENT '检查点详细数据',
    `status_before_checkpoint` VARCHAR(50) NULL COMMENT '检查点前的状态',
    `notes` TEXT NULL COMMENT '备注信息',
    FOREIGN KEY (`evaluation_run_id`) REFERENCES `evaluation_runs`(`id`) ON DELETE CASCADE,
    INDEX `idx_checkpoints_run_time` (`evaluation_run_id`, `checkpoint_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 27. llm_answers (LLM回答表)
DROP TABLE IF EXISTS `llm_answers`;
CREATE TABLE `llm_answers` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `model_answer_run_id` BIGINT NOT NULL COMMENT '所属的模型回答运行',
    `dataset_question_mapping_id` BIGINT NOT NULL COMMENT '对应的数据集问题',
    `answer_text` TEXT NULL COMMENT 'LLM生成的答案文本',
    `generation_status` ENUM('SUCCESS', 'FAILED') NOT NULL COMMENT '生成状态',
    `error_message` TEXT NULL COMMENT '如果生成失败，记录错误信息',
    `generation_time` DATETIME NULL COMMENT '答案生成时间',
    `prompt_used` TEXT NULL COMMENT '生成答案时使用的prompt',
    `raw_model_response` TEXT NULL COMMENT '模型的原始响应',
    `other_metadata` JSON NULL COMMENT '其他元数据',
    `repeat_index` INT NOT NULL DEFAULT 0 COMMENT '重复回答的索引，0表示第一次',
    FOREIGN KEY (`model_answer_run_id`) REFERENCES `model_answer_runs`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`dataset_question_mapping_id`) REFERENCES `dataset_question_mapping`(`id`) ON DELETE CASCADE,
    INDEX `idx_llm_answers_run_status` (`model_answer_run_id`, `generation_status`),
    INDEX `idx_llm_answers_question` (`dataset_question_mapping_id`),
    INDEX `idx_llm_answers_time` (`generation_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 28. evaluations (评测结果表)
DROP TABLE IF EXISTS `evaluations`;
CREATE TABLE `evaluations` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `llm_answer_id` BIGINT NOT NULL COMMENT '关联的LLM回答ID',
    `evaluator_id` BIGINT NOT NULL COMMENT '评测员/裁判ID',
    `evaluation_run_id` BIGINT NULL COMMENT '关联的评测运行ID',
    `overall_score` DECIMAL(5,2) NULL COMMENT '总体评分',
    `evaluation_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评测时间',
    `evaluation_status` ENUM('SUCCESS', 'FAILED', 'PENDING') NOT NULL DEFAULT 'PENDING' COMMENT '评测状态',
    `error_message` TEXT NULL COMMENT '如果评测失败，记录错误信息',
    `evaluation_results` JSON NULL COMMENT '详细评测结果，包含各评测标准的得分',
    `prompt_used` TEXT NULL COMMENT '评测使用的prompt',
    `comments` TEXT NULL COMMENT '评测员的补充说明或建议',
    `raw_evaluator_response` TEXT NULL COMMENT '评测员/裁判的原始响应',
    `created_by_user_id` BIGINT NULL COMMENT '发起评测的用户ID',
    `created_change_log_id` BIGINT NULL COMMENT '关联到创建此评测的change_log条目',
    FOREIGN KEY (`llm_answer_id`) REFERENCES `llm_answers`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`evaluator_id`) REFERENCES `evaluators`(`id`) ON DELETE RESTRICT,
    FOREIGN KEY (`created_by_user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`created_change_log_id`) REFERENCES `change_log`(`id`) ON DELETE SET NULL,
    INDEX `idx_evaluations_answer` (`llm_answer_id`),
    INDEX `idx_evaluations_evaluator` (`evaluator_id`),
    INDEX `idx_evaluations_status` (`evaluation_status`),
    INDEX `idx_evaluations_score` (`overall_score`),
    INDEX `idx_evaluations_time` (`evaluation_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 29. evaluation_runs (评测运行表)
DROP TABLE IF EXISTS `evaluation_runs`;
CREATE TABLE `evaluation_runs` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `model_answer_run_id` BIGINT NOT NULL COMMENT '关联的模型回答运行',
    `evaluator_id` BIGINT NOT NULL COMMENT '评测员/裁判ID',
    `run_name` VARCHAR(255) NOT NULL COMMENT '评测运行名称',
    `run_description` TEXT NULL COMMENT '评测运行描述',
    `run_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '运行开始时间',
    `status` ENUM('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'PAUSED', 'RESUMING') NOT NULL DEFAULT 'PENDING' COMMENT '运行状态',
    `parameters` JSON NULL COMMENT '评测参数',
    `error_message` TEXT NULL COMMENT '如果失败，记录错误信息',
    `created_by_user_id` BIGINT NULL COMMENT '发起评测的用户',
    `last_processed_answer_id` BIGINT NULL COMMENT '上次处理到的回答ID',
    `progress_percentage` DECIMAL(5,2) NULL COMMENT '完成百分比',
    `last_activity_time` DATETIME NULL COMMENT '最后活动时间',
    `completed_answers_count` INT NOT NULL DEFAULT 0 COMMENT '已完成评测的回答数量',
    `total_answers_count` INT NULL COMMENT '总回答数量',
    `failed_evaluations_count` INT NOT NULL DEFAULT 0 COMMENT '失败的评测数量',
    `resume_count` INT NOT NULL DEFAULT 0 COMMENT '重启次数',
    `completed_at` DATETIME NULL COMMENT '完成时间',
    FOREIGN KEY (`model_answer_run_id`) REFERENCES `model_answer_runs`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`evaluator_id`) REFERENCES `evaluators`(`id`) ON DELETE RESTRICT,
    FOREIGN KEY (`created_by_user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    INDEX `idx_evaluation_runs_status` (`status`),
    INDEX `idx_evaluation_runs_answer_run` (`model_answer_run_id`),
    INDEX `idx_evaluation_runs_progress` (`progress_percentage`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 添加外键约束
ALTER TABLE `evaluations` ADD CONSTRAINT `fk_evaluations_run` FOREIGN KEY (`evaluation_run_id`) REFERENCES `evaluation_runs`(`id`) ON DELETE SET NULL;

-- 30. evaluation_criteria (评测标准表)
DROP TABLE IF EXISTS `evaluation_criteria`;
CREATE TABLE `evaluation_criteria` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL COMMENT '标准名称',
    `version` VARCHAR(255) NULL COMMENT '标准版本',
    `description` TEXT NULL COMMENT '标准描述',
    `data_type` ENUM('SCORE', 'BOOLEAN', 'TEXT', 'CATEGORICAL') NOT NULL COMMENT '评分数据类型',
    `score_range` VARCHAR(255) NULL COMMENT '如果是分值类型，定义分值范围',
    `applicable_question_types` JSON NULL COMMENT '适用的问题类型列表',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_by_user_id` BIGINT NOT NULL,
    `parent_criterion_id` BIGINT NULL COMMENT '父标准ID，用于版本控制',
    `created_change_log_id` BIGINT NULL COMMENT '关联到创建此标准的 change_log 条目',
    `deleted_at` DATETIME NULL COMMENT '软删除标记',
    FOREIGN KEY (`created_by_user_id`) REFERENCES `users`(`id`) ON DELETE RESTRICT,
    FOREIGN KEY (`parent_criterion_id`) REFERENCES `evaluation_criteria`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`created_change_log_id`) REFERENCES `change_log`(`id`) ON DELETE SET NULL,
    INDEX `idx_eval_criteria_type` (`data_type`),
    INDEX `idx_eval_criteria_deleted` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 31. evaluators (评测员/裁判模型表)
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
    FOREIGN KEY (`created_by_user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- 性能优化视图
-- =============================================

-- 评测结果摘要视图
CREATE OR REPLACE VIEW `evaluation_summary` AS
SELECT 
    mar.id as run_id,
    mar.run_name,
    lm.name as model_name,
    lm.provider as model_provider,
    dv.name as dataset_name,
    dv.version_number as dataset_version,
    mar.status as run_status,
    mar.progress_percentage,
    mar.completed_questions_count,
    mar.total_questions_count,
    mar.failed_questions_count,
    COUNT(DISTINCT la.id) as total_answers,
    COUNT(DISTINCT e.id) as total_evaluations,
    AVG(e.overall_score) as avg_overall_score,
    mar.run_time,
    mar.last_activity_time
FROM model_answer_runs mar
JOIN llm_models lm ON mar.llm_model_id = lm.id
JOIN answer_generation_batches agb ON mar.answer_generation_batch_id = agb.id
JOIN dataset_versions dv ON agb.dataset_version_id = dv.id
LEFT JOIN llm_answers la ON mar.id = la.model_answer_run_id
LEFT JOIN evaluations e ON la.id = e.llm_answer_id
GROUP BY mar.id, mar.run_name, lm.name, lm.provider, dv.name, dv.version_number, 
         mar.status, mar.progress_percentage, mar.completed_questions_count, 
         mar.total_questions_count, mar.failed_questions_count, mar.run_time, mar.last_activity_time;

-- =============================================
-- 插入示例数据
-- =============================================

-- 插入用户数据
INSERT INTO `users` (`username`, `password`, `name`, `role`, `contact_info`) VALUES
('admin', '$2a$10$rJELrG6LpF1WC1UPD8kZPeYwZR0dDiXXJ5qpkWP0JKZWzKFg3ydJi', '系统管理员', 'ADMIN', 'admin@example.com'),
('expert1', '$2a$10$rJELrG6LpF1WC1UPD8kZPeYwZR0dDiXXJ5qpkWP0JKZWzKFg3ydJi', '张医生', 'EXPERT', 'expert1@example.com'),
('expert2', '$2a$10$rJELrG6LpF1WC1UPD8kZPeYwZR0dDiXXJ5qpkWP0JKZWzKFg3ydJi', '李医生', 'EXPERT', 'expert2@example.com'),
('curator1', '$2a$10$rJELrG6LpF1WC1UPD8kZPeYwZR0dDiXXJ5qpkWP0JKZWzKFg3ydJi', '王编辑', 'CURATOR', 'curator1@example.com');

-- 插入标签数据
INSERT INTO `tags` (`tag_name`, `tag_type`, `description`, `created_by_user_id`) VALUES
('内科', '科室', '内科相关问题', 1),
('外科', '科室', '外科相关问题', 1),
('心脏病', '疾病', '心脏病相关问题', 1),
('高血压', '疾病', '高血压相关问题', 1),
('糖尿病', '疾病', '糖尿病相关问题', 1),
('诊断', '类型', '诊断相关问题', 1),
('治疗', '类型', '治疗相关问题', 1),
('预防', '类型', '预防相关问题', 1);

-- 插入标准问题数据
INSERT INTO `standard_questions` (`question_text`, `question_type`, `difficulty`, `created_by_user_id`) VALUES
('高血压的主要症状有哪些？', 'SUBJECTIVE', 'MEDIUM', 1),
('糖尿病患者的日常饮食应该注意什么？', 'SUBJECTIVE', 'MEDIUM', 1),
('以下哪些是冠心病的典型症状？', 'MULTIPLE_CHOICE', 'MEDIUM', 1),
('正常人的空腹血糖值范围是多少？', 'SINGLE_CHOICE', 'EASY', 1);

-- 插入标准客观题答案
INSERT INTO `standard_objective_answers` (`standard_question_id`, `options`, `correct_ids`, `determined_by_user_id`) VALUES
(3, '[{"id":"A","text":"胸痛"},{"id":"B","text":"气短"},{"id":"C","text":"出汗"},{"id":"D","text":"恶心"}]', '["A","B","C"]', 2),
(4, '[{"id":"A","text":"3.9-6.1 mmol/L"},{"id":"B","text":"7.0-8.0 mmol/L"},{"id":"C","text":"2.0-3.5 mmol/L"},{"id":"D","text":"8.0-10.0 mmol/L"}]', '["A"]', 2);

-- 插入标准主观题答案
INSERT INTO `standard_subjective_answers` (`standard_question_id`, `answer_text`, `scoring_guidance`, `determined_by_user_id`) VALUES
(1, '高血压的主要症状包括：\n1. 头痛，特别是后脑部\n2. 头晕和眩晕\n3. 耳鸣\n4. 心悸\n5. 疲劳\n6. 视物模糊\n7. 失眠\n\n需要注意的是，早期高血压可能没有明显症状，因此定期测量血压很重要。', '评分要点：\n1. 症状的完整性（3分）\n2. 症状的准确性（4分）\n3. 补充说明的合理性（3分）', 2),
(2, '糖尿病患者的日常饮食注意事项：\n1. 控制总热量摄入\n2. 定时定量进餐\n3. 主食以复杂碳水化合物为主\n4. 增加膳食纤维的摄入\n5. 限制单糖和双糖的摄入\n6. 适量摄入优质蛋白\n7. 限制饱和脂肪酸的摄入\n8. 补充适量维生素和矿物质', '评分要点：\n1. 饮食原则的完整性（4分）\n2. 具体建议的实用性（3分）\n3. 说明的合理性（3分）', 2);

-- 插入评测标准
INSERT INTO `evaluation_criteria` (`name`, `description`, `data_type`, `score_range`, `applicable_question_types`, `created_by_user_id`) VALUES
('专业性', '答案在医学专业方面的准确性和规范性', 'SCORE', '0-10', '["SUBJECTIVE", "MULTIPLE_CHOICE", "SINGLE_CHOICE"]', 1),
('完整性', '答案是否完整覆盖了问题的各个方面', 'SCORE', '0-10', '["SUBJECTIVE", "MULTIPLE_CHOICE"]', 1),
('逻辑性', '答案的逻辑结构是否清晰', 'SCORE', '0-10', '["SUBJECTIVE"]', 1),
('实用性', '答案是否具有实际应用价值', 'SCORE', '0-10', '["SUBJECTIVE", "SIMPLE_FACT"]', 1);

-- 插入评测者数据
INSERT INTO `evaluators` (`name`, `evaluator_type`, `llm_model_id`, `created_by_user_id`) VALUES
('GPT-4评测器', 'AI_MODEL', NULL, 1);

INSERT INTO `evaluators` (`name`, `evaluator_type`, `user_id`, `created_by_user_id`) VALUES
('专家评测组A', 'HUMAN', 2, 1);  -- 假设user_id=2是专家用户

-- 插入问题标签关联
INSERT INTO `standard_question_tags` (`standard_question_id`, `tag_id`, `created_by_user_id`) VALUES
(1, 4, 1),  -- 高血压问题关联"高血压"标签
(1, 6, 1),  -- 高血压问题关联"诊断"标签
(2, 5, 1),  -- 糖尿病问题关联"糖尿病"标签
(2, 7, 1),  -- 糖尿病问题关联"治疗"标签
(3, 3, 1),  -- 冠心病问题关联"心脏病"标签
(3, 6, 1),  -- 冠心病问题关联"诊断"标签
(4, 5, 1),  -- 血糖问题关联"糖尿病"标签
(4, 6, 1);  -- 血糖问题关联"诊断"标签

-- =============================================
-- 插入示例Prompt数据
-- =============================================

-- 插入回答场景的标签提示词
INSERT INTO `answer_tag_prompts` (`tag_id`, `name`, `prompt_template`, `description`, `prompt_priority`, `created_by_user_id`) VALUES
(1, '内科基础知识prompt', '作为一名内科医生，你需要：\n1. 使用准确的医学术语\n2. 解释复杂的内科疾病机制\n3. 强调疾病的系统性表现\n4. 注重药物治疗的详细说明', '内科回答的基础指导prompt', 10, 1),
(3, '心脏病专业prompt', '在回答心脏病相关问题时：\n1. 详细说明心血管系统的病理生理变化\n2. 强调症状与体征的关联性\n3. 说明心电图等检查的重要性\n4. 突出用药注意事项和禁忌症', '心脏病问题的专业指导prompt', 20, 1),
(4, '高血压专业prompt', '回答高血压相关问题时：\n1. 强调血压值的正常范围和异常标准\n2. 详细说明生活方式的影响\n3. 解释各类降压药物的作用机制\n4. 说明并发症的预防措施', '高血压问题的专业指导prompt', 20, 1),
(6, '诊断类prompt', '对于诊断类问题：\n1. 系统性列举症状和体征\n2. 说明必要的检查项目\n3. 解释鉴别诊断要点\n4. 强调诊断的金标准', '诊断类问题的通用prompt', 30, 1),
(7, '治疗类prompt', '对于治疗类问题：\n1. 按照循证医学证据等级排序治疗方案\n2. 详细说明用药方案和注意事项\n3. 解释非药物治疗的重要性\n4. 说明治疗效果评估方法', '治疗类问题的通用prompt', 30, 1);

-- 插入回答场景的题型提示词
INSERT INTO `answer_question_type_prompts` (`name`, `question_type`, `prompt_template`, `response_format_instruction`, `response_example`, `created_by_user_id`) VALUES
('单选题回答prompt', 'SINGLE_CHOICE', '请仔细分析每个选项，选择最准确的一个答案。\n需要：\n1. 明确指出正确选项\n2. 解释为什么这个选项是正确的\n3. 简要说明其他选项不正确的原因', '回答格式：\n正确答案：[选项字母]\n选择理由：[解释]\n其他选项分析：[分析]', '正确答案：A\n选择理由：该选项准确描述了正常空腹血糖范围\n其他选项分析：B、C、D的数值都超出正常范围', 1),
('多选题回答prompt', 'MULTIPLE_CHOICE', '请仔细分析所有选项，选择所有正确的答案。\n需要：\n1. 明确指出所有正确选项\n2. 解释每个正确选项的原因\n3. 说明为什么排除其他选项', '回答格式：\n正确答案：[选项字母列表]\n选择理由：[解释]\n排除理由：[分析]', '正确答案：A,B,C\n选择理由：这些都是典型的冠心病症状\n排除理由：D选项不是典型症状', 1),
('主观题回答prompt', 'SUBJECTIVE', '请提供详细、系统的回答。\n需要：\n1. 结构化组织内容\n2. 使用专业准确的医学术语\n3. 提供具体的例子或解释\n4. 注意回答的完整性和逻辑性', '回答格式：\n[主要观点]\n1. [要点1]\n2. [要点2]\n...\n补充说明：[其他重要信息]', '高血压的主要症状：\n1. 头痛（特别是后枕部）\n2. 头晕\n3. 视物模糊\n补充说明：部分患者可能无明显症状', 1);

-- 插入评测场景的标签提示词
INSERT INTO `evaluation_tag_prompts` (`tag_id`, `name`, `prompt_template`, `prompt_priority`, `created_by_user_id`) VALUES
(1, '内科评测prompt', '评估内科问题回答时，请注意：\n1. 医学术语使用的准确性（0-10分）\n2. 病理生理机制解释的深度（0-10分）\n3. 治疗方案的规范性（0-10分）\n4. 整体专业水平（0-10分）', 10, 1),
(3, '心脏病评测prompt', '评估心脏病相关回答时，重点关注：\n1. 心血管病理生理的解释准确性（0-10分）\n2. 症状体征描述的完整性（0-10分）\n3. 检查方法推荐的合理性（0-10分）\n4. 治疗方案的循证医学支持（0-10分）', 20, 1),
(6, '诊断评测prompt', '评估诊断相关回答时，请考虑：\n1. 诊断思路的清晰度（0-10分）\n2. 鉴别诊断的完整性（0-10分）\n3. 检查建议的必要性（0-10分）\n4. 诊断依据的循证等级（0-10分）', 30, 1);

-- 插入评测场景的主观题提示词
INSERT INTO `evaluation_subjective_prompts` (`name`, `prompt_template`, `scoring_instruction`, `output_format_instruction`, `created_by_user_id`) VALUES
('标准主观题评测prompt', '请对以下回答进行全面评估：\n1. 专业性（医学术语、概念准确性）\n2. 完整性（要点覆盖程度）\n3. 逻辑性（条理性、结构性）\n4. 实用性（临床应用价值）', '评分标准：\n专业性（0-25分）：\n- 术语准确：0-10分\n- 概念清晰：0-15分\n\n完整性（0-25分）：\n- 核心要点：0-15分\n- 补充信息：0-10分\n\n逻辑性（0-25分）：\n- 结构完整：0-10分\n- 条理清晰：0-15分\n\n实用性（0-25分）：\n- 临床相关：0-15分\n- 可操作性：0-10分', '请按以下格式输出评分：\n{\n  "专业性": {\n    "分数": X,\n    "评语": "..."\n  },\n  "完整性": {\n    "分数": X,\n    "评语": "..."\n  },\n  "逻辑性": {\n    "分数": X,\n    "评语": "..."\n  },\n  "实用性": {\n    "分数": X,\n    "评语": "..."\n  },\n  "总分": X,\n  "总评": "..."\n}', 1);

-- 插入prompt组装配置
INSERT INTO `answer_prompt_assembly_configs` (`name`, `description`, `base_system_prompt`, `tag_prompts_section_header`, `question_type_section_header`, `final_instruction`, `created_by_user_id`) VALUES
('标准医学回答配置', '用于医学问题回答的标准prompt组装配置', '你是一个专业的医学AI助手，请基于循证医学和最新指南提供准确、专业的回答。', '## 专业知识要求', '## 回答要求', '请严格按照上述要求进行回答，确保专业性和准确性。', 1);

INSERT INTO `evaluation_prompt_assembly_configs` (`name`, `description`, `base_system_prompt`, `tag_prompts_section_header`, `subjective_section_header`, `final_instruction`, `created_by_user_id`) VALUES
('标准医学评测配置', '用于医学问题评测的标准prompt组装配置', '你是一个专业的医学评测专家，请基于专业知识对回答进行客观、公正的评估。', '## 专业评测标准', '## 评分要求', '请严格按照评分标准进行评估，给出详细的评分依据和建议。', 1);