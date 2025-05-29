package com.example.demo.converter;

import com.example.demo.entity.jdbc.ChangeType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * ChangeType枚举与数据库字符串之间的自动转换器
 * 用于解决数据库存储小写枚举值而Java代码使用大写枚举的问题
 */
@Converter(autoApply = true)
public class ChangeTypeConverter implements AttributeConverter<ChangeType, String> {

    @Override
    public String convertToDatabaseColumn(ChangeType attribute) {
        if (attribute == null) {
            return null;
        }
        // 可以选择存储enum的name()（大写加下划线）或getValue()（中文描述）
        // 这里选择存储name()的小写形式，与之前的实现保持一致
        return attribute.name().toLowerCase();
    }

    @Override
    public ChangeType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return ChangeType.fromString(dbData); // 使用我们新增的方法转换
    }
} 