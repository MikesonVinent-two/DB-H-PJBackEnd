package com.example.demo.repository.jdbc;

import com.example.demo.entity.jdbc.RawAnswer;
import com.example.demo.entity.jdbc.RawQuestion;
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
 * 基于JDBC的原始回答仓库实�?
 */
@Repository
public class RawAnswerRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String SQL_INSERT = 
            "INSERT INTO raw_answers (raw_question_id, author_info, content, publish_time, upvotes, is_accepted, other_metadata) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";
    
    private static final String SQL_UPDATE = 
            "UPDATE raw_answers SET raw_question_id=?, author_info=?, content=?, publish_time=?, upvotes=?, is_accepted=?, other_metadata=? " +
            "WHERE id=?";
    
    private static final String SQL_FIND_BY_ID = 
            "SELECT * FROM raw_answers WHERE id=?";
    
    private static final String SQL_FIND_BY_RAW_QUESTION_ID = 
            "SELECT * FROM raw_answers WHERE raw_question_id=?";
    
    private static final String SQL_FIND_ALL = 
            "SELECT * FROM raw_answers";
    
    private static final String SQL_DELETE = 
            "DELETE FROM raw_answers WHERE id=?";

    @Autowired
    public RawAnswerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 保存原始回答
     *
     * @param rawAnswer 原始回答对象
     * @return 带有ID的原始回答对�?
     */
    public RawAnswer save(RawAnswer rawAnswer) {
        if (rawAnswer.getId() == null) {
            return insert(rawAnswer);
        } else {
            return update(rawAnswer);
        }
    }

    /**
     * 插入新原始回�?
     *
     * @param rawAnswer 原始回答对象
     * @return 带有ID的原始回答对�?
     */
    private RawAnswer insert(RawAnswer rawAnswer) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            
            if (rawAnswer.getRawQuestion() != null && rawAnswer.getRawQuestion().getId() != null) {
                ps.setLong(1, rawAnswer.getRawQuestion().getId());
            } else {
                ps.setNull(1, java.sql.Types.BIGINT);
            }
            
            ps.setString(2, rawAnswer.getAuthorInfo());
            ps.setString(3, rawAnswer.getContent());
            
            if (rawAnswer.getPublishTime() != null) {
                ps.setTimestamp(4, Timestamp.valueOf(rawAnswer.getPublishTime()));
            } else {
                ps.setNull(4, java.sql.Types.TIMESTAMP);
            }
            
            ps.setInt(5, rawAnswer.getUpvotes() != null ? rawAnswer.getUpvotes() : 0);
            ps.setBoolean(6, rawAnswer.getIsAccepted() != null ? rawAnswer.getIsAccepted() : false);
            ps.setString(7, rawAnswer.getOtherMetadata());
            
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            rawAnswer.setId(key.longValue());
        }
        return rawAnswer;
    }

    /**
     * 更新原始回答
     *
     * @param rawAnswer 原始回答对象
     * @return 更新后的原始回答对象
     */
    private RawAnswer update(RawAnswer rawAnswer) {
        jdbcTemplate.update(SQL_UPDATE,
                rawAnswer.getRawQuestion().getId(),
                rawAnswer.getAuthorInfo(),
                rawAnswer.getContent(),
                rawAnswer.getPublishTime() != null ? Timestamp.valueOf(rawAnswer.getPublishTime()) : null,
                rawAnswer.getUpvotes(),
                rawAnswer.getIsAccepted(),
                rawAnswer.getOtherMetadata(),
                rawAnswer.getId());

        return rawAnswer;
    }

    /**
     * 根据ID查找原始回答
     *
     * @param id 原始回答ID
     * @return 原始回答的Optional包装
     */
    public Optional<RawAnswer> findById(Long id) {
        try {
            RawAnswer rawAnswer = jdbcTemplate.queryForObject(SQL_FIND_BY_ID, new Object[]{id}, new RawAnswerRowMapper());
            return Optional.ofNullable(rawAnswer);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
    
    /**
     * 根据原始问题ID查找所有原始回�?
     *
     * @param rawQuestionId 原始问题ID
     * @return 原始回答列表
     */
    public List<RawAnswer> findByRawQuestionId(Long rawQuestionId) {
        return jdbcTemplate.query(SQL_FIND_BY_RAW_QUESTION_ID, new Object[]{rawQuestionId}, new RawAnswerRowMapper());
    }

    /**
     * 查找所有原始回�?
     *
     * @return 原始回答列表
     */
    public List<RawAnswer> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, new RawAnswerRowMapper());
    }

    /**
     * 删除原始回答
     *
     * @param id 原始回答ID
     * @return 是否成功
     */
    public boolean delete(Long id) {
        int affected = jdbcTemplate.update(SQL_DELETE, id);
        return affected > 0;
    }

    /**
     * 原始回答行映射器
     */
    private class RawAnswerRowMapper implements RowMapper<RawAnswer> {
        @Override
        public RawAnswer mapRow(ResultSet rs, int rowNum) throws SQLException {
            RawAnswer rawAnswer = new RawAnswer();
            rawAnswer.setId(rs.getLong("id"));
            rawAnswer.setAuthorInfo(rs.getString("author_info"));
            rawAnswer.setContent(rs.getString("content"));
            
            Timestamp publishTime = rs.getTimestamp("publish_time");
            if (publishTime != null) {
                rawAnswer.setPublishTime(publishTime.toLocalDateTime());
            }
            
            rawAnswer.setUpvotes(rs.getInt("upvotes"));
            rawAnswer.setIsAccepted(rs.getBoolean("is_accepted"));
            rawAnswer.setOtherMetadata(rs.getString("other_metadata"));
            
            // 处理外键关联 - 这里我们只设置RawQuestion的ID，避免过度加�?
            Long rawQuestionId = rs.getLong("raw_question_id");
            if (!rs.wasNull()) {
                RawQuestion rawQuestion = new RawQuestion();
                rawQuestion.setId(rawQuestionId);
                rawAnswer.setRawQuestion(rawQuestion);
            }
            
            return rawAnswer;
        }
    }
} 
