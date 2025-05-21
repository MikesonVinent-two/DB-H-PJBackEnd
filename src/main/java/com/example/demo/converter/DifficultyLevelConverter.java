package com.example.demo.converter;

import com.example.demo.entity.DifficultyLevel;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * DifficultyLevel枚举与数据库字符串之间的自动转换器
 * 用于解决数据库存储小写枚举值而Java代码使用大写枚举的问题
 */
@Converter(autoApply = true)
public class DifficultyLevelConverter implements AttributeConverter<DifficultyLevel, String> {

    @Override
    public String convertToDatabaseColumn(DifficultyLevel attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name().toLowerCase(); // 存储为小写
    }

    @Override
    public DifficultyLevel convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return DifficultyLevel.fromString(dbData); // 使用我们新增的方法转换
    }
} 