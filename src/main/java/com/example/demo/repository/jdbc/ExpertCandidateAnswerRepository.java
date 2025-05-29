package com.example.demo.repository.jdbc;

import com.example.demo.entity.jdbc.ExpertCandidateAnswer;
import com.example.demo.entity.jdbc.StandardQuestion;
import com.example.demo.entity.jdbc.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 基于JDBC的专家候选答案仓库实�?
 */
@Repository
public class ExpertCandidateAnswerRepository {

    private final JdbcTemplate jdbcTemplate;
    private final StandardQuestionRepository standardQuestionRepository;
    private final UserRepository userRepository;

    private static final String SQL_INSERT = 
            "INSERT INTO EXPERT_CANDIDATE_ANSWERS (STANDARD_QUESTION_ID, USER_ID, CANDIDATE_ANSWER_TEXT, SUBMISSION_TIME, QUALITY_SCORE, FEEDBACK) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE = 
            "UPDATE EXPERT_CANDIDATE_ANSWERS SET STANDARD_QUESTION_ID=?, USER_ID=?, CANDIDATE_ANSWER_TEXT=?, SUBMISSION_TIME=?, QUALITY_SCORE=?, FEEDBACK=? " +
            "WHERE ID=?";

    private static final String SQL_FIND_BY_ID = 
            "SELECT * FROM EXPERT_CANDIDATE_ANSWERS WHERE ID=?";

    private static final String SQL_FIND_BY_STANDARD_QUESTION_ID = 
            "SELECT * FROM EXPERT_CANDIDATE_ANSWERS WHERE STANDARD_QUESTION_ID=?";

    private static final String SQL_FIND_BY_USER_ID = 
            "SELECT * FROM EXPERT_CANDIDATE_ANSWERS WHERE USER_ID=?";

    private static final String SQL_FIND_BY_STANDARD_QUESTION_ID_AND_USER_ID = 
            "SELECT * FROM EXPERT_CANDIDATE_ANSWERS WHERE STANDARD_QUESTION_ID=? AND USER_ID=?";

    private static final String SQL_FIND_BY_STANDARD_QUESTION_ID_PAGEABLE = 
            "SELECT * FROM EXPERT_CANDIDATE_ANSWERS WHERE STANDARD_QUESTION_ID=? " +
            "ORDER BY SUBMISSION_TIME DESC LIMIT ? OFFSET ?";

    private static final String SQL_COUNT_BY_STANDARD_QUESTION_ID = 
            "SELECT COUNT(*) FROM EXPERT_CANDIDATE_ANSWERS WHERE STANDARD_QUESTION_ID=?";

    private static final String SQL_FIND_BY_USER_ID_PAGEABLE = 
            "SELECT * FROM EXPERT_CANDIDATE_ANSWERS WHERE USER_ID=? " +
            "ORDER BY SUBMISSION_TIME DESC LIMIT ? OFFSET ?";

    private static final String SQL_COUNT_BY_USER_ID = 
            "SELECT COUNT(*) FROM EXPERT_CANDIDATE_ANSWERS WHERE USER_ID=?";

    private static final String SQL_FIND_BY_QUALITY_SCORE_GREATER_THAN = 
            "SELECT * FROM EXPERT_CANDIDATE_ANSWERS WHERE QUALITY_SCORE > ?";

    private static final String SQL_FIND_ALL = 
            "SELECT * FROM EXPERT_CANDIDATE_ANSWERS";

    private static final String SQL_DELETE = 
            "DELETE FROM EXPERT_CANDIDATE_ANSWERS WHERE ID=?";

    @Autowired
    public ExpertCandidateAnswerRepository(JdbcTemplate jdbcTemplate, 
                                             StandardQuestionRepository standardQuestionRepository,
                                             UserRepository userRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.standardQuestionRepository = standardQuestionRepository;
        this.userRepository = userRepository;
    }

    /**
     * 保存专家候选答�?
     *
     * @param expertCandidateAnswer 专家候选答案对�?
     * @return 带有ID的专家候选答案对�?
     */
    public ExpertCandidateAnswer save(ExpertCandidateAnswer expertCandidateAnswer) {
        if (expertCandidateAnswer.getId() == null) {
            return insert(expertCandidateAnswer);
        } else {
            return update(expertCandidateAnswer);
        }
    }

