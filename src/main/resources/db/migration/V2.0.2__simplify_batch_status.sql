-- 删除不必要的字段
ALTER TABLE answer_generation_batches
DROP COLUMN last_processed_question_id,
DROP COLUMN last_processed_run_id,
DROP COLUMN checkpoint_data; 