package com.example.demo.repository.jdbc;

import com.example.demo.entity.jdbc.AnswerGenerationBatch;
import com.example.demo.entity.jdbc.AnswerGenerationBatch.BatchStatus;
import com.example.demo.entity.jdbc.DatasetVersion;
import com.example.demo.entity.jdbc.User;
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
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 基于JDBC的答案生成批次仓库实现
 */
@Repository
public class AnswerGenerationBatchRepository {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;

    private static final String SQL_INSERT = 
            "INSERT INTO answer_generation_batches " +
            "(name, description, dataset_version_id, creation_time, status, " +
            "answer_assembly_config_id, evaluation_assembly_config_id, single_choice_prompt_id, " +
            "multiple_choice_prompt_id, simple_fact_prompt_id, subjective_prompt_id, " +
            "global_parameters, created_by_user_id, completed_at, progress_percentage, " +
            "last_activity_time, last_check_time, resume_count, pause_time, pause_reason, " +
            "answer_repeat_count, error_message, processing_instance, last_processed_run_id) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::json, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    private static final String SQL_UPDATE = 
            "UPDATE answer_generation_batches SET " +
            "name=?, description=?, dataset_version_id=?, creation_time=?, status=?, " +
            "answer_assembly_config_id=?, evaluation_assembly_config_id=?, single_choice_prompt_id=?, " +
            "multiple_choice_prompt_id=?, simple_fact_prompt_id=?, subjective_prompt_id=?, " +
            "global_parameters=?::json, created_by_user_id=?, completed_at=?, progress_percentage=?, " +
            "last_activity_time=?, last_check_time=?, resume_count=?, pause_time=?, pause_reason=?, " +
            "answer_repeat_count=?, error_message=?, processing_instance=?, last_processed_run_id=? " +
            "WHERE id=?";
    
    private static final String SQL_FIND_BY_ID = 
            "SELECT * FROM answer_generation_batches WHERE id=?";
    
    private static final String SQL_FIND_BY_STATUS = 
            "SELECT * FROM answer_generation_batches WHERE status=?";
    
    private static final String SQL_FIND_BY_CREATED_BY_USER_ID = 
            "SELECT * FROM answer_generation_batches WHERE created_by_user_id=?";
    
    private static final String SQL_FIND_BY_DATASET_VERSION_ID = 
            "SELECT * FROM answer_generation_batches WHERE dataset_version_id=?";
    
    private static final String SQL_COUNT_BY_STATUS = 
            "SELECT COUNT(*) FROM answer_generation_batches WHERE status=?";
    
    private static final String SQL_FIND_ALL = 
            "SELECT * FROM answer_generation_batches";
    
    private static final String SQL_DELETE = 
            "DELETE FROM answer_generation_batches WHERE id=?";

    @Autowired
    public AnswerGenerationBatchRepository(JdbcTemplate jdbcTemplate, UserRepository userRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
    }

    /**
     * 保存答案生成批次
     *
     * @param batch 答案生成批次对象
     * @return 带有ID的答案生成批次对象
     */
    public AnswerGenerationBatch save(AnswerGenerationBatch batch) {
        if (batch.getId() == null) {
            return insert(batch);
        } else {
            return update(batch);
        }
    }

    /**
     * 插入新答案生成批次
     *
     * @param batch 答案生成批次对象
     * @return 带有ID的答案生成批次对象
     */
    private AnswerGenerationBatch insert(AnswerGenerationBatch batch) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            
            // 设置名称
            ps.setString(1, batch.getName());
            
            // 设置描述
            if (batch.getDescription() != null) {
                ps.setString(2, batch.getDescription());
            } else {
                ps.setNull(2, Types.VARCHAR);
            }
            
            // 设置数据集版本ID
            if (batch.getDatasetVersion() != null && batch.getDatasetVersion().getId() != null) {
                ps.setLong(3, batch.getDatasetVersion().getId());
            } else {
                ps.setNull(3, Types.BIGINT);
            }
            
