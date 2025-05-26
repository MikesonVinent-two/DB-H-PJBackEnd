package com.example.demo.manager;

import com.example.demo.entity.AnswerGenerationBatch.BatchStatus;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class BatchStateManager {
    private static final Logger logger = LoggerFactory.getLogger(BatchStateManager.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final RedissonClient redissonClient;
    private com.example.demo.task.AnswerGenerationTask answerGenerationTask;

    private static final String BATCH_STATE_PREFIX = "batch:state:";
    private static final String BATCH_INTERRUPT_PREFIX = "batch:interrupt:";
    private static final String BATCH_LOCK_PREFIX = "batch:lock:";
    
    // 定义允许的状态转换
    private static final Map<BatchStatus, Set<BatchStatus>> ALLOWED_TRANSITIONS = new HashMap<>();
    
    static {
        ALLOWED_TRANSITIONS.put(BatchStatus.PENDING, EnumSet.of(BatchStatus.IN_PROGRESS, BatchStatus.PAUSED, BatchStatus.FAILED));
        ALLOWED_TRANSITIONS.put(BatchStatus.IN_PROGRESS, EnumSet.of(BatchStatus.PAUSED, BatchStatus.COMPLETED, BatchStatus.FAILED));
        ALLOWED_TRANSITIONS.put(BatchStatus.PAUSED, EnumSet.of(BatchStatus.RESUMING, BatchStatus.FAILED));
        ALLOWED_TRANSITIONS.put(BatchStatus.RESUMING, EnumSet.of(BatchStatus.IN_PROGRESS, BatchStatus.PAUSED, BatchStatus.FAILED));
        ALLOWED_TRANSITIONS.put(BatchStatus.COMPLETED, EnumSet.of(BatchStatus.FAILED));
        ALLOWED_TRANSITIONS.put(BatchStatus.FAILED, EnumSet.noneOf(BatchStatus.class));
    }

    @Autowired
    public BatchStateManager(RedisTemplate<String, String> redisTemplate, 
                            JdbcTemplate jdbcTemplate, 
                            RedissonClient redissonClient) {
        this.redisTemplate = redisTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.redissonClient = redissonClient;
    }
    
    @Autowired
    @Lazy
    public void setAnswerGenerationTask(com.example.demo.task.AnswerGenerationTask answerGenerationTask) {
        this.answerGenerationTask = answerGenerationTask;
    }

    /**
     * 获取批次状态锁
     * @param batchId 批次ID
     * @return 锁对象
     */
    public RLock getBatchLock(Long batchId) {
        return redissonClient.getLock(BATCH_LOCK_PREFIX + batchId);
    }

    /**
     * 暂停批次
     * @param batchId 批次ID
     * @param reason 暂停原因
     * @return 是否成功暂停
     */
    public boolean pauseBatch(Long batchId, String reason) {
        RLock lock = getBatchLock(batchId);
        try {
            // 获取锁，最多等待5秒，锁定30秒
            if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                try {
                    logger.info("获取到批次{}的锁，开始暂停操作", batchId);
                    
                    // 1. 检查当前状态
                    String currentDbStatus = jdbcTemplate.queryForObject(
                        "SELECT status FROM answer_generation_batches WHERE id = ?", 
                        String.class, batchId);
                    
                    logger.info("批次{}当前数据库状态: {}", batchId, currentDbStatus);
                    
                    // 2. 验证状态转换是否合法
                    BatchStatus fromStatus = BatchStatus.valueOf(currentDbStatus);
                    if (!ALLOWED_TRANSITIONS.get(fromStatus).contains(BatchStatus.PAUSED)) {
                        logger.warn("批次{}当前状态{}不允许转换为PAUSED", batchId, fromStatus);
                        return false;
                    }
                    
                    // 3. 设置中断标志
                    setInterruptFlag(batchId, true);
                    logger.info("批次{}已设置中断标志", batchId);
                    
                    // 4. 更新Redis状态
                    setBatchState(batchId, BatchStatus.PAUSED.name());
                    logger.info("批次{}Redis状态已更新为PAUSED", batchId);
                    
                    // 5. 更新数据库状态
                    int updated = jdbcTemplate.update(
                        "UPDATE answer_generation_batches SET status = ?, pause_time = ?, pause_reason = ?, last_activity_time = ? WHERE id = ?",
                        BatchStatus.PAUSED.name(), LocalDateTime.now(), reason, LocalDateTime.now(), batchId);
                    
                    // 6. 更新运行状态
                    int runUpdated = jdbcTemplate.update(
                        "UPDATE model_answer_runs SET status = ?, pause_time = ?, pause_reason = ?, last_activity_time = ? " +
                        "WHERE answer_generation_batch_id = ? AND (status = 'GENERATING_ANSWERS' OR status = 'RESUMING' OR status = 'EVALUATING' OR status = 'PENDING')",
                        "PAUSED", LocalDateTime.now(), reason, LocalDateTime.now(), batchId);
                    
                    logger.info("批次{}数据库状态更新结果: 批次={}, 运行={}", batchId, updated, runUpdated);
                    
                    // 7. 验证状态更新
                    String finalStatus = jdbcTemplate.queryForObject(
                        "SELECT status FROM answer_generation_batches WHERE id = ?", 
                        String.class, batchId);
                    
                    logger.info("批次{}最终状态: {}", batchId, finalStatus);
                    
                    return "PAUSED".equals(finalStatus);
                } finally {
                    lock.unlock();
                    logger.info("批次{}的锁已释放", batchId);
                }
            } else {
                logger.warn("无法获取批次{}的锁，暂停操作失败", batchId);
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("获取批次{}的锁时被中断", batchId, e);
            return false;
        } catch (Exception e) {
            logger.error("暂停批次{}时发生错误", batchId, e);
            return false;
        }
    }

    /**
     * 恢复批次
     * @param batchId 批次ID
     * @return 是否成功恢复
     */
    public boolean resumeBatch(Long batchId) {
        return resumeBatch(batchId, null);
    }

    /**
     * 恢复批次，并在状态更新后执行回调函数
     * @param batchId 批次ID
     * @param onComplete 状态更新完成后的回调函数
     * @return 是否成功恢复
     */
    public boolean resumeBatch(Long batchId, Runnable onComplete) {
        RLock lock = getBatchLock(batchId);
        try {
            // 获取锁，最多等待5秒，锁定30秒
            if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                try {
                    logger.info("获取到批次{}的锁，开始恢复操作", batchId);
                    
                    // 1. 检查当前状态
                    String currentDbStatus = jdbcTemplate.queryForObject(
                        "SELECT status FROM answer_generation_batches WHERE id = ?", 
                        String.class, batchId);
                    
                    logger.info("批次{}当前数据库状态: {}", batchId, currentDbStatus);
                    
                    // 修正可能的状态值异常
                    if (currentDbStatus != null && currentDbStatus.contains("bootRun")) {
                        String correctedStatus = currentDbStatus.replace("bootRun", "");
                        logger.warn("批次{}状态值异常: {}，修正为: {}", batchId, currentDbStatus, correctedStatus);
                        
                        jdbcTemplate.update(
                            "UPDATE answer_generation_batches SET status = ? WHERE id = ?",
                            correctedStatus, batchId);
                        
                        currentDbStatus = correctedStatus;
                    }
                    
                    // 2. 验证状态转换是否合法
                    if (!"PAUSED".equals(currentDbStatus)) {
                        logger.warn("批次{}当前状态{}不是PAUSED，无法恢复", batchId, currentDbStatus);
                        return false;
                    }
                    
                    // 3. 清除中断标志
                    setInterruptFlag(batchId, false);
                    logger.info("批次{}已清除中断标志", batchId);
                    
                    // 3.1 清除任务内存中的中断标志
                    if (answerGenerationTask != null) {
                        answerGenerationTask.clearInterruptionFlag(batchId);
                        logger.info("批次{}的任务内存中断标志已清除", batchId);
                    }
                    
                    // 4. 更新Redis状态
                    setBatchState(batchId, BatchStatus.RESUMING.name());
                    logger.info("批次{}Redis状态已更新为RESUMING", batchId);
                    
                    // 4.1 更新任务内存状态
                    if (answerGenerationTask != null) {
                        answerGenerationTask.updateBatchMemoryState(batchId, BatchStatus.RESUMING.name());
                        logger.info("批次{}的任务内存状态已更新为RESUMING", batchId);
                    }
                    
                    // 5. 更新数据库状态
                    int updated = jdbcTemplate.update(
                        "UPDATE answer_generation_batches SET status = ?, last_activity_time = ? WHERE id = ?",
                        BatchStatus.RESUMING.name(), LocalDateTime.now(), batchId);
                    
                    // 6. 更新运行状态
                    int runUpdated = jdbcTemplate.update(
                        "UPDATE model_answer_runs SET status = ?, last_activity_time = ? " +
                        "WHERE answer_generation_batch_id = ? AND status = 'PAUSED'",
                        "RESUMING", LocalDateTime.now(), batchId);
                    
                    logger.info("批次{}数据库状态更新结果: 批次={}, 运行={}", batchId, updated, runUpdated);
                    
                    // 7. 验证状态更新
                    String finalStatus = jdbcTemplate.queryForObject(
                        "SELECT status FROM answer_generation_batches WHERE id = ?", 
                        String.class, batchId);
                    
                    logger.info("批次{}最终状态: {}", batchId, finalStatus);
                    
                    // 检查最终状态是否有异常，如果有则修正
                    if (finalStatus != null && finalStatus.contains("bootRun")) {
                        String correctedStatus = finalStatus.replace("bootRun", "");
                        logger.warn("批次{}最终状态异常: {}，修正为: {}", batchId, finalStatus, correctedStatus);
                        
                        jdbcTemplate.update(
                            "UPDATE answer_generation_batches SET status = ? WHERE id = ?",
                            correctedStatus, batchId);
                        
                        finalStatus = correctedStatus;
                    }
                    
                    boolean success = "RESUMING".equals(finalStatus);
                    
                    // 8. 如果更新成功并且提供了回调函数，执行回调
                    if (success && onComplete != null) {
                        // 在锁释放前执行回调，确保状态更新和任务启动的原子性
                        try {
                            onComplete.run();
                            logger.info("批次{}已触发恢复回调函数", batchId);
                        } catch (Exception e) {
                            logger.error("批次{}执行恢复回调函数时出错", batchId, e);
                        }
                    }
                    
                    return success;
                } finally {
                    lock.unlock();
                    logger.info("批次{}的锁已释放", batchId);
                }
            } else {
                logger.warn("无法获取批次{}的锁，恢复操作失败", batchId);
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("获取批次{}的锁时被中断", batchId, e);
            return false;
        } catch (Exception e) {
            logger.error("恢复批次{}时发生错误", batchId, e);
            return false;
        }
    }

    /**
     * 设置批次状态
     * @param batchId 批次ID
     * @param state 状态
     */
    public void setBatchState(Long batchId, String state) {
        String key = BATCH_STATE_PREFIX + batchId;
        redisTemplate.opsForValue().set(key, state);
        redisTemplate.expire(key, Duration.ofHours(24));
    }

    /**
     * 获取批次状态
     * @param batchId 批次ID
     * @return 状态
     */
    public String getBatchState(Long batchId) {
        String key = BATCH_STATE_PREFIX + batchId;
        String state = redisTemplate.opsForValue().get(key);
        if (state == null) {
            // 如果Redis中没有状态，从数据库获取
            try {
                state = jdbcTemplate.queryForObject(
                    "SELECT status FROM answer_generation_batches WHERE id = ?", 
                    String.class, batchId);
                if (state != null) {
                    // 更新Redis
                    setBatchState(batchId, state);
                }
            } catch (Exception e) {
                logger.error("从数据库获取批次{}状态失败", batchId, e);
            }
        }
        return state;
    }

    /**
     * 设置中断标志
     * @param batchId 批次ID
     * @param interrupted 是否中断
     */
    public void setInterruptFlag(Long batchId, boolean interrupted) {
        String key = BATCH_INTERRUPT_PREFIX + batchId;
        redisTemplate.opsForValue().set(key, interrupted ? "true" : "false");
        redisTemplate.expire(key, Duration.ofHours(24));
    }

    /**
     * 检查批次是否被标记为中断
     * @param batchId 批次ID
     * @return 是否中断
     */
    public boolean isInterrupted(Long batchId) {
        String key = BATCH_INTERRUPT_PREFIX + batchId;
        String value = redisTemplate.opsForValue().get(key);
        return "true".equals(value);
    }

    /**
     * 同步批次状态
     * 确保Redis和数据库状态一致
     * @param batchId 批次ID
     */
    public void syncBatchState(Long batchId) {
        RLock lock = getBatchLock(batchId);
        try {
            if (lock.tryLock(2, 10, TimeUnit.SECONDS)) {
                try {
                    String dbStatus = jdbcTemplate.queryForObject(
                        "SELECT status FROM answer_generation_batches WHERE id = ?", 
                        String.class, batchId);
                    
                    String redisStatus = getBatchState(batchId);
                    
                    if (dbStatus != null && !dbStatus.equals(redisStatus)) {
                        logger.info("同步批次{}状态: 数据库={}, Redis={}", batchId, dbStatus, redisStatus);
                        setBatchState(batchId, dbStatus);
                        
                        // 如果数据库状态是PAUSED，确保设置中断标志
                        if ("PAUSED".equals(dbStatus)) {
                            setInterruptFlag(batchId, true);
                        } else if ("RESUMING".equals(dbStatus) || "IN_PROGRESS".equals(dbStatus)) {
                            setInterruptFlag(batchId, false);
                        }
                    }
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("同步批次{}状态时被中断", batchId, e);
        } catch (Exception e) {
            logger.error("同步批次{}状态时发生错误", batchId, e);
        }
    }
} 