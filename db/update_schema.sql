-- 添加last_check_time字段到answer_generation_batches表
-- 用于任务调度器追踪批次的检查时间
SET @dbname = DATABASE();
SET @tablename = "answer_generation_batches";
SET @columnname = "last_check_time";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (COLUMN_NAME = @columnname)
  ) > 0,
  "SELECT 1",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " DATETIME NULL COMMENT '最近一次调度器检查时间';")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 更新现有记录的last_check_time字段
UPDATE `answer_generation_batches` 
SET `last_check_time` = `last_activity_time` 
WHERE `last_check_time` IS NULL; 