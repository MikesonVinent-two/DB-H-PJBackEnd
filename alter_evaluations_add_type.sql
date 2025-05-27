-- 添加evaluation_type字段到evaluations表
ALTER TABLE `evaluations` 
ADD COLUMN `evaluation_type` ENUM('MANUAL', 'AUTO', 'AI_MODEL') NOT NULL DEFAULT 'AI_MODEL' 
COMMENT '评测类型' AFTER `evaluation_run_id`;

-- 添加索引提高查询性能
ALTER TABLE `evaluations` 
ADD INDEX `IDX_EVALUATIONS_TYPE` (`evaluation_type`);

-- 修改评测状态枚举，增加PROCESSING状态
ALTER TABLE `evaluations` 
MODIFY COLUMN `evaluation_status` ENUM('SUCCESS', 'FAILED', 'PENDING', 'PROCESSING') 
NOT NULL DEFAULT 'PENDING' COMMENT '评测状态'; 