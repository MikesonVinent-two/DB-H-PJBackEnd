package com.example.demo.repository.jdbc;

import com.example.demo.entity.jdbc.ChangeLog;
import com.example.demo.entity.jdbc.StandardQuestion;
import com.example.demo.entity.jdbc.StandardObjectiveAnswer;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 基于JDBC的标准客观题答案仓库实现
 */
@Repository
public class StandardObjectiveAnswerRepository {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository UserRepository;

    private static final String SQL_INSERT = 
            "INSERT INTO standard_objective_answers (standard_question_id, options, correct_ids, " +
            "determined_by_user_id, determined_time, created_change_log_id, deleted_at) " +
            "VALUES (?, ?::json, ?::json, ?, ?, ?, ?)";
    
    private static final String SQL_UPDATE = 
            "UPDATE standard_objective_answers SET standard_question_id=?, options=?::json, correct_ids=?::json, " +
            "determined_by_user_id=?, determined_time=?, created_change_log_id=?, deleted_at=? " +
            "WHERE id=?";
    
    private static final String SQL_FIND_BY_ID = 
            "SELECT * FROM standard_objective_answers WHERE id=?";
    
    private static final String SQL_FIND_BY_STANDARD_QUESTION_ID = 
            "SELECT * FROM standard_objective_answers WHERE standard_question_id=?";
    
    private static final String SQL_FIND_BY_STANDARD_QUESTION_ID_AND_DELETED_AT_IS_NULL = 
            "SELECT * FROM standard_objective_answers WHERE standard_question_id=? AND deleted_at IS NULL";
    
    private static final String SQL_SOFT_DELETE = 
            "UPDATE standard_objective_answers SET deleted_at=? WHERE id=?";
    
    private static final String SQL_FIND_ALL = 
            "SELECT * FROM standard_objective_answers";

    @Autowired
    public StandardObjectiveAnswerRepository(JdbcTemplate jdbcTemplate, UserRepository UserRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.UserRepository = UserRepository;
    }

    /**
     * 保存标准客观题答�?
     *
     * @param answer 标准客观题答案对�?
     * @return 带有ID的标准客观题答案对象
     */
    public StandardObjectiveAnswer save(StandardObjectiveAnswer answer) {
        if (answer.getId() == null) {
            return insert(answer);
        } else {
            return update(answer);
        }
    }

    /**
     * 插入新标准客观题答案
     *
     * @param answer 标准客观题答案对�?
     * @return 带有ID的标准客观题答案对象
     */
    private StandardObjectiveAnswer insert(StandardObjectiveAnswer answer) {
        if (answer.getDeterminedTime() == null) {
            answer.setDeterminedTime(LocalDateTime.now());
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            
            // 设置标准问题ID
            ps.setLong(1, answer.getStandardQuestion().getId());
            
            // 设置选项(JSON格式)
            if (answer.getOptions() != null) {
                ps.setString(2, answer.getOptions());
            } else {
                ps.setString(2, "[]");
            }
            
            // 设置正确选项ID(JSON格式)
            if (answer.getCorrectOptionIds() != null) {
                ps.setString(3, answer.getCorrectOptionIds());
            } else {
                ps.setString(3, "[]");
            }
            
            // 设置确定人ID
            ps.setLong(4, answer.getDeterminedByUser().getId());
            
            // 设置确定时间
            ps.setTimestamp(5, Timestamp.valueOf(answer.getDeterminedTime()));
            
            // 设置创建变更日志ID
            if (answer.getCreatedChangeLog() != null && answer.getCreatedChangeLog().getId() != null) {
                ps.setLong(6, answer.getCreatedChangeLog().getId());
            } else {
                ps.setNull(6, java.sql.Types.BIGINT);
            }
            
            // 设置删除时间
            if (answer.getDeletedAt() != null) {
                ps.setTimestamp(7, Timestamp.valueOf(answer.getDeletedAt()));
            } else {
                ps.setNull(7, java.sql.Types.TIMESTAMP);
            }
            
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            answer.setId(key.longValue());
        }
        return answer;
    }

    /**
     * 更新标准客观题答�?
     *
     * @param answer 标准客观题答案对�?
     * @return 更新后的标准客观题答案对�?
     */
    private StandardObjectiveAnswer update(StandardObjectiveAnswer answer) {
        jdbcTemplate.update(SQL_UPDATE,
                answer.getStandardQuestion().getId(),
                answer.getOptions() != null ? answer.getOptions() : "[]",
                answer.getCorrectOptionIds() != null ? answer.getCorrectOptionIds() : "[]",
                answer.getDeterminedByUser().getId(),
                Timestamp.valueOf(answer.getDeterminedTime()),
                answer.getCreatedChangeLog() != null ? answer.getCreatedChangeLog().getId() : null,
                answer.getDeletedAt() != null ? Timestamp.valueOf(answer.getDeletedAt()) : null,
                answer.getId());

        return answer;
    }

