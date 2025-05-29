package com.example.demo.repository.jdbc;

import com.example.demo.entity.jdbc.ChangeLog;
import com.example.demo.entity.jdbc.DatasetQuestionMapping;
import com.example.demo.entity.jdbc.DatasetVersion;
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
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 基于JDBC的数据集问题映射仓库实现
 */
@Repository
public class DatasetQuestionMappingRepository {

    private final JdbcTemplate jdbcTemplate;
    private final DatasetVersionRepository datasetVersionRepository;
    private final StandardQuestionRepository standardQuestionRepository;
    private final UserRepository userRepository;
    private final ChangeLogRepository changeLogRepository;

    private static final String SQL_INSERT = 
            "INSERT INTO DATASET_QUESTION_MAPPING (DATASET_VERSION_ID, STANDARD_QUESTION_ID, " +
            "ORDER_IN_DATASET, CREATED_AT, CREATED_BY_USER_ID, CREATED_CHANGE_LOG_ID) " +
            "VALUES (?, ?, ?, ?, ?, ?)";
    
    private static final String SQL_UPDATE = 
            "UPDATE DATASET_QUESTION_MAPPING SET DATASET_VERSION_ID=?, STANDARD_QUESTION_ID=?, " +
            "ORDER_IN_DATASET=?, CREATED_AT=?, CREATED_BY_USER_ID=?, CREATED_CHANGE_LOG_ID=? " +
            "WHERE ID=?";
    
    private static final String SQL_FIND_BY_ID = 
            "SELECT * FROM DATASET_QUESTION_MAPPING WHERE ID=?";
    
    private static final String SQL_FIND_BY_DATASET_VERSION_ID_ORDER_BY_ORDER = 
            "SELECT * FROM DATASET_QUESTION_MAPPING WHERE DATASET_VERSION_ID=? ORDER BY ORDER_IN_DATASET";
    
    private static final String SQL_EXISTS_BY_DATASET_VERSION_ID_AND_STANDARD_QUESTION_ID = 
            "SELECT COUNT(*) FROM DATASET_QUESTION_MAPPING WHERE DATASET_VERSION_ID=? AND STANDARD_QUESTION_ID=?";
    
    private static final String SQL_COUNT_BY_DATASET_VERSION_ID = 
            "SELECT COUNT(*) FROM DATASET_QUESTION_MAPPING WHERE DATASET_VERSION_ID=?";
    
    private static final String SQL_FIND_MAX_ORDER_IN_DATASET = 
            "SELECT MAX(ORDER_IN_DATASET) FROM DATASET_QUESTION_MAPPING WHERE DATASET_VERSION_ID=?";
    
    private static final String SQL_FIND_ALL = 
            "SELECT * FROM DATASET_QUESTION_MAPPING";
    
    private static final String SQL_DELETE = 
            "DELETE FROM DATASET_QUESTION_MAPPING WHERE ID=?";

    @Autowired
    public DatasetQuestionMappingRepository(JdbcTemplate jdbcTemplate,
                                              DatasetVersionRepository datasetVersionRepository,
                                              StandardQuestionRepository standardQuestionRepository,
                                              UserRepository userRepository,
                                              ChangeLogRepository changeLogRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.datasetVersionRepository = datasetVersionRepository;
        this.standardQuestionRepository = standardQuestionRepository;
        this.userRepository = userRepository;
        this.changeLogRepository = changeLogRepository;
    }

    /**
     * 保存数据集问题映�?
     *
     * @param datasetQuestionMapping 数据集问题映射对�?
     * @return 带有ID的数据集问题映射对象
     */
    public DatasetQuestionMapping save(DatasetQuestionMapping datasetQuestionMapping) {
        if (datasetQuestionMapping.getId() == null) {
            return insert(datasetQuestionMapping);
        } else {
            return update(datasetQuestionMapping);
        }
    }

    /**
     * 插入新数据集问题映射
     *
     * @param datasetQuestionMapping 数据集问题映射对�?
     * @return 带有ID的数据集问题映射对象
     */
    private DatasetQuestionMapping insert(DatasetQuestionMapping datasetQuestionMapping) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        // 设置默认创建时间
        if (datasetQuestionMapping.getCreatedAt() == null) {
            datasetQuestionMapping.setCreatedAt(LocalDateTime.now());
        }

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            
            // 设置数据集版本ID
            ps.setLong(1, datasetQuestionMapping.getDatasetVersion().getId());
            
