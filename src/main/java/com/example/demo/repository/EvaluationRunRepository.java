package com.example.demo.repository;

import com.example.demo.entity.EvaluationRun;
import com.example.demo.entity.EvaluationRun.RunStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评测运行数据访问接口
 */
@Repository
public interface EvaluationRunRepository extends JpaRepository<EvaluationRun, Long> {
    
    /**
     * 根据模型回答运行ID查询评测运行
     * 
     * @param modelAnswerRunId 模型回答运行ID
     * @param pageable 分页参数
     * @return 评测运行列表
     */
    List<EvaluationRun> findByModelAnswerRunId(Long modelAnswerRunId, Pageable pageable);
    
    /**
     * 根据评测者ID查询评测运行
     * 
     * @param evaluatorId 评测者ID
     * @param pageable 分页参数
     * @return 评测运行列表
     */
    List<EvaluationRun> findByEvaluatorId(Long evaluatorId, Pageable pageable);
    
    /**
     * 根据状态查询评测运行
     * 
     * @param status 状态
     * @param pageable 分页参数
     * @return 评测运行列表
     */
    List<EvaluationRun> findByStatus(RunStatus status, Pageable pageable);
    
    /**
     * 根据模型回答运行ID和评测者ID查询评测运行
     * 
     * @param modelAnswerRunId 模型回答运行ID
     * @param evaluatorId 评测者ID
     * @param pageable 分页参数
     * @return 评测运行列表
     */
    List<EvaluationRun> findByModelAnswerRunIdAndEvaluatorId(Long modelAnswerRunId, Long evaluatorId, Pageable pageable);
    
    /**
     * 根据模型回答运行ID和状态查询评测运行
     * 
     * @param modelAnswerRunId 模型回答运行ID
     * @param status 状态
     * @param pageable 分页参数
     * @return 评测运行列表
     */
    List<EvaluationRun> findByModelAnswerRunIdAndStatus(Long modelAnswerRunId, RunStatus status, Pageable pageable);
    
    /**
     * 根据评测者ID和状态查询评测运行
     * 
     * @param evaluatorId 评测者ID
     * @param status 状态
     * @param pageable 分页参数
     * @return 评测运行列表
     */
    List<EvaluationRun> findByEvaluatorIdAndStatus(Long evaluatorId, RunStatus status, Pageable pageable);
    
    /**
     * 根据模型回答运行ID、评测者ID和状态查询评测运行
     * 
     * @param modelAnswerRunId 模型回答运行ID
     * @param evaluatorId 评测者ID
     * @param status 状态
     * @param pageable 分页参数
     * @return 评测运行列表
     */
    List<EvaluationRun> findByModelAnswerRunIdAndEvaluatorIdAndStatus(Long modelAnswerRunId, Long evaluatorId, RunStatus status, Pageable pageable);
    
    // 新增的查询方法
    List<EvaluationRun> findByModelAnswerRunIdAndEvaluatorIdAndStatusNot(
            Long modelAnswerRunId, Long evaluatorId, RunStatus status);
    
    List<EvaluationRun> findByStatusAndLastActivityTimeBefore(
            RunStatus status, LocalDateTime time);
    
    @Query("SELECT e FROM EvaluationRun e WHERE e.status = :status " +
           "AND e.lastActivityTime < :time " +
           "AND e.isAutoResume = true")
    List<EvaluationRun> findStaleRunsForAutoResume(
            @Param("status") RunStatus status, 
            @Param("time") LocalDateTime time);
    
    @Query("SELECT e FROM EvaluationRun e WHERE e.status IN :statuses " +
           "AND e.lastActivityTime < :time")
    List<EvaluationRun> findStaleRuns(
            @Param("statuses") List<RunStatus> statuses, 
            @Param("time") LocalDateTime time);
    
    @Modifying
    @Query("UPDATE EvaluationRun e SET e.status = :newStatus, " +
           "e.lastActivityTime = CURRENT_TIMESTAMP, " +
           "e.errorMessage = :errorMessage " +
           "WHERE e.id = :runId")
    void updateRunStatus(
            @Param("runId") Long runId, 
            @Param("newStatus") RunStatus newStatus, 
            @Param("errorMessage") String errorMessage);
} 