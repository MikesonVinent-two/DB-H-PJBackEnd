package com.example.demo.entity;

public enum QuestionType {
    SINGLE_CHOICE,    // 单选题
    MULTIPLE_CHOICE,  // 多选题
    SIMPLE_FACT,     // 简单事实题
    SUBJECTIVE;      // 主观题
    
    /**
     * 根据字符串查找对应的问题类型，忽略大小写
     * @param value 问题类型字符串
     * @return 对应的问题类型枚举值，如果找不到则返回null
     */
    public static QuestionType fromString(String value) {
        if (value == null) {
            return null;
        }
        
        try {
            return QuestionType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            for (QuestionType type : QuestionType.values()) {
                if (type.name().equalsIgnoreCase(value)) {
                    return type;
                }
            }
            // 处理特殊情况，如下划线与横杠的替换
            if (value.contains("_")) {
                return fromString(value.replace("_", "-"));
            } else if (value.contains("-")) {
                return fromString(value.replace("-", "_"));
            }
            return null;
        }
    }
} 