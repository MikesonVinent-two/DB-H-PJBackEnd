package com.example.demo.repository.jdbc;

import com.example.demo.entity.jdbc.DatasetQuestionMapping;
import com.example.demo.entity.jdbc.LlmAnswer;
import com.example.demo.entity.jdbc.ModelAnswerRun;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 基于JDBC的LLM回答仓库实现
 */
@Repository
public class JdbcLlmAnswerRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String SQL_INSERT = 
            "INSERT INTO llm_answers (model_answer_run_id, dataset_question_mapping_id, answer_text, " +
            "generation_status, error_message, generation_time, prompt_used, raw_model_response, other_metadata, repeat_index) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::json, ?)";
    
    private static final String SQL_UPDATE = 
            "UPDATE llm_answers SET model_answer_run_id=?, dataset_question_mapping_id=?, answer_text=?, " +
            "generation_status=?, error_message=?, generation_time=?, prompt_used=?, raw_model_response=?, other_metadata=?::json, repeat_index=? " +
            "WHERE id=?";
    
    private static final String SQL_FIND_BY_ID = 
            "SELECT * FROM llm_answers WHERE id=?";
    
    private static final String SQL_FIND_BY_MODEL_ANSWER_RUN_ID = 
            "SELECT * FROM llm_answers WHERE model_answer_run_id=?";
    
    private static final String SQL_FIND_BY_ID_WITH_QUESTION = 
            "SELECT a.*, dqm.id as dqm_id, dqm.standard_question_id as sq_id " +
            "FROM llm_answers a " +
            "JOIN dataset_question_mapping dqm ON a.dataset_question_mapping_id = dqm.id " +
            "WHERE a.id=?";
    
    private static final String SQL_FIND_BY_MODEL_ANSWER_RUN_ID_WITH_QUESTIONS = 
            "SELECT a.*, dqm.id as dqm_id, dqm.standard_question_id as sq_id " +
            "FROM llm_answers a " +
            "JOIN dataset_question_mapping dqm ON a.dataset_question_mapping_id = dqm.id " +
            "WHERE a.model_answer_run_id=?";
    
    private static final String SQL_FIND_BY_DATASET_QUESTION_MAPPING_ID = 
            "SELECT * FROM llm_answers WHERE dataset_question_mapping_id=?";
    
    private static final String SQL_FIND_BY_MODEL_ANSWER_RUN_ID_AND_DATASET_QUESTION_MAPPING_ID = 
            "SELECT * FROM llm_answers WHERE model_answer_run_id=? AND dataset_question_mapping_id=?";
    
    private static final String SQL_FIND_BY_MODEL_ANSWER_RUN_ID_AND_DATASET_QUESTION_MAPPING_ID_AND_REPEAT_INDEX = 
            "SELECT * FROM llm_answers WHERE model_answer_run_id=? AND dataset_question_mapping_id=? AND repeat_index=?";
    
    private static final String SQL_COUNT_BY_RUN_ID = 
            "SELECT COUNT(*) FROM llm_answers WHERE model_answer_run_id=?";
    
    private static final String SQL_FIND_BY_BATCH_ID = 
            "SELECT a.* FROM llm_answers a " +
            "JOIN model_answer_runs mar ON a.model_answer_run_id = mar.id " +
            "WHERE mar.answer_generation_batch_id=?";
    
    private static final String SQL_FIND_BY_MODEL_ANSWER_RUN_ID_AND_ID_GREATER_THAN = 
            "SELECT * FROM llm_answers WHERE model_answer_run_id=? AND id>? ORDER BY id";
    
    private static final String SQL_FIND_ALL = 
            "SELECT * FROM llm_answers";

    @Autowired
    public JdbcLlmAnswerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 保存LLM回答
     *
     * @param llmAnswer LLM回答对象
     * @return 带有ID的LLM回答对象
     */
    public LlmAnswer save(LlmAnswer llmAnswer) {
        if (llmAnswer.getId() == null) {
            return insert(llmAnswer);
        } else {
            return update(llmAnswer);
        }
    }

    /**
     * 插入新LLM回答
     *
     * @param llmAnswer LLM回答对象
     * @return 带有ID的LLM回答对象
     */
    private LlmAnswer insert(LlmAnswer llmAnswer) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            
            // 设置模型回答运行ID
            ps.setLong(1, llmAnswer.getModelAnswerRun().getId());
            
            // 设置数据集问题映射ID
            ps.setLong(2, llmAnswer.getDatasetQuestionMapping().getId());
            
            // 设置回答文本
            if (llmAnswer.getAnswerText() != null) {
                ps.setString(3, llmAnswer.getAnswerText());
            } else {
                ps.setNull(3, java.sql.Types.VARCHAR);
            }
            
            // 设置生成状态
            ps.setString(4, llmAnswer.getGenerationStatus().name());
            
            // 设置错误信息
            if (llmAnswer.getErrorMessage() != null) {
                ps.setString(5, llmAnswer.getErrorMessage());
            } else {
                ps.setNull(5, java.sql.Types.VARCHAR);
            }
            
            // 设置生成时间
            if (llmAnswer.getGenerationTime() != null) {
                ps.setTimestamp(6, Timestamp.valueOf(llmAnswer.getGenerationTime()));
            } else {
                ps.setNull(6, java.sql.Types.TIMESTAMP);
            }
            
            // 设置使用的提示词
            if (llmAnswer.getPromptUsed() != null) {
                ps.setString(7, llmAnswer.getPromptUsed());
            } else {
                ps.setNull(7, java.sql.Types.VARCHAR);
            }
            
            // 设置原始模型响应
            if (llmAnswer.getRawModelResponse() != null) {
                ps.setString(8, llmAnswer.getRawModelResponse());
            } else {
                ps.setNull(8, java.sql.Types.VARCHAR);
            }
            
            // 设置其他元数据
            if (llmAnswer.getOtherMetadata() != null) {
                ps.setString(9, llmAnswer.getOtherMetadata());
            } else {
                ps.setString(9, "{}");
            }
            
            // 设置重复索引
            ps.setInt(10, llmAnswer.getRepeatIndex() != null ? llmAnswer.getRepeatIndex() : 0);
            
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            llmAnswer.setId(key.longValue());
        }
        return llmAnswer;
    }

    /**
     * 更新LLM回答
     *
     * @param llmAnswer LLM回答对象
     * @return 更新后的LLM回答对象
     */
    private LlmAnswer update(LlmAnswer llmAnswer) {
        jdbcTemplate.update(SQL_UPDATE,
                llmAnswer.getModelAnswerRun().getId(),
                llmAnswer.getDatasetQuestionMapping().getId(),
                llmAnswer.getAnswerText(),
                llmAnswer.getGenerationStatus().name(),
                llmAnswer.getErrorMessage(),
                llmAnswer.getGenerationTime() != null ? Timestamp.valueOf(llmAnswer.getGenerationTime()) : null,
                llmAnswer.getPromptUsed(),
                llmAnswer.getRawModelResponse(),
                llmAnswer.getOtherMetadata() != null ? llmAnswer.getOtherMetadata() : "{}",
                llmAnswer.getRepeatIndex(),
                llmAnswer.getId());

        return llmAnswer;
    }

    /**
     * 根据ID查找LLM回答
     *
     * @param id LLM回答ID
     * @return LLM回答的Optional包装
     */
    public Optional<LlmAnswer> findById(Long id) {
        try {
            LlmAnswer llmAnswer = jdbcTemplate.queryForObject(SQL_FIND_BY_ID, new Object[]{id}, new LlmAnswerRowMapper());
            return Optional.ofNullable(llmAnswer);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
    
    /**
     * 根据ID查找LLM回答，同时预加载问题
     *
     * @param id 回答ID
     * @return 回答的Optional包装，包含预加载的问题
     */
    public Optional<LlmAnswer> findByIdWithQuestion(Long id) {
        try {
            LlmAnswer llmAnswer = jdbcTemplate.queryForObject(SQL_FIND_BY_ID_WITH_QUESTION, new Object[]{id}, new LlmAnswerWithQuestionRowMapper());
            return Optional.ofNullable(llmAnswer);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
    
    /**
     * 根据运行ID查找回答
     *
     * @param modelAnswerRunId 运行ID
     * @return 回答列表
     */
    public List<LlmAnswer> findByModelAnswerRunId(Long modelAnswerRunId) {
        return jdbcTemplate.query(
                SQL_FIND_BY_MODEL_ANSWER_RUN_ID,
                new Object[]{modelAnswerRunId},
                new LlmAnswerRowMapper()
        );
    }
    
    /**
     * 根据运行ID查找回答，同时预加载问题
     *
     * @param modelAnswerRunId 运行ID
     * @return 回答列表，包含预加载的问题
     */
    public List<LlmAnswer> findByModelAnswerRunIdWithQuestions(Long modelAnswerRunId) {
        return jdbcTemplate.query(
                SQL_FIND_BY_MODEL_ANSWER_RUN_ID_WITH_QUESTIONS,
                new Object[]{modelAnswerRunId},
                new LlmAnswerWithQuestionRowMapper()
        );
    }
    
    /**
     * 根据数据集映射问题ID查找回答
     *
     * @param datasetQuestionMappingId 数据集映射问题ID
     * @return 回答列表
     */
    public List<LlmAnswer> findByDatasetQuestionMappingId(Long datasetQuestionMappingId) {
        return jdbcTemplate.query(
                SQL_FIND_BY_DATASET_QUESTION_MAPPING_ID,
                new Object[]{datasetQuestionMappingId},
                new LlmAnswerRowMapper()
        );
    }
    
    /**
     * 根据运行ID和数据集映射问题ID查找回答
     *
     * @param runId 运行ID
     * @param datasetQuestionMappingId 数据集映射问题ID
     * @return 回答列表
     */
    public List<LlmAnswer> findByModelAnswerRunIdAndDatasetQuestionMappingId(Long runId, Long datasetQuestionMappingId) {
        return jdbcTemplate.query(
                SQL_FIND_BY_MODEL_ANSWER_RUN_ID_AND_DATASET_QUESTION_MAPPING_ID,
                new Object[]{runId, datasetQuestionMappingId},
                new LlmAnswerRowMapper()
        );
    }
    
    /**
     * 根据运行ID和数据集映射问题ID及重复索引查找回答
     *
     * @param runId 运行ID
     * @param datasetQuestionMappingId 数据集映射问题ID
     * @param repeatIndex 重复索引
     * @return 回答
     */
    public LlmAnswer findByModelAnswerRunIdAndDatasetQuestionMappingIdAndRepeatIndex(
            Long runId, Long datasetQuestionMappingId, Integer repeatIndex) {
        try {
            return jdbcTemplate.queryForObject(
                    SQL_FIND_BY_MODEL_ANSWER_RUN_ID_AND_DATASET_QUESTION_MAPPING_ID_AND_REPEAT_INDEX,
                    new Object[]{runId, datasetQuestionMappingId, repeatIndex},
                    new LlmAnswerRowMapper()
            );
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
    
    /**
     * 统计运行的已完成回答数量
     *
     * @param runId 运行ID
     * @return 回答数量
     */
    public int countByRunId(Long runId) {
        Integer count = jdbcTemplate.queryForObject(
                SQL_COUNT_BY_RUN_ID,
                Integer.class,
                runId
        );
        return count != null ? count : 0;
    }
    
    /**
     * 按批次ID查找所有回答
     *
     * @param batchId 批次ID
     * @return 回答列表
     */
    public List<LlmAnswer> findByBatchId(Long batchId) {
        return jdbcTemplate.query(
                SQL_FIND_BY_BATCH_ID,
                new Object[]{batchId},
                new LlmAnswerRowMapper()
        );
    }
    
    /**
     * 根据模型回答运行ID和回答ID查询大于指定ID的回答列表
     *
     * @param modelAnswerRunId 模型回答运行ID
     * @param id 回答ID
     * @return 回答列表
     */
    public List<LlmAnswer> findByModelAnswerRunIdAndIdGreaterThan(Long modelAnswerRunId, Long id) {
        return jdbcTemplate.query(
                SQL_FIND_BY_MODEL_ANSWER_RUN_ID_AND_ID_GREATER_THAN,
                new Object[]{modelAnswerRunId, id},
                new LlmAnswerRowMapper()
        );
    }
    
    /**
     * 查找所有LLM回答
     *
     * @return LLM回答列表
     */
    public List<LlmAnswer> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, new LlmAnswerRowMapper());
    }

    /**
     * LLM回答行映射器
     */
    private class LlmAnswerRowMapper implements RowMapper<LlmAnswer> {
        @Override
        public LlmAnswer mapRow(ResultSet rs, int rowNum) throws SQLException {
            LlmAnswer llmAnswer = new LlmAnswer();
            llmAnswer.setId(rs.getLong("id"));
            
            // 设置模型回答运行
            Long modelAnswerRunId = rs.getLong("model_answer_run_id");
            if (!rs.wasNull()) {
                ModelAnswerRun modelAnswerRun = new ModelAnswerRun();
                modelAnswerRun.setId(modelAnswerRunId);
                llmAnswer.setModelAnswerRun(modelAnswerRun);
            }
            
            // 设置数据集问题映射
            Long datasetQuestionMappingId = rs.getLong("dataset_question_mapping_id");
            if (!rs.wasNull()) {
                DatasetQuestionMapping datasetQuestionMapping = new DatasetQuestionMapping();
                datasetQuestionMapping.setId(datasetQuestionMappingId);
                llmAnswer.setDatasetQuestionMapping(datasetQuestionMapping);
            }
            
            // 设置回答文本
            llmAnswer.setAnswerText(rs.getString("answer_text"));
            
            // 设置生成状态
            String generationStatusStr = rs.getString("generation_status");
            if (generationStatusStr != null) {
                llmAnswer.setGenerationStatus(LlmAnswer.GenerationStatus.valueOf(generationStatusStr));
            }
            
            // 设置错误信息
            llmAnswer.setErrorMessage(rs.getString("error_message"));
            
            // 设置生成时间
            Timestamp generationTime = rs.getTimestamp("generation_time");
            if (generationTime != null) {
                llmAnswer.setGenerationTime(generationTime.toLocalDateTime());
            }
            
            // 设置使用的提示词
            llmAnswer.setPromptUsed(rs.getString("prompt_used"));
            
            // 设置原始模型响应
            llmAnswer.setRawModelResponse(rs.getString("raw_model_response"));
            
            // 设置其他元数据
            llmAnswer.setOtherMetadata(rs.getString("other_metadata"));
            
            // 设置重复索引
            llmAnswer.setRepeatIndex(rs.getInt("repeat_index"));
            
            return llmAnswer;
        }
    }
    
    /**
     * 带问题的LLM回答行映射器
     */
    private class LlmAnswerWithQuestionRowMapper extends LlmAnswerRowMapper {
        @Override
        public LlmAnswer mapRow(ResultSet rs, int rowNum) throws SQLException {
            // 首先获取基本的LLM回答对象
            LlmAnswer llmAnswer = super.mapRow(rs, rowNum);
            
            // 进一步填充问题相关信息
            try {
                Long dqmId = rs.getLong("dqm_id");
                Long sqId = rs.getLong("sq_id");
                
                if (!rs.wasNull()) {
                    // 这里仅设置ID，实际使用时可能需要加载完整的StandardQuestion对象
                    DatasetQuestionMapping dqm = new DatasetQuestionMapping();
                    dqm.setId(dqmId);
                    llmAnswer.setDatasetQuestionMapping(dqm);
                }
            } catch (SQLException e) {
                // 如果查询中没有这些列，则忽略异常
            }
            
            return llmAnswer;
        }
    }
} 