    /**
     * 插入新专家候选答�?
     *
     * @param expertCandidateAnswer 专家候选答案对�?
     * @return 带有ID的专家候选答案对�?
     */
    private ExpertCandidateAnswer insert(ExpertCandidateAnswer expertCandidateAnswer) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        // 设置默认提交时间
        if (expertCandidateAnswer.getSubmissionTime() == null) {
            expertCandidateAnswer.setSubmissionTime(LocalDateTime.now());
        }

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            
            // 设置标准问题ID
            ps.setLong(1, expertCandidateAnswer.getStandardQuestion().getId());
            
            // 设置用户ID
            ps.setLong(2, expertCandidateAnswer.getUser().getId());
            
            // 设置候选答案文�?
            ps.setString(3, expertCandidateAnswer.getCandidateAnswerText());
            
            // 设置提交时间
            ps.setTimestamp(4, Timestamp.valueOf(expertCandidateAnswer.getSubmissionTime()));
            
            // 设置质量评分
            if (expertCandidateAnswer.getQualityScore() != null) {
                ps.setInt(5, expertCandidateAnswer.getQualityScore());
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            
            // 设置反馈
            if (expertCandidateAnswer.getFeedback() != null) {
                ps.setString(6, expertCandidateAnswer.getFeedback());
            } else {
                ps.setNull(6, Types.VARCHAR);
            }
            
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            expertCandidateAnswer.setId(key.longValue());
        }

