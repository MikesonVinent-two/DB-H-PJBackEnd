package com.example.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

/**
 * JDBC配置类
 */
@Configuration
public class JdbcConfig {

    private final DataSource dataSource;

    @Autowired
    public JdbcConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 配置JdbcTemplate
     */
    @Bean
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource);
    }

    /**
     * 配置NamedParameterJdbcTemplate
     * 支持命名参数，而不是位置参数（如?）
     */
    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate() {
        return new NamedParameterJdbcTemplate(dataSource);
    }
} 