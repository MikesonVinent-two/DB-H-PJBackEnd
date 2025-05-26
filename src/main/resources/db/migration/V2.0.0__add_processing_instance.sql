-- 向answer_generation_batches表添加processing_instance字段
ALTER TABLE answer_generation_batches
ADD COLUMN processing_instance VARCHAR(100) NULL COMMENT '处理实例标识，用于防止多进程同时处理同一批次';

-- 创建索引以提高查询性能
CREATE INDEX idx_batches_processing_instance ON answer_generation_batches(processing_instance);

-- 初始化所有现有批次的processing_instance为NULL
UPDATE answer_generation_batches SET processing_instance = NULL; 