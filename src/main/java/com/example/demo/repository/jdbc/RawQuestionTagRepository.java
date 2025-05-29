package com.example.demo.repository.jdbc;

import com.example.demo.entity.jdbc.RawQuestion;
import com.example.demo.entity.jdbc.RawQuestionTag;
import com.example.demo.entity.jdbc.Tag;
import com.example.demo.entity.jdbc.User;
import com.example.demo.entity.jdbc.ChangeLog;
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
import java.util.List;
import java.util.Optional;

/**
 * 基于JDBC的原始问题标签仓库实�?
 */
@Repository
public class RawQuestionTagRepository {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;

    private static final String SQL_INSERT = 
            "INSERT INTO raw_question_tags (raw_question_id, tag_id, created_at, created_by_user_id, created_change_log_id) " +
            "VALUES (?, ?, CURRENT_TIMESTAMP, ?, ?)";
    
    private static final String SQL_FIND_BY_ID = 
            "SELECT * FROM raw_question_tags WHERE id=?";
    
    private static final String SQL_FIND_BY_RAW_QUESTION_ID = 
            "SELECT * FROM raw_question_tags WHERE raw_question_id=?";
    
    private static final String SQL_FIND_BY_TAG_ID = 
            "SELECT * FROM raw_question_tags WHERE tag_id=?";
    
    private static final String SQL_DELETE_BY_RAW_QUESTION_ID_AND_TAG_ID = 
            "DELETE FROM raw_question_tags WHERE raw_question_id=? AND tag_id=?";
    
    private static final String SQL_EXISTS_BY_RAW_QUESTION_ID_AND_TAG_ID = 
            "SELECT COUNT(*) FROM raw_question_tags WHERE raw_question_id=? AND tag_id=?";
    
    private static final String SQL_FIND_ALL = 
            "SELECT * FROM raw_question_tags";
    
    private static final String SQL_DELETE = 
            "DELETE FROM raw_question_tags WHERE id=?";

    @Autowired
    public RawQuestionTagRepository(JdbcTemplate jdbcTemplate, UserRepository userRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
    }

    /**
     * 保存原始问题标签
     *
     * @param rawQuestionTag 原始问题标签对象
     * @return 带有ID的原始问题标签对�?
     */
    public RawQuestionTag save(RawQuestionTag rawQuestionTag) {
        if (rawQuestionTag.getId() == null) {
            return insert(rawQuestionTag);
        } else {
            // 由于原始问题标签是简单的关联实体，通常不需要更新，只需插入和删�?
            // 如果有需要，可以实现update方法
            return rawQuestionTag;
        }
    }

    /**
     * 插入新原始问题标�?
     *
     * @param rawQuestionTag 原始问题标签对象
     * @return 带有ID的原始问题标签对�?
     */
    private RawQuestionTag insert(RawQuestionTag rawQuestionTag) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            
            // 设置原始问题ID
            ps.setLong(1, rawQuestionTag.getRawQuestion().getId());
            
            // 设置标签ID
            ps.setLong(2, rawQuestionTag.getTag().getId());
            
            // 设置创建者用户ID
            if (rawQuestionTag.getCreatedByUser() != null && rawQuestionTag.getCreatedByUser().getId() != null) {
                ps.setLong(3, rawQuestionTag.getCreatedByUser().getId());
            } else {
                ps.setNull(3, java.sql.Types.BIGINT);
            }
            
