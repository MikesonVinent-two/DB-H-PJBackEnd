package com.example.demo.repository.jdbc;

import com.example.demo.entity.jdbc.AnswerPromptAssemblyConfig;
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
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 基于JDBC的回答场景prompt组装配置仓库实现
 */
@Repository
public class AnswerPromptAssemblyConfigRepository {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository UserRepository;

    private static final String SQL_INSERT = 
            "INSERT INTO answer_prompt_assembly_configs " +
            "(name, description, is_active, base_system_prompt, created_by_user_id, " +
            "created_at, updated_at, tag_prompts_section_header, question_type_section_header, " +
            "tag_prompt_separator, section_separator, final_instruction, created_change_log_id) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    private static final String SQL_UPDATE = 
            "UPDATE answer_prompt_assembly_configs SET " +
            "name=?, description=?, is_active=?, base_system_prompt=?, created_by_user_id=?, " +
            "created_at=?, updated_at=?, tag_prompts_section_header=?, question_type_section_header=?, " +
            "tag_prompt_separator=?, section_separator=?, final_instruction=?, created_change_log_id=? " +
            "WHERE id=?";
    
    private static final String SQL_FIND_BY_ID = 
            "SELECT * FROM answer_prompt_assembly_configs WHERE id=?";
    
    private static final String SQL_FIND_BY_IS_ACTIVE_TRUE = 
            "SELECT * FROM answer_prompt_assembly_configs WHERE is_active=true";
    
    private static final String SQL_FIND_BY_NAME = 
            "SELECT * FROM answer_prompt_assembly_configs WHERE name=?";
    
    private static final String SQL_FIND_BY_CREATED_BY_USER_ID = 
            "SELECT * FROM answer_prompt_assembly_configs WHERE created_by_user_id=?";
    
    private static final String SQL_FIND_ALL = 
            "SELECT * FROM answer_prompt_assembly_configs";
    
    private static final String SQL_DELETE = 
            "DELETE FROM answer_prompt_assembly_configs WHERE id=?";

    @Autowired
    public AnswerPromptAssemblyConfigRepository(JdbcTemplate jdbcTemplate, UserRepository UserRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.UserRepository = UserRepository;
    }

    /**
     * 保存配置
     *
     * @param config 配置对象
     * @return 带有ID的配置对�?
     */
    public AnswerPromptAssemblyConfig save(AnswerPromptAssemblyConfig config) {
        if (config.getId() == null) {
            return insert(config);
        } else {
            return update(config);
        }
    }

    /**
     * 插入新配�?
     *
     * @param config 配置对象
     * @return 带有ID的配置对�?
     */
    private AnswerPromptAssemblyConfig insert(AnswerPromptAssemblyConfig config) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        // 确保创建时间和最后修改时间已设置
        if (config.getCreatedAt() == null) {
            config.setCreatedAt(LocalDateTime.now());
        }
        if (config.getUpdatedAt() == null) {
            config.setUpdatedAt(config.getCreatedAt());
        }

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            
            // 设置名称
            ps.setString(1, config.getName());
            
            // 设置描述
            if (config.getDescription() != null) {
                ps.setString(2, config.getDescription());
            } else {
                ps.setNull(2, Types.VARCHAR);
            }
            
            // 设置是否激�?
            ps.setBoolean(3, config.getIsActive());
            
            // 设置基础系统提示
            ps.setString(4, config.getBaseSystemPrompt());
            
            // 设置创建者ID
            if (config.getCreatedByUser() != null && config.getCreatedByUser().getId() != null) {
                ps.setLong(5, config.getCreatedByUser().getId());
            } else {
                ps.setNull(5, Types.BIGINT);
            }
            
            // 设置创建时间
            ps.setTimestamp(6, Timestamp.valueOf(config.getCreatedAt()));
            
            // 设置更新时间
            ps.setTimestamp(7, Timestamp.valueOf(config.getUpdatedAt()));
            
            // 设置标签提示部分标题
            ps.setString(8, config.getTagPromptsSectionHeader());
            
            // 设置问题类型部分标题
            ps.setString(9, config.getQuestionTypeSectionHeader());
            
            // 设置标签提示分隔�?
            ps.setString(10, config.getTagPromptSeparator());
            
            // 设置部分分隔�?
            ps.setString(11, config.getSectionSeparator());
            
            // 设置最终指�?
            ps.setString(12, config.getFinalInstruction());
            
