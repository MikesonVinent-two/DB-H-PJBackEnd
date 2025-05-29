package com.example.demo.repository.jdbc;

import com.example.demo.entity.jdbc.ChangeLog;
import com.example.demo.entity.jdbc.ChangeLogDetail;
import com.example.demo.entity.jdbc.EntityType;
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
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

/**
 * 基于JDBC的变更日志详情仓库实�?
 */
@Repository
public class ChangeLogDetailRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String SQL_INSERT = 
            "INSERT INTO change_log_details (change_log_id, entity_type, entity_id, attribute_name, old_value, new_value) " +
            "VALUES (?, ?, ?, ?, ?, ?)";
    
    private static final String SQL_UPDATE = 
            "UPDATE change_log_details SET change_log_id=?, entity_type=?, entity_id=?, attribute_name=?, old_value=?, new_value=? " +
            "WHERE id=?";
    
    private static final String SQL_FIND_BY_ID = 
            "SELECT * FROM change_log_details WHERE id=?";
    
    private static final String SQL_FIND_BY_CHANGE_LOG = 
            "SELECT * FROM change_log_details WHERE change_log_id=?";
    
    private static final String SQL_FIND_ALL = 
            "SELECT * FROM change_log_details";
    
    private static final String SQL_DELETE = 
            "DELETE FROM change_log_details WHERE id=?";

    @Autowired
    public ChangeLogDetailRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 保存变更日志详情
     *
     * @param changeLogDetail 变更日志详情对象
     * @return 带有ID的变更日志详情对�?
     */
    public ChangeLogDetail save(ChangeLogDetail changeLogDetail) {
        if (getId(changeLogDetail) == null) {
            return insert(changeLogDetail);
        } else {
            return update(changeLogDetail);
        }
    }

    /**
     * 插入新变更日志详�?
     *
     * @param changeLogDetail 变更日志详情对象
     * @return 带有ID的变更日志详情对�?
     */
    private ChangeLogDetail insert(ChangeLogDetail changeLogDetail) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            
            if (changeLogDetail.getChangeLog() != null && getId(changeLogDetail.getChangeLog()) != null) {
                ps.setLong(1, getId(changeLogDetail.getChangeLog()));
            } else {
                ps.setNull(1, java.sql.Types.BIGINT);
            }
            
            ps.setString(2, changeLogDetail.getEntityType().name());
            ps.setLong(3, changeLogDetail.getEntityId());
            ps.setString(4, changeLogDetail.getAttributeName());
            ps.setString(5, changeLogDetail.getOldValue());
            ps.setString(6, changeLogDetail.getNewValue());
            
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            setId(changeLogDetail, key.longValue());
        }
        return changeLogDetail;
    }

    /**
     * 更新变更日志详情
     *
     * @param changeLogDetail 变更日志详情对象
     * @return 更新后的变更日志详情对象
     */
    private ChangeLogDetail update(ChangeLogDetail changeLogDetail) {
        jdbcTemplate.update(SQL_UPDATE,
                getId(changeLogDetail.getChangeLog()),
                changeLogDetail.getEntityType().name(),
                changeLogDetail.getEntityId(),
                changeLogDetail.getAttributeName(),
                changeLogDetail.getOldValue(),
                changeLogDetail.getNewValue(),
                getId(changeLogDetail));

        return changeLogDetail;
    }

    /**
     * 根据ID查找变更日志详情
     *
     * @param id 变更日志详情ID
     * @return 变更日志详情的Optional包装
     */
    public Optional<ChangeLogDetail> findById(Long id) {
        try {
            ChangeLogDetail changeLogDetail = jdbcTemplate.queryForObject(SQL_FIND_BY_ID, new Object[]{id}, new ChangeLogDetailRowMapper());
            return Optional.ofNullable(changeLogDetail);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
    
    /**
     * 根据变更日志查找所有详�?
     *
     * @param changeLog 变更日志对象
     * @return 变更日志详情列表
     */
    public List<ChangeLogDetail> findByChangeLog(ChangeLog changeLog) {
        return jdbcTemplate.query(SQL_FIND_BY_CHANGE_LOG, new Object[]{getId(changeLog)}, new ChangeLogDetailRowMapper());
    }

    /**
     * 查找所有变更日志详�?
     *
     * @return 变更日志详情列表
     */
    public List<ChangeLogDetail> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, new ChangeLogDetailRowMapper());
    }

    /**
     * 删除变更日志详情
     *
     * @param id 变更日志详情ID
     * @return 是否成功
     */
    public boolean delete(Long id) {
        int affected = jdbcTemplate.update(SQL_DELETE, id);
        return affected > 0;
    }

    /**
     * 变更日志详情行映射器
     */
    private class ChangeLogDetailRowMapper implements RowMapper<ChangeLogDetail> {
        @Override
        public ChangeLogDetail mapRow(ResultSet rs, int rowNum) throws SQLException {
            ChangeLogDetail detail = new ChangeLogDetail();
            setId(detail, rs.getLong("id"));
            
            // 解析枚举
            String entityTypeStr = rs.getString("entity_type");
            if (entityTypeStr != null) {
                detail.setEntityType(EntityType.valueOf(entityTypeStr));
            }
            
            detail.setEntityId(rs.getLong("entity_id"));
            detail.setAttributeName(rs.getString("attribute_name"));
            detail.setOldValue(rs.getString("old_value"));
            detail.setNewValue(rs.getString("new_value"));
            
            // 处理外键关联 - 这里简单地创建一个只有ID的ChangeLog对象
            Long changeLogId = rs.getLong("change_log_id");
            if (!rs.wasNull()) {
                ChangeLog changeLog = new ChangeLog();
                setId(changeLog, changeLogId);
                detail.setChangeLog(changeLog);
            }
            
            return detail;
        }
    }
    
    /**
     * 通过反射获取id字段的�?
     */
    private Long getId(Object entity) {
        try {
            Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            return (Long) idField.get(entity);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 通过反射设置id字段的�?
     */
    private void setId(Object entity, Long id) {
        try {
            Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            // 忽略异常
        }
    }
} 
