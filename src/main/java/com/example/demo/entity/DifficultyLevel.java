package com.example.demo.entity;
 
public enum DifficultyLevel {
    EASY,    // 简单
    MEDIUM,  // 中等
    HARD;    // 困难
    
    /**
     * 根据字符串查找对应的难度级别，忽略大小写
     * @param value 难度级别字符串
     * @return 对应的难度级别枚举值，如果找不到则返回null
     */
    public static DifficultyLevel fromString(String value) {
        if (value == null) {
            return null;
        }
        
        try {
            return DifficultyLevel.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            for (DifficultyLevel level : DifficultyLevel.values()) {
                if (level.name().equalsIgnoreCase(value)) {
                    return level;
                }
            }
            return null;
        }
    }
} 