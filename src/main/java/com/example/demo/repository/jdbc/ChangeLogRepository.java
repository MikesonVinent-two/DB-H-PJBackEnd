package com.example.demo.repository.jdbc;

import com.example.demo.entity.jdbc.ChangeLog;
import com.example.demo.entity.jdbc.ChangeType;
import com.example.demo.entity.jdbc.StandardQuestion;
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
 * 基于JDBC的变更日志仓库实?
 */
@Repository
public class ChangeLogRepository {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository UserRepository;

    private static final String SQL_INSERT = 
            "INSERT INTO change_log (change_type, changed_by_user_id, change_time, commit_message, associated_standard_question_id) " +
            "VALUES (?, ?, ?, ?, ?)";
    
    private static final String SQL_UPDATE = 
            "UPDATE change_log SET change_type=?, changed_by_user_id=?, change_time=?, commit_message=?, associated_standard_question_id=? " +
            "WHERE id=?";
    
    private static final String SQL_FIND_BY_ID = 
            "SELECT * FROM change_log WHERE id=?";
    
    private static final String SQL_FIND_ALL = 
            "SELECT * FROM change_log";
    
    private static final String SQL_DELETE = 
            "DELETE FROM change_log WHERE id=?";
    
    private static final String SQL_FIND_BY_ASSOCIATED_STANDARD_QUESTION = 
            "SELECT cl.* FROM change_log cl " +
            "INNER JOIN standard_questions sq ON sq.created_change_log_id = cl.id " +
            "WHERE sq.id = ?";

    @Autowired
    public ChangeLogRepository(JdbcTemplate jdbcTemplate, UserRepository UserRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.UserRepository = UserRepository;
    }

    /**
     * 保存变更日志
     *
     * @param changeLog 变更日志对象
     * @return 带有ID的变更日志对?
     */
    public ChangeLog save(ChangeLog changeLog) {
        if (changeLog.getId() == null) {
            return insert(changeLog);
        } else {
            return update(changeLog);
        }
    }

    /**
     * 插入新变更日?
     *
     * @param changeLog 变更日志对象
     * @return 带有ID的变更日志对?
     */
    private ChangeLog insert(ChangeLog changeLog) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, changeLog.getChangeType().name());
            
            if (changeLog.getChangedByUser() != null && changeLog.getChangedByUser().getId() != null) {
                ps.setLong(2, changeLog.getChangedByUser().getId());
            } else {
                ps.setNull(2, java.sql.Types.BIGINT);
            }
            
            if (changeLog.getChangeTime() != null) {
                ps.setTimestamp(3, Timestamp.valueOf(changeLog.getChangeTime()));
            } else {
                ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            }
            
            ps.setString(4, changeLog.getCommitMessage());
            
            // 设置关联的标准问题ID
            if (changeLog.getAssociatedStandardQuestion() != null && changeLog.getAssociatedStandardQuestion().getId() != null) {
                ps.setLong(5, changeLog.getAssociatedStandardQuestion().getId());
            } else {
                ps.setNull(5, java.sql.Types.BIGINT);
            }
            
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            changeLog.setId(key.longValue());
        }
        return changeLog;
    }

    /**
     * 更新变更日志
     *
     * @param changeLog 变更日志对象
     * @return 更新后的变更日志对象
     */
    private ChangeLog update(ChangeLog changeLog) {
        jdbcTemplate.update(SQL_UPDATE,
                changeLog.getChangeType().name(),
                changeLog.getChangedByUser() != null ? changeLog.getChangedByUser().getId() : null,
                changeLog.getChangeTime() != null ? Timestamp.valueOf(changeLog.getChangeTime()) : Timestamp.valueOf(LocalDateTime.now()),
                changeLog.getCommitMessage(),
                changeLog.getAssociatedStandardQuestion() != null ? changeLog.getAssociatedStandardQuestion().getId() : null,
                changeLog.getId());

        return changeLog;
    }

    /**
     * 根据ID查找变更日志
     *
     * @param id 变更日志ID
     * @return 变更日志的Optional包装
     */
    public Optional<ChangeLog> findById(Long id) {
        try {
            ChangeLog changeLog = jdbcTemplate.queryForObject(SQL_FIND_BY_ID, new Object[]{id}, new ChangeLogRowMapper());
            return Optional.ofNullable(changeLog);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
    
    /**
     * 根据关联的标准问题查找变更日?
     *
     * @param question 标准问题对象
     * @return 变更日志
     */
    public ChangeLog findByAssociatedStandardQuestion(StandardQuestion question) {
        try {
            return jdbcTemplate.queryForObject(SQL_FIND_BY_ASSOCIATED_STANDARD_QUESTION, 
                    new Object[]{question.getId()}, 
                    new ChangeLogRowMapper());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    /**
     * 查找所有变更日?
     *
     * @return 变更日志列表
     */
    public List<ChangeLog> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, new ChangeLogRowMapper());
    }

    /**
     * 删除变更日志
     *
     * @param id 变更日志ID
     * @return 是否成功
     */
    public boolean delete(Long id) {
        int affected = jdbcTemplate.update(SQL_DELETE, id);
        return affected > 0;
    }

    /**
     * 变更日志行映射器
     */
    private class ChangeLogRowMapper implements RowMapper<ChangeLog> {
        @Override
        public ChangeLog mapRow(ResultSet rs, int rowNum) throws SQLException {
            ChangeLog changeLog = new ChangeLog();
            changeLog.setId(rs.getLong("id"));
            
            // 解析枚举
            String changeTypeStr = rs.getString("change_type");
            if (changeTypeStr != null) {
                changeLog.setChangeType(ChangeType.valueOf(changeTypeStr));
            }
            
            // 设置时间
            Timestamp changeTime = rs.getTimestamp("change_time");
            if (changeTime != null) {
                changeLog.setChangeTime(changeTime.toLocalDateTime());
            }
            
            changeLog.setCommitMessage(rs.getString("commit_message"));
            
            // 处理外键关联
            Long changedByUserId = rs.getLong("changed_by_user_id");
            if (!rs.wasNull()) {
                UserRepository.findById(changedByUserId).ifPresent(changeLog::setChangedByUser);
            }
            
            // 设置关联的标准问?
            Long associatedStandardQuestionId = rs.getLong("associated_standard_question_id");
            if (!rs.wasNull()) {
                StandardQuestion question = new StandardQuestion();
                question.setId(associatedStandardQuestionId);
                changeLog.setAssociatedStandardQuestion(question);
            }
            
            // 注意：这里没有加载关联的details和其他关联对?
            // 这些关联对象需要在服务层按需加载
            
            return changeLog;
        }
    }
} 
