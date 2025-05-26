package com.example.demo.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 批次任务调度器，负责检查和启动需要处理的批次
 */
@Component
public class BatchTaskScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(BatchTaskScheduler.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private AnswerGenerationTask answerGenerationTask;
    
    private ExecutorService taskExecutor;
    private volatile boolean isRunning = false;
    
    @PostConstruct
    public void init() {
        logger.info("初始化批次任务调度器");
        taskExecutor = Executors.newFixedThreadPool(5); // 最多同时处理5个批次
        isRunning = true;
        
        // 系统启动时检查一次，处理之前未完成的批次
        scheduleTaskCheck();
    }
    
    @PreDestroy
    public void destroy() {
        logger.info("关闭批次任务调度器");
        isRunning = false;
        
        if (taskExecutor != null) {
            taskExecutor.shutdown();
            try {
                if (!taskExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    taskExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                taskExecutor.shutdownNow();
            }
        }
    }
    
    /**
     * 定期检查需要处理的批次
     * 每30秒执行一次
     */
    @Scheduled(fixedDelay = 30000)
    public void scheduleTaskCheck() {
        if (!isRunning) {
            return;
        }
        
        try {
            logger.debug("开始检查需要处理的批次");
            
            // 查找所有处于IN_PROGRESS或RESUMING状态的批次
            List<Map<String, Object>> batches = jdbcTemplate.queryForList(
                "SELECT id, status FROM answer_generation_batches " +
                "WHERE status IN ('IN_PROGRESS', 'RESUMING') " +
                "ORDER BY last_activity_time DESC"
            );
            
            if (batches.isEmpty()) {
                logger.debug("没有需要处理的批次");
                return;
            }
            
            logger.info("发现{}个需要处理的批次", batches.size());
            
            // 处理每个批次
            for (Map<String, Object> batch : batches) {
                Long batchId = ((Number) batch.get("id")).longValue();
                String status = (String) batch.get("status");
                
                // 检查该批次当前是否已有处理任务在执行
                boolean hasActiveTask = checkBatchHasActiveTask(batchId);
                
                if (!hasActiveTask) {
                    logger.info("调度批次{}处理，当前状态: {}", batchId, status);
                    scheduleBatchProcessing(batchId, status);
                } else {
                    logger.info("批次{}已有活动任务在执行，跳过调度", batchId);
                }
            }
        } catch (Exception e) {
            logger.error("批次任务调度检查出错", e);
        }
    }
    
    /**
     * 检查批次是否有活动任务在执行
     */
    private boolean checkBatchHasActiveTask(Long batchId) {
        try {
            // 检查批次活动时间，如果在过去3分钟内有活动，则认为有任务在执行
            // 同时检查上次检查时间，如果刚检查过（1分钟内），也认为有任务在执行
            String sql = 
                "SELECT CASE " +
                "  WHEN last_activity_time > ? THEN TRUE " +  // 如果最近有活动
                "  WHEN last_check_time > ? THEN TRUE " +     // 如果最近刚检查过
                "  ELSE FALSE " +
                "END as has_active_task " +
                "FROM answer_generation_batches WHERE id = ?";
                
            Boolean hasActiveTask = jdbcTemplate.queryForObject(
                sql,
                Boolean.class,
                LocalDateTime.now().minusMinutes(3),  // 3分钟内有活动
                LocalDateTime.now().minusMinutes(1),  // 1分钟内有检查
                batchId
            );
            
            // 如果没有活动任务，更新last_check_time
            if (hasActiveTask != null && !hasActiveTask) {
                jdbcTemplate.update(
                    "UPDATE answer_generation_batches SET last_check_time = ? WHERE id = ?",
                    LocalDateTime.now(), batchId
                );
                
                logger.debug("批次{}没有活动任务，更新检查时间", batchId);
                return false;
            }
            
            return hasActiveTask != null && hasActiveTask;
        } catch (Exception e) {
            logger.error("检查批次{}活动状态出错", batchId, e);
            return false;
        }
    }
    
    /**
     * 调度批次处理任务
     */
    @Transactional
    public void scheduleBatchProcessing(Long batchId, String status) {
        try {
            // 更新批次活动时间
            jdbcTemplate.update(
                "UPDATE answer_generation_batches SET last_activity_time = ? WHERE id = ?",
                LocalDateTime.now(), batchId
            );
            
            // 提交一个新任务到线程池
            taskExecutor.submit(() -> {
                try {
                    logger.info("开始执行批次{}处理任务", batchId);
                    answerGenerationTask.startBatchAnswerGeneration(batchId);
                    logger.info("批次{}处理任务完成", batchId);
                } catch (Exception e) {
                    logger.error("批次{}处理任务执行出错", batchId, e);
                    
                    // 处理失败时更新批次状态
                    try {
                        jdbcTemplate.update(
                            "UPDATE answer_generation_batches SET status = 'FAILED', error_message = ?, last_activity_time = ? WHERE id = ?",
                            e.getMessage(), LocalDateTime.now(), batchId
                        );
                    } catch (Exception ex) {
                        logger.error("更新批次{}失败状态时出错", batchId, ex);
                    }
                }
            });
            
            logger.info("批次{}处理任务已提交到线程池", batchId);
        } catch (Exception e) {
            logger.error("调度批次{}处理任务时出错", batchId, e);
        }
    }
    
    /**
     * 提交一个批次到调度器
     */
    public void submitBatch(Long batchId) {
        submitBatch(batchId, false);
    }

    /**
     * 提交一个批次到调度器
     * @param batchId 批次ID
     * @param force 是否强制提交（忽略活动检查）
     */
    public void submitBatch(Long batchId, boolean force) {
        if (!isRunning) {
            logger.warn("调度器未运行，无法提交批次{}", batchId);
            return;
        }
        
        try {
            // 获取批次状态
            String status = jdbcTemplate.queryForObject(
                "SELECT status FROM answer_generation_batches WHERE id = ?",
                String.class, batchId
            );
            
            if ("IN_PROGRESS".equals(status) || "RESUMING".equals(status)) {
                // 如果force为true，则强制调度，否则检查是否有活动任务
                boolean hasActiveTask = force ? false : checkBatchHasActiveTask(batchId);
                
                if (!hasActiveTask) {
                    logger.info("手动提交批次{}到调度器，当前状态: {}, 强制提交: {}", batchId, status, force);
                    scheduleBatchProcessing(batchId, status);
                } else {
                    logger.info("批次{}已有活动任务在执行，跳过调度", batchId);
                }
            } else {
                logger.warn("批次{}状态为{}，不是IN_PROGRESS或RESUMING，无法提交", batchId, status);
            }
        } catch (Exception e) {
            logger.error("提交批次{}到调度器时出错", batchId, e);
        }
    }
} 