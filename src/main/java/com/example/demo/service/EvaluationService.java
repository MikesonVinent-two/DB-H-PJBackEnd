package com.example.demo.service;

import com.example.demo.entity.jdbc.Evaluation;
import com.example.demo.entity.jdbc.EvaluationCriterion;
import com.example.demo.entity.jdbc.EvaluationDetail;
import com.example.demo.entity.jdbc.EvaluationRun;
import com.example.demo.entity.jdbc.LlmAnswer;
import com.example.demo.entity.jdbc.StandardQuestion;
import com.example.demo.entity.jdbc.QuestionType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 评测服务接口
 */
public interface EvaluationService {
    
    /**
     * 评测单个LLM回答
     * 
     * @param llmAnswer LLM回答
     * @param evaluatorId 评测者ID
     * @param userId 用户ID
     * @return 评测结果
     */
    Evaluation evaluateAnswer(LlmAnswer llmAnswer, Long evaluatorId, Long userId);
    
    /**
     * 批量评测LLM回答
     * 
     * @param llmAnswers LLM回答列表
     * @param evaluatorId 评测者ID
     * @param userId 用户ID
     * @return 评测结果列表
     */
    List<Evaluation> evaluateAnswers(List<LlmAnswer> llmAnswers, Long evaluatorId, Long userId);
    
    /**
     * 评测单选题回答
     * 
     * @param answerText 回答文本
     * @param correctIds 正确选项ID列表
     * @param options 选项列表
     * @return 评测结果，包含得分和评语
     */
    Map<String, Object> evaluateSingleChoice(String answerText, String correctIds, String options);
    
    /**
     * 评测多选题回答
     * 
     * @param answerText 回答文本
     * @param correctIds 正确选项ID列表
     * @param options 选项列表
     * @return 评测结果，包含得分和评语
     */
    Map<String, Object> evaluateMultipleChoice(String answerText, String correctIds, String options);
    
    /**
     * 评测简单事实题回答
     * 
     * @param answerText 回答文本
     * @param standardAnswer 标准答案文本
     * @param alternativeAnswers 备选答案文本列表
     * @return 评测结果，包含得分、评语和相似度指标
     */
    Map<String, Object> evaluateSimpleFact(String answerText, String standardAnswer, String alternativeAnswers);
    
    /**
     * 使用AI评测主观题回答
     * 
     * @param answerText 回答文本
     * @param questionText 问题文本
     * @param referenceAnswer 参考答案
     * @param criteria 评测标准列表
     * @param evaluatorId AI评测者ID
     * @return 评测结果，包含总分、各维度得分和评语
     */
    Map<String, Object> evaluateSubjectiveWithAI(String answerText, String questionText, 
                                               String referenceAnswer, List<EvaluationCriterion> criteria,
                                               Long evaluatorId);
    
    /**
     * 创建人工评测记录（用于主观题人工评分）
     * 
     * @param llmAnswerId LLM回答ID
     * @param evaluatorId 评测者ID（人类）
     * @param userId 用户ID
     * @return 创建的评测记录
     */
    Evaluation createHumanEvaluation(Long llmAnswerId, Long evaluatorId, Long userId);
    
    /**
     * 提交人工评测结果
     * 
     * @param evaluationId 评测ID
     * @param overallScore 总分
     * @param comments 评语
     * @param detailScores 各维度得分和评语
     * @param userId 用户ID
     * @return 更新后的评测记录
     */
    Evaluation submitHumanEvaluation(Long evaluationId, BigDecimal overallScore, String comments, 
                                    List<Map<String, Object>> detailScores, Long userId);
    
    /**
     * 创建评测运行记录
     * 
     * @param modelAnswerRunId 模型回答运行ID
     * @param evaluatorId 评测者ID
     * @param runName 运行名称
     * @param runDescription 运行描述
     * @param parameters 运行参数
     * @param userId 用户ID
     * @return 创建的评测运行记录
     */
    EvaluationRun createEvaluationRun(Long modelAnswerRunId, Long evaluatorId, String runName,
                                     String runDescription, Map<String, Object> parameters, Long userId);
    
    /**
     * 启动评测运行
     * 
     * @param evaluationRunId 评测运行ID
     * @return 异步任务
     */
    CompletableFuture<Void> startEvaluationRun(Long evaluationRunId);
    
    /**
     * 暂停评测运行
     * 
     * @param evaluationRunId 评测运行ID
     * @return 是否成功暂停
     */
    boolean pauseEvaluationRun(Long evaluationRunId);
    