        return expertCandidateAnswer;
    }

    /**
     * 更新专家候选答�?
     *
     * @param expertCandidateAnswer 专家候选答案对�?
     * @return 更新后的专家候选答案对�?
     */
    private ExpertCandidateAnswer update(ExpertCandidateAnswer expertCandidateAnswer) {
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_UPDATE);
            
            // 设置标准问题ID
            ps.setLong(1, expertCandidateAnswer.getStandardQuestion().getId());
            
            // 设置用户ID
            ps.setLong(2, expertCandidateAnswer.getUser().getId());
            
            // 设置候选答案文�?
            ps.setString(3, expertCandidateAnswer.getCandidateAnswerText());
            
            // 设置提交时间
            ps.setTimestamp(4, Timestamp.valueOf(expertCandidateAnswer.getSubmissionTime()));
            
            // 设置质量评分
            if (expertCandidateAnswer.getQualityScore() != null) {
                ps.setInt(5, expertCandidateAnswer.getQualityScore());
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            
            // 设置反馈
            if (expertCandidateAnswer.getFeedback() != null) {
                ps.setString(6, expertCandidateAnswer.getFeedback());
            } else {
                ps.setNull(6, Types.VARCHAR);
            }
            
            // 设置ID
            ps.setLong(7, expertCandidateAnswer.getId());
            
            return ps;
        });

        return expertCandidateAnswer;
    }

    /**
     * 根据ID查找专家候选答�?
     *
     * @param id 专家候选答案ID
     * @return 专家候选答案对�?
     */
    public Optional<ExpertCandidateAnswer> findById(Long id) {
        try {
            ExpertCandidateAnswer expertCandidateAnswer = jdbcTemplate.queryForObject(
                SQL_FIND_BY_ID, 
                new ExpertCandidateAnswerRowMapper(), 
                id
            );
            return Optional.ofNullable(expertCandidateAnswer);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 根据标准问题ID查找专家候选答案列�?
     *
     * @param standardQuestionId 标准问题ID
     * @return 专家候选答案列�?
     */
    public List<ExpertCandidateAnswer> findByStandardQuestionId(Long standardQuestionId) {
        try {
            return jdbcTemplate.query(
                SQL_FIND_BY_STANDARD_QUESTION_ID, 
                new ExpertCandidateAnswerRowMapper(), 
                standardQuestionId
            );
        } catch (EmptyResultDataAccessException e) {
            return Collections.emptyList();
        }
    }

    /**
     * 根据用户ID查找专家候选答案列�?
     *
     * @param userId 用户ID
     * @return 专家候选答案列�?
     */
    public List<ExpertCandidateAnswer> findByUserId(Long userId) {
        try {
            return jdbcTemplate.query(
                SQL_FIND_BY_USER_ID, 
                new ExpertCandidateAnswerRowMapper(), 
                userId
            );
        } catch (EmptyResultDataAccessException e) {
            return Collections.emptyList();
        }
    }

    /**
     * 根据标准问题ID和用户ID查找专家候选答�?
     *
     * @param standardQuestionId 标准问题ID
     * @param userId 用户ID
     * @return 专家候选答案对�?
     */
    public Optional<ExpertCandidateAnswer> findByStandardQuestionIdAndUserId(Long standardQuestionId, Long userId) {
        try {
            ExpertCandidateAnswer expertCandidateAnswer = jdbcTemplate.queryForObject(
                SQL_FIND_BY_STANDARD_QUESTION_ID_AND_USER_ID, 
                new ExpertCandidateAnswerRowMapper(), 
                standardQuestionId, 
                userId
            );
            return Optional.ofNullable(expertCandidateAnswer);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 根据标准问题ID分页查找专家候选答�?
     *
     * @param standardQuestionId 标准问题ID
     * @param pageable 分页参数
     * @return 分页结果
     */
    public Page<ExpertCandidateAnswer> findByStandardQuestionId(Long standardQuestionId, Pageable pageable) {
        // 查询总数
        Integer total = jdbcTemplate.queryForObject(
            SQL_COUNT_BY_STANDARD_QUESTION_ID,
            Integer.class,
            standardQuestionId
        );
        
        // 查询数据
        List<ExpertCandidateAnswer> content = jdbcTemplate.query(
            SQL_FIND_BY_STANDARD_QUESTION_ID_PAGEABLE,
            new ExpertCandidateAnswerRowMapper(),
            standardQuestionId,
            pageable.getPageSize(),
            pageable.getOffset()
        );
        
        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    /**
     * 根据用户ID分页查找专家候选答�?
     *
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 分页结果
     */
    public Page<ExpertCandidateAnswer> findByUserId(Long userId, Pageable pageable) {
        // 查询总数
        Integer total = jdbcTemplate.queryForObject(
            SQL_COUNT_BY_USER_ID,
            Integer.class,
            userId
        );
        
        // 查询数据
        List<ExpertCandidateAnswer> content = jdbcTemplate.query(
            SQL_FIND_BY_USER_ID_PAGEABLE,
            new ExpertCandidateAnswerRowMapper(),
            userId,
            pageable.getPageSize(),
            pageable.getOffset()
        );
        
        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    /**
     * 查找质量评分大于指定值的专家候选答�?
     *
     * @param score 质量评分阈�?
     * @return 专家候选答案列�?
     */
    public List<ExpertCandidateAnswer> findByQualityScoreGreaterThan(Integer score) {
        try {
            return jdbcTemplate.query(
                SQL_FIND_BY_QUALITY_SCORE_GREATER_THAN, 
                new ExpertCandidateAnswerRowMapper(), 
                score
            );
        } catch (EmptyResultDataAccessException e) {
            return Collections.emptyList();
        }
    }

    /**
     * 查找所有专家候选答�?
     *
     * @return 所有专家候选答案列�?
     */
    public List<ExpertCandidateAnswer> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, new ExpertCandidateAnswerRowMapper());
    }

    /**
     * 删除专家候选答�?
     *
     * @param expertCandidateAnswer 专家候选答案对�?
     */
    public void delete(ExpertCandidateAnswer expertCandidateAnswer) {
        jdbcTemplate.update(SQL_DELETE, expertCandidateAnswer.getId());
    }

    /**
     * 专家候选答案行映射�?
     */
    private class ExpertCandidateAnswerRowMapper implements RowMapper<ExpertCandidateAnswer> {
        @Override
        public ExpertCandidateAnswer mapRow(ResultSet rs, int rowNum) throws SQLException {
            ExpertCandidateAnswer expertCandidateAnswer = new ExpertCandidateAnswer();
            
            // 设置ID
            expertCandidateAnswer.setId(rs.getLong("ID"));
            
            // 设置标准问题
            Long standardQuestionId = rs.getLong("standard_question_id");
            if (!rs.wasNull()) {
                standardQuestionRepository.findById(standardQuestionId).ifPresent(question -> 
                    expertCandidateAnswer.setStandardQuestion(question));
            }
            
            // 设置用户
            Long userId = rs.getLong("user_id");
            if (!rs.wasNull()) {
                userRepository.findById(userId).ifPresent(user -> 
                    expertCandidateAnswer.setUser(user));
            }
            
            // 设置候选答案文�?
            expertCandidateAnswer.setCandidateAnswerText(rs.getString("CANDIDATE_ANSWER_TEXT"));
            
            // 设置提交时间
            Timestamp submissionTime = rs.getTimestamp("SUBMISSION_TIME");
            if (submissionTime != null) {
                expertCandidateAnswer.setSubmissionTime(submissionTime.toLocalDateTime());
            }
            
            // 设置质量评分
            Integer qualityScore = rs.getInt("QUALITY_SCORE");
            if (!rs.wasNull()) {
                expertCandidateAnswer.setQualityScore(qualityScore);
            }
            
            // 设置反馈
            expertCandidateAnswer.setFeedback(rs.getString("FEEDBACK"));
            
            return expertCandidateAnswer;
        }
    }
} 