    /**
     * 根据ID查找标准客观题答�?
     *
     * @param id 标准客观题答案ID
     * @return 标准客观题答案的Optional包装
     */
    public Optional<StandardObjectiveAnswer> findById(Long id) {
        try {
            StandardObjectiveAnswer answer = jdbcTemplate.queryForObject(SQL_FIND_BY_ID, new Object[]{id}, new StandardObjectiveAnswerRowMapper());
            return Optional.ofNullable(answer);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 根据标准问题ID查找标准客观题答�?
     *
     * @param standardQuestionId 标准问题ID
     * @return 标准客观题答案的Optional包装
     */
    public Optional<StandardObjectiveAnswer> findByStandardQuestionId(Long standardQuestionId) {
        try {
            StandardObjectiveAnswer answer = jdbcTemplate.queryForObject(
                    SQL_FIND_BY_STANDARD_QUESTION_ID,
                    new Object[]{standardQuestionId},
                    new StandardObjectiveAnswerRowMapper()
            );
            return Optional.ofNullable(answer);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 根据标准问题ID查找未删除的客观题答�?
     *
     * @param standardQuestionId 标准问题ID
     * @return 未删除的客观题答案的Optional包装
     */
    public Optional<StandardObjectiveAnswer> findByStandardQuestionIdAndDeletedAtIsNull(Long standardQuestionId) {
        try {
            StandardObjectiveAnswer answer = jdbcTemplate.queryForObject(
                    SQL_FIND_BY_STANDARD_QUESTION_ID_AND_DELETED_AT_IS_NULL,
                    new Object[]{standardQuestionId},
                    new StandardObjectiveAnswerRowMapper()
            );
            return Optional.ofNullable(answer);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
    
    /**
     * 软删除标准客观题答案
     *
     * @param id 标准客观题答案ID
     * @return 是否成功
     */
    public boolean softDelete(Long id) {
        int affected = jdbcTemplate.update(SQL_SOFT_DELETE, Timestamp.valueOf(LocalDateTime.now()), id);
        return affected > 0;
    }
    
    /**
     * 查找所有标准客观题答案
     *
     * @return 标准客观题答案列�?
     */
    public List<StandardObjectiveAnswer> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, new StandardObjectiveAnswerRowMapper());
    }

    /**
     * 标准客观题答案行映射�?
     */
    private class StandardObjectiveAnswerRowMapper implements RowMapper<StandardObjectiveAnswer> {
        @Override
        public StandardObjectiveAnswer mapRow(ResultSet rs, int rowNum) throws SQLException {
            StandardObjectiveAnswer answer = new StandardObjectiveAnswer();
            answer.setId(rs.getLong("id"));
            
            // 设置标准问题
            Long standardQuestionId = rs.getLong("standard_question_id");
            if (!rs.wasNull()) {
                StandardQuestion standardQuestion = new StandardQuestion();
                standardQuestion.setId(standardQuestionId);
                answer.setStandardQuestion(standardQuestion);
            }
            
            // 设置选项和正确答案ID(JSON格式)
            answer.setOptions(rs.getString("options"));
            answer.setCorrectOptionIds(rs.getString("correct_ids"));
            
            // 设置确定时间
            Timestamp determinedTime = rs.getTimestamp("determined_time");
            if (determinedTime != null) {
                answer.setDeterminedTime(determinedTime.toLocalDateTime());
            }
            
            // 设置删除时间
            Timestamp deletedAt = rs.getTimestamp("deleted_at");
            if (deletedAt != null) {
                answer.setDeletedAt(deletedAt.toLocalDateTime());
            }
            
            // 设置确定�?
            Long determinedByUserId = rs.getLong("determined_by_user_id");
            if (!rs.wasNull()) {
                UserRepository.findById(determinedByUserId).ifPresent(user -> answer.setDeterminedByUser(user));
            }
            
            // 设置创建变更日志
            Long createdChangeLogId = rs.getLong("created_change_log_id");
            if (!rs.wasNull()) {
                ChangeLog changeLog = new ChangeLog();
                changeLog.setId(createdChangeLogId);
                answer.setCreatedChangeLog(changeLog);
            }
            
            return answer;
        }
    }
} 