    /**
     * 恢复评测运行
     * 
     * @param evaluationRunId 评测运行ID
     * @return 异步任务
     */
    CompletableFuture<Void> resumeEvaluationRun(Long evaluationRunId);
    
    /**
     * 获取评测运行进度
     * 
     * @param evaluationRunId 评测运行ID
     * @return 进度信息
     */
    Map<String, Object> getEvaluationRunProgress(Long evaluationRunId);
    
    /**
     * 获取评测运行列表
     * 
     * @param modelAnswerRunId 模型回答运行ID
     * @param evaluatorId 评测者ID
     * @param status 状态
     * @param page 页码
     * @param size 每页大小
     * @return 评测运行列表
     */
    List<EvaluationRun> getEvaluationRuns(Long modelAnswerRunId, Long evaluatorId, String status, int page, int size);
    
    /**
     * 获取评测运行详情
     * 
     * @param evaluationRunId 评测运行ID
     * @return 评测运行详情
     */
    EvaluationRun getEvaluationRun(Long evaluationRunId);
    
    /**
     * 获取评测运行结果
     * 
     * @param evaluationRunId 评测运行ID
     * @return 评测运行结果
     */
    Map<String, Object> getEvaluationRunResults(Long evaluationRunId);
    
    /**
     * 获取评测详情
     * 
     * @param evaluationId 评测ID
     * @return 评测详情列表
     */
    List<EvaluationDetail> getEvaluationDetails(Long evaluationId);
    
    /**
     * 获取特定问题类型的评测标准
     * 
     * @param questionType 问题类型
     * @return 评测标准列表
     */
    List<EvaluationCriterion> getCriteriaForQuestionType(QuestionType questionType);
    
    /**
     * 计算文本相似度
     * 
     * @param text1 第一个文本
     * @param text2 第二个文本
     * @return 相似度（0-1之间）
     */
    BigDecimal calculateTextSimilarity(String text1, String text2);
    
    /**
     * 使用BERT模型计算文本相似度
     * 
     * @param text1 第一个文本
     * @param text2 第二个文本
     * @return 相似度（0-1之间）
     */
    BigDecimal calculateBertSimilarity(String text1, String text2);
    
    /**
     * 计算ROUGE分数
     * 
     * @param candidateText 候选文本
     * @param referenceText 参考文本
     * @return ROUGE分数
     */
    BigDecimal calculateRougeScore(String candidateText, String referenceText);
    
    /**
     * 计算BLEU分数
     * 
     * @param candidateText 候选文本
     * @param referenceText 参考文本
     * @return BLEU分数
     */
    BigDecimal calculateBleuScore(String candidateText, String referenceText);
    
    /**
     * 评测一个批次中的所有客观题（单选题、多选题和简单事实题）
     * 
     * @param batchId 回答生成批次ID
     * @param evaluatorId 评测者ID
     * @param userId 用户ID
     * @return 评测结果统计信息
     */
    Map<String, Object> evaluateBatchObjectiveQuestions(Long batchId, Long evaluatorId, Long userId);

    /**
     * 评测批次的主观题
     * @param batchId 批次ID
     * @param evaluatorId 评测者ID（必须是AI模型类型）
     * @param userId 用户ID
     * @return 评测结果统计
     */
    Map<String, Object> evaluateBatchSubjectiveQuestions(Long batchId, Long evaluatorId, Long userId);
    
    /**
     * 重新评测单个主观题回答（强制覆盖已有评测）
     * 
     * @param llmAnswerId LLM回答ID
     * @param evaluatorId 评测者ID
     * @param userId 用户ID
     * @return 评测分数
     */
    BigDecimal reEvaluateSubjectiveAnswer(Long llmAnswerId, Long evaluatorId, Long userId);
    
    /**
     * 批量重新评测一个批次中的所有主观题（强制覆盖已有评测）
     * 
     * @param batchId 回答生成批次ID
     * @param evaluatorId 评测者ID
     * @param userId 用户ID
     * @return 评测结果统计
     */
    Map<String, Object> reEvaluateBatchSubjectiveQuestions(Long batchId, Long evaluatorId, Long userId);
    
    /**
     * 创建并提交人工评测（一步式操作）
     * 
     * @param llmAnswerId LLM回答ID
     * @param evaluatorId 评测者ID（人类）
     * @param overallScore 总分
     * @param comments 评语
     * @param detailScores 各维度得分和评语
     * @param userId 用户ID
     * @return 评测记录
     */
    Evaluation createAndSubmitHumanEvaluation(Long llmAnswerId, Long evaluatorId, 
                                           BigDecimal overallScore, String comments, 
                                           List<Map<String, Object>> detailScores, Long userId);
} 