-- 为model_answer_runs表创建临时表，简化状态枚举
CREATE TABLE model_answer_runs_temp LIKE model_answer_runs;

-- 修改临时表的状态枚举
ALTER TABLE model_answer_runs_temp 
MODIFY COLUMN status ENUM('PENDING', 'GENERATING_ANSWERS', 'COMPLETED', 'FAILED', 'PAUSED') 
NOT NULL DEFAULT 'PENDING' COMMENT '运行状态';

-- 将数据从原表迁移到临时表，转换状态值
INSERT INTO model_answer_runs_temp
SELECT 
    id,
    answer_generation_batch_id,
    llm_model_id,
    run_name,
    run_description,
    run_index,
    run_time,
    CASE 
        WHEN status = 'ANSWER_GENERATION_FAILED' THEN 'FAILED'
        WHEN status = 'READY_FOR_EVALUATION' THEN 'COMPLETED'
        WHEN status = 'EVALUATING' THEN 'COMPLETED'
        WHEN status = 'RESUMING' THEN 'GENERATING_ANSWERS'
        ELSE status
    END AS status,
    parameters,
    error_message,
    created_by_user_id,
    last_processed_question_id,
    last_processed_question_index,
    progress_percentage,
    last_activity_time,
    resume_count,
    pause_time,
    pause_reason,
    completed_questions_count,
    total_questions_count,
    failed_questions_count,
    failed_questions_ids
FROM model_answer_runs;

-- 删除原表并重命名临时表
DROP TABLE model_answer_runs;
RENAME TABLE model_answer_runs_temp TO model_answer_runs;

-- 重新添加外键约束
ALTER TABLE model_answer_runs
ADD CONSTRAINT fk_model_answer_runs_batch
FOREIGN KEY (answer_generation_batch_id) REFERENCES answer_generation_batches(id) ON DELETE CASCADE,
ADD CONSTRAINT fk_model_answer_runs_model
FOREIGN KEY (llm_model_id) REFERENCES llm_models(id) ON DELETE RESTRICT,
ADD CONSTRAINT fk_model_answer_runs_user
FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE SET NULL;

-- 重新添加唯一约束和索引
ALTER TABLE model_answer_runs
ADD UNIQUE INDEX idx_unique_batch_model_run (answer_generation_batch_id, llm_model_id, run_index),
ADD INDEX idx_model_answer_runs_status (status),
ADD INDEX idx_model_answer_runs_batch_model (answer_generation_batch_id, llm_model_id),
ADD INDEX idx_model_answer_runs_progress (progress_percentage); 