            // 设置创建时间
            if (batch.getCreationTime() != null) {
                ps.setTimestamp(4, Timestamp.valueOf(batch.getCreationTime()));
            } else {
                ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            }
            
            // 设置状态
            ps.setString(5, batch.getStatus().name());
            
            // 设置回答组装配置ID
            if (batch.getAnswerAssemblyConfig() != null && batch.getAnswerAssemblyConfig().getId() != null) {
                ps.setLong(6, batch.getAnswerAssemblyConfig().getId());
            } else {
                ps.setNull(6, Types.BIGINT);
            }
            
            // 设置评测组装配置ID
            if (batch.getEvaluationAssemblyConfig() != null && batch.getEvaluationAssemblyConfig().getId() != null) {
                ps.setLong(7, batch.getEvaluationAssemblyConfig().getId());
            } else {
                ps.setNull(7, Types.BIGINT);
            }
            
            // 设置单选题prompt ID
            if (batch.getSingleChoicePrompt() != null && batch.getSingleChoicePrompt().getId() != null) {
                ps.setLong(8, batch.getSingleChoicePrompt().getId());
            } else {
                ps.setNull(8, Types.BIGINT);
            }
            
            // 设置多选题prompt ID
            if (batch.getMultipleChoicePrompt() != null && batch.getMultipleChoicePrompt().getId() != null) {
                ps.setLong(9, batch.getMultipleChoicePrompt().getId());
            } else {
                ps.setNull(9, Types.BIGINT);
            }
            
            // 设置简单事实题prompt ID
            if (batch.getSimpleFactPrompt() != null && batch.getSimpleFactPrompt().getId() != null) {
                ps.setLong(10, batch.getSimpleFactPrompt().getId());
            } else {
                ps.setNull(10, Types.BIGINT);
            }
            
            // 设置主观题prompt ID
            if (batch.getSubjectivePrompt() != null && batch.getSubjectivePrompt().getId() != null) {
                ps.setLong(11, batch.getSubjectivePrompt().getId());
            } else {
                ps.setNull(11, Types.BIGINT);
            }
            
            // 设置全局参数
            if (batch.getGlobalParameters() != null) {
                ps.setString(12, batch.getGlobalParameters().toString());
            } else {
                ps.setString(12, "{}");
            }
            
            // 设置创建者ID
            if (batch.getCreatedByUser() != null && batch.getCreatedByUser().getId() != null) {
                ps.setLong(13, batch.getCreatedByUser().getId());
            } else {
                ps.setNull(13, Types.BIGINT);
            }
            
            // 设置完成时间
            if (batch.getCompletedAt() != null) {
                ps.setTimestamp(14, Timestamp.valueOf(batch.getCompletedAt()));
            } else {
                ps.setNull(14, Types.TIMESTAMP);
            }
            
            // 设置进度百分比
            if (batch.getProgressPercentage() != null) {
                ps.setBigDecimal(15, batch.getProgressPercentage());
            } else {
                ps.setNull(15, Types.DECIMAL);
            }
            
            // 设置最后活动时间
            if (batch.getLastActivityTime() != null) {
                ps.setTimestamp(16, Timestamp.valueOf(batch.getLastActivityTime()));
            } else {
                ps.setNull(16, Types.TIMESTAMP);
            }
            
            // 设置最后检查时间
            if (batch.getLastCheckTime() != null) {
                ps.setTimestamp(17, Timestamp.valueOf(batch.getLastCheckTime()));
            } else {
                ps.setNull(17, Types.TIMESTAMP);
            }
            
            // 设置恢复次数
            if (batch.getResumeCount() != null) {
                ps.setInt(18, batch.getResumeCount());
            } else {
                ps.setInt(18, 0);
            }
            
            // 设置暂停时间
            if (batch.getPauseTime() != null) {
                ps.setTimestamp(19, Timestamp.valueOf(batch.getPauseTime()));
            } else {
                ps.setNull(19, Types.TIMESTAMP);
            }
            
