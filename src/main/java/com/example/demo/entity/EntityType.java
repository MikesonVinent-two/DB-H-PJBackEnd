package com.example.demo.entity;

/**
 * 可追踪变更的实体类型枚举
 */
public enum EntityType {
    STANDARD_QUESTION("standard_question"),
    STD_OBJECTIVE_ANSWER("std_objective_answer"),
    STD_SIMPLE_ANSWER("std_simple_answer"),
    STD_SUBJECTIVE_ANSWER("std_subjective_answer"),
    STANDARD_OBJECTIVE_ANSWER("standard_objective_answer"),
    STANDARD_SIMPLE_ANSWER("standard_simple_answer"),
    STANDARD_SUBJECTIVE_ANSWER("standard_subjective_answer"),
    CHECKLIST_ITEM("checklist_item"),
    EVAL_CRITERION("eval_criterion"),
    AI_PROMPT("ai_prompt"),
    TAG("tag"),
    DATASET_VERSION("dataset_version"),
    LLM_MODEL("llm_model"),
    EVALUATOR("evaluator"),
    STANDARD_QUESTION_TAGS("standard_question_tags"),
    DATASET_QUESTION_MAPPING("dataset_question_mapping"),
    AI_PROMPT_TAGS("ai_prompt_tags");

    private final String value;

    EntityType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
    
    /**
     * 根据字符串查找对应的实体类型，不区分大小写
     * @param value 实体类型字符串
     * @return 对应的实体类型枚举值，如果找不到则返回null
     */
    public static EntityType fromString(String value) {
        if (value == null) {
            return null;
        }
        
        // 尝试精确匹配getValue值
        for (EntityType type : EntityType.values()) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }
        
        // 尝试不区分大小写匹配
        for (EntityType type : EntityType.values()) {
            if (type.getValue().equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
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