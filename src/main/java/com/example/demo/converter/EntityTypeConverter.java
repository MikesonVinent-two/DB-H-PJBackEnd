package com.example.demo.converter;

import com.example.demo.entity.EntityType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * EntityType枚举与数据库字符串之间的自动转换器
 * 用于解决数据库存储小写下划线形式而Java代码使用大写枚举的问题
 */
@Converter(autoApply = true)
public class EntityTypeConverter implements AttributeConverter<EntityType, String> {

    @Override
    public String convertToDatabaseColumn(EntityType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue(); // 使用枚举的getValue()获取小写下划线形式
    }

    @Override
    public EntityType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        // 查找匹配的枚举值
        for (EntityType type : EntityType.values()) {
            if (type.getValue().equals(dbData)) {
                return type;
            }
        }
        // 如果找不到精确匹配，尝试不区分大小写的匹配
        for (EntityType type : EntityType.values()) {
            if (type.getValue().equalsIgnoreCase(dbData) || type.name().equalsIgnoreCase(dbData)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的EntityType值: " + dbData);
    }
} 