            // 设置关联的变更日志ID
            if (config.getCreatedChangeLog() != null && config.getCreatedChangeLog().getId() != null) {
                ps.setLong(13, config.getCreatedChangeLog().getId());
            } else {
                ps.setNull(13, Types.BIGINT);
            }
            
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            config.setId(key.longValue());
        }

        return config;
    }

    /**
     * 更新配置
     *
     * @param config 配置对象
     * @return 更新后的配置对象
     */
    private AnswerPromptAssemblyConfig update(AnswerPromptAssemblyConfig config) {
        // 更新最后修改时�?
        config.setUpdatedAt(LocalDateTime.now());

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_UPDATE);
            
            // 设置名称
            ps.setString(1, config.getName());
            
            // 设置描述
            if (config.getDescription() != null) {
                ps.setString(2, config.getDescription());
            } else {
                ps.setNull(2, Types.VARCHAR);
            }
            
            // 设置是否激�?
            ps.setBoolean(3, config.getIsActive());
            
            // 设置基础系统提示
            ps.setString(4, config.getBaseSystemPrompt());
            
            // 设置创建者ID
            if (config.getCreatedByUser() != null && config.getCreatedByUser().getId() != null) {
                ps.setLong(5, config.getCreatedByUser().getId());
            } else {
                ps.setNull(5, Types.BIGINT);
            }
            
            // 设置创建时间
            ps.setTimestamp(6, Timestamp.valueOf(config.getCreatedAt()));
            
            // 设置更新时间
            ps.setTimestamp(7, Timestamp.valueOf(config.getUpdatedAt()));
            
            // 设置标签提示部分标题
            ps.setString(8, config.getTagPromptsSectionHeader());
            
            // 设置问题类型部分标题
            ps.setString(9, config.getQuestionTypeSectionHeader());
            
            // 设置标签提示分隔�?
            ps.setString(10, config.getTagPromptSeparator());
            
            // 设置部分分隔�?
            ps.setString(11, config.getSectionSeparator());
            
            // 设置最终指�?
            ps.setString(12, config.getFinalInstruction());
            
            // 设置关联的变更日志ID
            if (config.getCreatedChangeLog() != null && config.getCreatedChangeLog().getId() != null) {
                ps.setLong(13, config.getCreatedChangeLog().getId());
            } else {
                ps.setNull(13, Types.BIGINT);
            }
            
            // 设置ID
            ps.setLong(14, config.getId());
            
            return ps;
        });

        return config;
    }

    /**
     * 根据ID查找配置
     *
     * @param id 配置ID
     * @return 配置对象
     */
    public Optional<AnswerPromptAssemblyConfig> findById(Long id) {
        try {
            AnswerPromptAssemblyConfig config = jdbcTemplate.queryForObject(
                SQL_FIND_BY_ID,
                new AnswerPromptAssemblyConfigRowMapper(),
                id
            );
            return Optional.ofNullable(config);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 查找所有激活状态的配置
     * 
     * @return 激活状态的配置列表
     */
    public List<AnswerPromptAssemblyConfig> findByIsActiveTrue() {
        return jdbcTemplate.query(SQL_FIND_BY_IS_ACTIVE_TRUE, new AnswerPromptAssemblyConfigRowMapper());
    }
    
    /**
     * 根据名称查找配置
     * 
     * @param name 配置名称
     * @return 匹配的配置列�?
     */
    public List<AnswerPromptAssemblyConfig> findByName(String name) {
        return jdbcTemplate.query(SQL_FIND_BY_NAME, new AnswerPromptAssemblyConfigRowMapper(), name);
    }
    
    /**
     * 根据创建者ID查找配置
     * 
     * @param userId 用户ID
     * @return 该用户创建的配置列表
     */
    public List<AnswerPromptAssemblyConfig> findByCreatedByUserId(Long userId) {
        return jdbcTemplate.query(SQL_FIND_BY_CREATED_BY_USER_ID, new AnswerPromptAssemblyConfigRowMapper(), userId);
    }
    
    /**
     * 查找所有配�?
     *
     * @return 所有配置列�?
     */
    public List<AnswerPromptAssemblyConfig> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, new AnswerPromptAssemblyConfigRowMapper());
    }
    
    /**
     * 删除配置
     *
     * @param config 配置对象
     */
    public void delete(AnswerPromptAssemblyConfig config) {
        jdbcTemplate.update(SQL_DELETE, config.getId());
    }

    /**
     * 配置行映射器
     */
    private class AnswerPromptAssemblyConfigRowMapper implements RowMapper<AnswerPromptAssemblyConfig> {
        @Override
        public AnswerPromptAssemblyConfig mapRow(ResultSet rs, int rowNum) throws SQLException {
            AnswerPromptAssemblyConfig config = new AnswerPromptAssemblyConfig();
            
            // 设置ID和基本属�?
            config.setId(rs.getLong("id"));
            config.setName(rs.getString("name"));
            config.setDescription(rs.getString("description"));
            config.setIsActive(rs.getBoolean("is_active"));
            config.setBaseSystemPrompt(rs.getString("base_system_prompt"));
            
            // 设置创建�?
            User createdByUser = null;
            Long createdByUserId = rs.getLong("created_by_user_id");
            if (!rs.wasNull()) {
                createdByUser = UserRepository.findById(createdByUserId).orElse(null);
                config.setCreatedByUser(createdByUser);
            }
            
            // 设置时间
            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                config.setCreatedAt(createdAt.toLocalDateTime());
            }
            
            Timestamp updatedAt = rs.getTimestamp("updated_at");
            if (updatedAt != null) {
                config.setUpdatedAt(updatedAt.toLocalDateTime());
            }
            
            // 设置其他属�?
            config.setTagPromptsSectionHeader(rs.getString("tag_prompts_section_header"));
            config.setQuestionTypeSectionHeader(rs.getString("question_type_section_header"));
            config.setTagPromptSeparator(rs.getString("tag_prompt_separator"));
            config.setSectionSeparator(rs.getString("section_separator"));
            config.setFinalInstruction(rs.getString("final_instruction"));
            
            // 设置关联的变更日志ID
            Long createdChangeLogId = rs.getLong("created_change_log_id");
            if (!rs.wasNull()) {
                ChangeLog changeLog = new ChangeLog();
                changeLog.setId(createdChangeLogId);
                config.setCreatedChangeLog(changeLog);
            }
            
            return config;
        }
    }
} 