            // 设置标准问题ID
            ps.setLong(2, datasetQuestionMapping.getStandardQuestion().getId());
            
            // 设置数据集中的顺�?
            if (datasetQuestionMapping.getOrderInDataset() != null) {
                ps.setInt(3, datasetQuestionMapping.getOrderInDataset());
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            
            // 设置创建时间
            ps.setTimestamp(4, Timestamp.valueOf(datasetQuestionMapping.getCreatedAt()));
            
            // 设置创建者用户ID
            if (datasetQuestionMapping.getCreatedByUser() != null && datasetQuestionMapping.getCreatedByUser().getId() != null) {
                ps.setLong(5, datasetQuestionMapping.getCreatedByUser().getId());
            } else {
                ps.setNull(5, Types.BIGINT);
            }
            
            // 设置创建变更日志ID
            if (datasetQuestionMapping.getCreatedChangeLog() != null && datasetQuestionMapping.getCreatedChangeLog().getId() != null) {
                ps.setLong(6, datasetQuestionMapping.getCreatedChangeLog().getId());
            } else {
                ps.setNull(6, Types.BIGINT);
            }
            
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            datasetQuestionMapping.setId(key.longValue());
        }

        return datasetQuestionMapping;
    }

    /**
     * 更新数据集问题映�?
     *
     * @param datasetQuestionMapping 数据集问题映射对�?
     * @return 更新后的数据集问题映射对�?
     */
    private DatasetQuestionMapping update(DatasetQuestionMapping datasetQuestionMapping) {
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_UPDATE);
            
            // 设置数据集版本ID
            ps.setLong(1, datasetQuestionMapping.getDatasetVersion().getId());
            
            // 设置标准问题ID
            ps.setLong(2, datasetQuestionMapping.getStandardQuestion().getId());
            
            // 设置数据集中的顺�?
            if (datasetQuestionMapping.getOrderInDataset() != null) {
                ps.setInt(3, datasetQuestionMapping.getOrderInDataset());
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            
            // 设置创建时间
            ps.setTimestamp(4, Timestamp.valueOf(datasetQuestionMapping.getCreatedAt()));
            
            // 设置创建者用户ID
            if (datasetQuestionMapping.getCreatedByUser() != null && datasetQuestionMapping.getCreatedByUser().getId() != null) {
                ps.setLong(5, datasetQuestionMapping.getCreatedByUser().getId());
            } else {
                ps.setNull(5, Types.BIGINT);
            }
            
            // 设置创建变更日志ID
            if (datasetQuestionMapping.getCreatedChangeLog() != null && datasetQuestionMapping.getCreatedChangeLog().getId() != null) {
                ps.setLong(6, datasetQuestionMapping.getCreatedChangeLog().getId());
            } else {
                ps.setNull(6, Types.BIGINT);
            }
            
            // 设置ID
            ps.setLong(7, datasetQuestionMapping.getId());
            
            return ps;
        });

        return datasetQuestionMapping;
    }

    /**
     * 根据ID查找数据集问题映�?
     *
     * @param id 数据集问题映射ID
     * @return 数据集问题映射对�?
     */
    public Optional<DatasetQuestionMapping> findById(Long id) {
        try {
            DatasetQuestionMapping datasetQuestionMapping = jdbcTemplate.queryForObject(
                SQL_FIND_BY_ID, 
                new DatasetQuestionMappingRowMapper(), 
                id
            );
            return Optional.ofNullable(datasetQuestionMapping);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 根据数据集版本查找所有问题映射，按顺序排�?
     *
     * @param datasetVersion 数据集版本对�?
     * @return 数据集问题映射列�?
     */
    public List<DatasetQuestionMapping> findByDatasetVersionOrderByOrderInDataset(DatasetVersion datasetVersion) {
        return jdbcTemplate.query(
            SQL_FIND_BY_DATASET_VERSION_ID_ORDER_BY_ORDER, 
            new DatasetQuestionMappingRowMapper(), 
            datasetVersion.getId()
        );
    }

    /**
     * 根据数据集版本ID查找所有问题映射，按顺序排�?
     *
     * @param datasetVersionId 数据集版本ID
     * @return 数据集问题映射列�?
     */
    public List<DatasetQuestionMapping> findByDatasetVersionId(Long datasetVersionId) {
        return jdbcTemplate.query(
            SQL_FIND_BY_DATASET_VERSION_ID_ORDER_BY_ORDER, 
            new DatasetQuestionMappingRowMapper(), 
            datasetVersionId
        );
    }

    /**
     * 检查某个标准问题是否已经在指定数据集版本中
     *
     * @param datasetVersionId 数据集版本ID
     * @param standardQuestionId 标准问题ID
     * @return 是否存在
     */
    public boolean existsByDatasetVersionIdAndStandardQuestionId(Long datasetVersionId, Long standardQuestionId) {
        Integer count = jdbcTemplate.queryForObject(
            SQL_EXISTS_BY_DATASET_VERSION_ID_AND_STANDARD_QUESTION_ID, 
            Integer.class, 
            datasetVersionId, 
            standardQuestionId
        );
        return count != null && count > 0;
    }

    /**
     * 获取数据集版本中的问题数�?
     *
     * @param datasetVersionId 数据集版本ID
     * @return 问题数量
     */
    public long countByDatasetVersionId(Long datasetVersionId) {
        Long count = jdbcTemplate.queryForObject(
            SQL_COUNT_BY_DATASET_VERSION_ID, 
            Long.class, 
            datasetVersionId
        );
        return count != null ? count : 0;
    }

    /**
     * 获取数据集版本中的最大顺序号
     *
     * @param datasetVersionId 数据集版本ID
     * @return 最大顺序号
     */
    public Integer findMaxOrderInDataset(Long datasetVersionId) {
        return jdbcTemplate.queryForObject(
            SQL_FIND_MAX_ORDER_IN_DATASET, 
            Integer.class, 
            datasetVersionId
        );
    }

    /**
     * 查找所有数据集问题映射
     *
     * @return 所有数据集问题映射列表
     */
    public List<DatasetQuestionMapping> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, new DatasetQuestionMappingRowMapper());
    }

    /**
     * 删除数据集问题映�?
     *
     * @param datasetQuestionMapping 数据集问题映射对�?
     */
    public void delete(DatasetQuestionMapping datasetQuestionMapping) {
        jdbcTemplate.update(SQL_DELETE, datasetQuestionMapping.getId());
    }

    /**
     * 数据集问题映射行映射�?
     */
    private class DatasetQuestionMappingRowMapper implements RowMapper<DatasetQuestionMapping> {
        @Override
        public DatasetQuestionMapping mapRow(ResultSet rs, int rowNum) throws SQLException {
            DatasetQuestionMapping datasetQuestionMapping = new DatasetQuestionMapping();
            
            // 设置ID和基本属�?
            datasetQuestionMapping.setId(rs.getLong("ID"));
            
            // 设置数据集中的顺�?
            Integer orderInDataset = rs.getInt("ORDER_IN_DATASET");
            if (!rs.wasNull()) {
                datasetQuestionMapping.setOrderInDataset(orderInDataset);
            }
            
            // 设置创建时间
            Timestamp createdAt = rs.getTimestamp("CREATED_AT");
            if (createdAt != null) {
                datasetQuestionMapping.setCreatedAt(createdAt.toLocalDateTime());
            }
            
            // 获取并设置数据集版本
            Long datasetVersionId = rs.getLong("DATASET_VERSION_ID");
            if (!rs.wasNull()) {
                datasetVersionRepository.findById(datasetVersionId)
                    .ifPresent(datasetQuestionMapping::setDatasetVersion);
            }
            
            // 获取并设置标准问�?
            Long standardQuestionId = rs.getLong("STANDARD_QUESTION_ID");
            if (!rs.wasNull()) {
                standardQuestionRepository.findById(standardQuestionId)
                    .ifPresent(datasetQuestionMapping::setStandardQuestion);
            }
            
            // 获取并设置创建者用�?
            Long createdByUserId = rs.getLong("CREATED_BY_USER_ID");
            if (!rs.wasNull()) {
                userRepository.findById(createdByUserId)
                    .ifPresent(datasetQuestionMapping::setCreatedByUser);
            }
            
            // 获取并设置创建变更日�?
            Long createdChangeLogId = rs.getLong("CREATED_CHANGE_LOG_ID");
            if (!rs.wasNull()) {
                changeLogRepository.findById(createdChangeLogId)
                    .ifPresent(datasetQuestionMapping::setCreatedChangeLog);
            }
            
            return datasetQuestionMapping;
        }
    }
} 