            // 设置暂停原因
            if (batch.getPauseReason() != null) {
                ps.setString(20, batch.getPauseReason());
            } else {
                ps.setNull(20, Types.VARCHAR);
            }
            
            // 设置回答重复次数
            if (batch.getAnswerRepeatCount() != null) {
                ps.setInt(21, batch.getAnswerRepeatCount());
            } else {
                ps.setInt(21, 1); // 默认值
            }
            
            // 设置错误信息
            if (batch.getErrorMessage() != null) {
                ps.setString(22, batch.getErrorMessage());
            } else {
                ps.setNull(22, Types.VARCHAR);
            }
            
            // 设置处理实例标识
            if (batch.getProcessingInstance() != null) {
                ps.setString(23, batch.getProcessingInstance());
            } else {
                ps.setNull(23, Types.VARCHAR);
            }
            
            // 设置上次处理的运行ID
            if (batch.getLastProcessedRunId() != null) {
                ps.setLong(24, batch.getLastProcessedRunId());
            } else {
                ps.setNull(24, Types.BIGINT);
            }
            
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            batch.setId(key.longValue());
        }

        return batch;
    }

    /**
     * 更新答案生成批次
     *
     * @param batch 答案生成批次对象
     * @return 更新后的答案生成批次对象
     */
    private AnswerGenerationBatch update(AnswerGenerationBatch batch) {
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_UPDATE);
            
            // 设置名称
            ps.setString(1, batch.getName());
            
            // 设置描述
            if (batch.getDescription() != null) {
                ps.setString(2, batch.getDescription());
            } else {
                ps.setNull(2, Types.VARCHAR);
            }
            
            // 设置数据集版本ID
            if (batch.getDatasetVersion() != null && batch.getDatasetVersion().getId() != null) {
                ps.setLong(3, batch.getDatasetVersion().getId());
            } else {
                ps.setNull(3, Types.BIGINT);
            }
            
            // 设置创建时间
            if (batch.getCreationTime() != null) {
                ps.setTimestamp(4, Timestamp.valueOf(batch.getCreationTime()));
            } else {
                ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            }
            
            // 设置状态
            ps.setString(5, batch.getStatus().name());
            
            // 设置回答组装配置ID
            if (batch.getAnswerAssemblyConfig() != null && batch.getAnswerAssemblyConfig().getId() != null) {
                ps.setLong(6, batch.getAnswerAssemblyConfig().getId());
            } else {
                ps.setNull(6, Types.BIGINT);
            }
            
            // 设置评测组装配置ID
            if (batch.getEvaluationAssemblyConfig() != null && batch.getEvaluationAssemblyConfig().getId() != null) {
                ps.setLong(7, batch.getEvaluationAssemblyConfig().getId());
            } else {
                ps.setNull(7, Types.BIGINT);
            }
            
            // 设置单选题prompt ID
            if (batch.getSingleChoicePrompt() != null && batch.getSingleChoicePrompt().getId() != null) {
                ps.setLong(8, batch.getSingleChoicePrompt().getId());
            } else {
                ps.setNull(8, Types.BIGINT);
            }
            
            // 设置多选题prompt ID
            if (batch.getMultipleChoicePrompt() != null && batch.getMultipleChoicePrompt().getId() != null) {
                ps.setLong(9, batch.getMultipleChoicePrompt().getId());
            } else {
                ps.setNull(9, Types.BIGINT);
            }
            
            // 设置简单事实题prompt ID
            if (batch.getSimpleFactPrompt() != null && batch.getSimpleFactPrompt().getId() != null) {
                ps.setLong(10, batch.getSimpleFactPrompt().getId());
            } else {
                ps.setNull(10, Types.BIGINT);
            }
            
            // 设置主观题prompt ID
            if (batch.getSubjectivePrompt() != null && batch.getSubjectivePrompt().getId() != null) {
                ps.setLong(11, batch.getSubjectivePrompt().getId());
            } else {
                ps.setNull(11, Types.BIGINT);
            }
            
            // 设置全局参数
            if (batch.getGlobalParameters() != null) {
                ps.setString(12, batch.getGlobalParameters().toString());
            } else {
                ps.setString(12, "{}");
            }
            
            // 设置创建者ID
            if (batch.getCreatedByUser() != null && batch.getCreatedByUser().getId() != null) {
                ps.setLong(13, batch.getCreatedByUser().getId());
            } else {
                ps.setNull(13, Types.BIGINT);
            }
            
            // 设置完成时间
            if (batch.getCompletedAt() != null) {
                ps.setTimestamp(14, Timestamp.valueOf(batch.getCompletedAt()));
            } else {
                ps.setNull(14, Types.TIMESTAMP);
            }
            
            // 设置进度百分比
            if (batch.getProgressPercentage() != null) {
                ps.setBigDecimal(15, batch.getProgressPercentage());
            } else {
                ps.setNull(15, Types.DECIMAL);
            }
            
            // 设置最后活动时间
            if (batch.getLastActivityTime() != null) {
                ps.setTimestamp(16, Timestamp.valueOf(batch.getLastActivityTime()));
            } else {
                ps.setNull(16, Types.TIMESTAMP);
            }
            
            // 设置最后检查时间
            if (batch.getLastCheckTime() != null) {
                ps.setTimestamp(17, Timestamp.valueOf(batch.getLastCheckTime()));
            } else {
                ps.setNull(17, Types.TIMESTAMP);
            }
            
            // 设置恢复次数
            if (batch.getResumeCount() != null) {
                ps.setInt(18, batch.getResumeCount());
            } else {
                ps.setInt(18, 0);
            }
            
            // 设置暂停时间
            if (batch.getPauseTime() != null) {
                ps.setTimestamp(19, Timestamp.valueOf(batch.getPauseTime()));
            } else {
                ps.setNull(19, Types.TIMESTAMP);
            }
            
            // 设置暂停原因
            if (batch.getPauseReason() != null) {
                ps.setString(20, batch.getPauseReason());
            } else {
                ps.setNull(20, Types.VARCHAR);
            }
            
            // 设置回答重复次数
            if (batch.getAnswerRepeatCount() != null) {
                ps.setInt(21, batch.getAnswerRepeatCount());
            } else {
                ps.setInt(21, 1); // 默认值
            }
            
            // 设置错误信息
            if (batch.getErrorMessage() != null) {
                ps.setString(22, batch.getErrorMessage());
            } else {
                ps.setNull(22, Types.VARCHAR);
            }
            
            // 设置处理实例标识
            if (batch.getProcessingInstance() != null) {
                ps.setString(23, batch.getProcessingInstance());
            } else {
                ps.setNull(23, Types.VARCHAR);
            }
            
            // 设置上次处理的运行ID
            if (batch.getLastProcessedRunId() != null) {
                ps.setLong(24, batch.getLastProcessedRunId());
            } else {
                ps.setNull(24, Types.BIGINT);
            }
            
            // 设置ID
            ps.setLong(25, batch.getId());
            
            return ps;
        });

        return batch;
    }

    /**
     * 根据ID查找答案生成批次
     *
     * @param id 答案生成批次ID
     * @return 答案生成批次对象
     */
    public Optional<AnswerGenerationBatch> findById(Long id) {
        try {
            AnswerGenerationBatch batch = jdbcTemplate.queryForObject(
                SQL_FIND_BY_ID,
                new AnswerGenerationBatchRowMapper(),
                id
            );
            return Optional.ofNullable(batch);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 根据状态查找答案生成批次
     *
     * @param status 状态
     * @return 答案生成批次列表
     */
    public List<AnswerGenerationBatch> findByStatus(BatchStatus status) {
        return jdbcTemplate.query(SQL_FIND_BY_STATUS, new AnswerGenerationBatchRowMapper(), status.name());
    }

    /**
     * 根据创建者ID查找答案生成批次
     *
     * @param userId 用户ID
     * @return 答案生成批次列表
     */
    public List<AnswerGenerationBatch> findByCreatedByUserId(Long userId) {
        return jdbcTemplate.query(SQL_FIND_BY_CREATED_BY_USER_ID, new AnswerGenerationBatchRowMapper(), userId);
    }

    /**
     * 根据数据集版本ID查找答案生成批次
     *
     * @param datasetVersionId 数据集版本ID
     * @return 答案生成批次列表
     */
    public List<AnswerGenerationBatch> findByDatasetVersionId(Long datasetVersionId) {
        return jdbcTemplate.query(SQL_FIND_BY_DATASET_VERSION_ID, new AnswerGenerationBatchRowMapper(), datasetVersionId);
    }

    /**
     * 按状态统计答案生成批次数
     *
     * @param status 状态
     * @return 数量
     */
    public long countByStatus(BatchStatus status) {
        return jdbcTemplate.queryForObject(SQL_COUNT_BY_STATUS, Long.class, status.name());
    }

    /**
     * 查找所有答案生成批次
     *
     * @return 所有答案生成批次列表
     */
    public List<AnswerGenerationBatch> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, new AnswerGenerationBatchRowMapper());
    }

    /**
     * 删除答案生成批次
     *
     * @param batch 答案生成批次对象
     */
    public void delete(AnswerGenerationBatch batch) {
        jdbcTemplate.update(SQL_DELETE, batch.getId());
    }

    /**
     * 答案生成批次行映射器
     */
    private class AnswerGenerationBatchRowMapper implements RowMapper<AnswerGenerationBatch> {
        @Override
        public AnswerGenerationBatch mapRow(ResultSet rs, int rowNum) throws SQLException {
            AnswerGenerationBatch batch = new AnswerGenerationBatch();
            
            // 设置ID和基本属性
            batch.setId(rs.getLong("id"));
            batch.setName(rs.getString("name"));
            batch.setDescription(rs.getString("description"));
            batch.setStatus(BatchStatus.valueOf(rs.getString("status")));
            
            // 设置时间字段
            Timestamp creationTime = rs.getTimestamp("creation_time");
            if (creationTime != null) {
                batch.setCreationTime(creationTime.toLocalDateTime());
            }
            
            Timestamp completedAt = rs.getTimestamp("completed_at");
            if (completedAt != null) {
                batch.setCompletedAt(completedAt.toLocalDateTime());
            }
            
            // 设置创建者
            Long createdByUserId = rs.getLong("created_by_user_id");
            if (!rs.wasNull()) {
                User user = new User();
                user.setId(createdByUserId);
                batch.setCreatedByUser(user);
                
                // 可选：通过UserRepository加载完整的用户对象
                // UserRepository.findById(createdByUserId).ifPresent(batch::setCreatedByUser);
            }
            
            // 设置数据集版本
            Long datasetVersionId = rs.getLong("dataset_version_id");
            if (!rs.wasNull()) {
                DatasetVersion datasetVersion = new DatasetVersion();
                datasetVersion.setId(datasetVersionId);
                batch.setDatasetVersion(datasetVersion);
            }
            
            // 设置配置
            String configJson = rs.getString("global_parameters");
            if (configJson != null) {
                // 转换为Map<String, Object>
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    Map<String, Object> globalParams = objectMapper.readValue(configJson, new TypeReference<Map<String, Object>>() {});
                    batch.setGlobalParameters(globalParams);
                } catch (Exception e) {
                    // 处理JSON解析异常
                    Map<String, Object> emptyMap = new HashMap<>();
                    batch.setGlobalParameters(emptyMap);
                }
            }
            
            return batch;
        }
    }

    /**
     * 保存答案生成批次并立即刷新
     * 在JDBC实现中与save方法功能相同，但保持与JPA接口兼容
     *
     * @param batch 答案生成批次对象
     * @return 带有ID的答案生成批次对象
     */
    public AnswerGenerationBatch saveAndFlush(AnswerGenerationBatch batch) {
        return save(batch);
    }
} 