            // 设置创建变更日志ID
            if (rawQuestionTag.getCreatedChangeLog() != null && rawQuestionTag.getCreatedChangeLog().getId() != null) {
                ps.setLong(4, rawQuestionTag.getCreatedChangeLog().getId());
            } else {
                ps.setNull(4, java.sql.Types.BIGINT);
            }
            
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            rawQuestionTag.setId(key.longValue());
        }

        return rawQuestionTag;
    }

    /**
     * 根据ID查找原始问题标签
     *
     * @param id 原始问题标签ID
     * @return 原始问题标签对象
     */
    public Optional<RawQuestionTag> findById(Long id) {
        try {
            RawQuestionTag rawQuestionTag = jdbcTemplate.queryForObject(
                SQL_FIND_BY_ID, 
                new RawQuestionTagRowMapper(), 
                id
            );
            return Optional.ofNullable(rawQuestionTag);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 根据原始问题查找原始问题标签
     *
     * @param rawQuestion 原始问题对象
     * @return 原始问题标签列表
     */
    public List<RawQuestionTag> findByRawQuestion(RawQuestion rawQuestion) {
        return jdbcTemplate.query(
            SQL_FIND_BY_RAW_QUESTION_ID, 
            new RawQuestionTagRowMapper(), 
            rawQuestion.getId()
        );
    }

    /**
     * 根据原始问题ID查找原始问题标签
     *
     * @param rawQuestionId 原始问题ID
     * @return 原始问题标签列表
     */
    public List<RawQuestionTag> findByRawQuestionId(Long rawQuestionId) {
        return jdbcTemplate.query(
            SQL_FIND_BY_RAW_QUESTION_ID, 
            new RawQuestionTagRowMapper(), 
            rawQuestionId
        );
    }

    /**
     * 根据标签查找原始问题标签
     *
     * @param tag 标签对象
     * @return 原始问题标签列表
     */
    public List<RawQuestionTag> findByTag(Tag tag) {
        return jdbcTemplate.query(
            SQL_FIND_BY_TAG_ID, 
            new RawQuestionTagRowMapper(), 
            tag.getId()
        );
    }

    /**
     * 根据原始问题和标签删除原始问题标�?
     *
     * @param rawQuestion 原始问题对象
     * @param tag 标签对象
     */
    public void deleteByRawQuestionAndTag(RawQuestion rawQuestion, Tag tag) {
        jdbcTemplate.update(
            SQL_DELETE_BY_RAW_QUESTION_ID_AND_TAG_ID, 
            rawQuestion.getId(), 
            tag.getId()
        );
    }

    /**
     * 检查原始问题和标签的关联是否存�?
     *
     * @param rawQuestion 原始问题对象
     * @param tag 标签对象
     * @return 是否存在
     */
    public boolean existsByRawQuestionAndTag(RawQuestion rawQuestion, Tag tag) {
        Integer count = jdbcTemplate.queryForObject(
            SQL_EXISTS_BY_RAW_QUESTION_ID_AND_TAG_ID, 
            Integer.class, 
            rawQuestion.getId(), 
            tag.getId()
        );
        return count != null && count > 0;
    }

    /**
     * 查找所有原始问题标�?
     *
     * @return 所有原始问题标签列�?
     */
    public List<RawQuestionTag> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, new RawQuestionTagRowMapper());
    }

    /**
     * 删除原始问题标签
     *
     * @param rawQuestionTag 原始问题标签对象
     */
    public void delete(RawQuestionTag rawQuestionTag) {
        jdbcTemplate.update(SQL_DELETE, rawQuestionTag.getId());
    }

    /**
     * 原始问题标签行映射器
     */
    private class RawQuestionTagRowMapper implements RowMapper<RawQuestionTag> {
        @Override
        public RawQuestionTag mapRow(ResultSet rs, int rowNum) throws SQLException {
            RawQuestionTag rawQuestionTag = new RawQuestionTag();
            
            // 设置ID
            rawQuestionTag.setId(rs.getLong("id"));
            
            // 设置原始问题
            Long rawQuestionId = rs.getLong("raw_question_id");
            RawQuestion rawQuestion = new RawQuestion();
            rawQuestion.setId(rawQuestionId);
            rawQuestionTag.setRawQuestion(rawQuestion);
            
            // 设置标签
            Long tagId = rs.getLong("tag_id");
            Tag tag = new Tag();
            tag.setId(tagId);
            rawQuestionTag.setTag(tag);
            
            // 设置创建时间
            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                rawQuestionTag.setCreatedAt(createdAt.toLocalDateTime());
            }
            
            // 设置创建者用�?
            Long createdByUserId = rs.getLong("created_by_user_id");
            if (!rs.wasNull()) {
                userRepository.findById(createdByUserId).ifPresent(user -> rawQuestionTag.setCreatedByUser(user));
            }
            
            // 设置创建变更日志
            Long createdChangeLogId = rs.getLong("created_change_log_id");
            if (!rs.wasNull()) {
                ChangeLog changeLog = new ChangeLog();
                changeLog.setId(createdChangeLogId);
                rawQuestionTag.setCreatedChangeLog(changeLog);
            }
            
            return rawQuestionTag;
        }
    }
} 
