# 评分标准表(EVALUATION_CRITERIA)深度分析

## 1. 表结构概述

评分标准表是整个评估系统的核心组件，定义了对LLM回答进行评估的具体标准和方法。

### 核心字段分析

| 字段名 | 类型 | 作用 | 示例 |
|--------|------|------|------|
| `NAME` | VARCHAR(255) | 标准名称 | "专业性"、"完整性"、"逻辑性" |
| `VERSION` | VARCHAR(255) | 版本控制 | "v1.0"、"v2.1" |
| `DESCRIPTION` | TEXT | 详细描述 | "答案在医学专业方面的准确性和规范性" |
| `DATA_TYPE` | ENUM | 数据类型 | SCORE/BOOLEAN/TEXT/CATEGORICAL |
| `SCORE_RANGE` | VARCHAR(255) | 分值范围 | "0-10"、"1-5" |
| `APPLICABLE_QUESTION_TYPES` | JSON | 适用题型 | ["SUBJECTIVE", "MULTIPLE_CHOICE"] |
| `IS_REQUIRED` | BOOLEAN | 是否必选 | TRUE/FALSE |
| `ORDER_INDEX` | INT | 排序顺序 | 1, 2, 3... |
| `WEIGHT` | DECIMAL(5,2) | 评分权重 | 1.0, 1.5, 2.0 |
| `QUESTION_TYPE` | ENUM | 特定题型 | SINGLE_CHOICE/MULTIPLE_CHOICE等 |
| `OPTIONS` | JSON | 分类选项 | 用于CATEGORICAL类型 |

## 2. 数据类型详解

### 2.1 SCORE类型（分数评分）
- **用途**: 最常用的评分方式，给出具体分数
- **示例**: 专业性评分0-10分
- **应用场景**: 主观题的多维度评估

### 2.2 BOOLEAN类型（布尔判断）
- **用途**: 是/否的二元判断
- **示例**: 答案是否包含关键信息
- **应用场景**: 客观题的正确性判断

### 2.3 TEXT类型（文本评价）
- **用途**: 提供详细的文字评语
- **示例**: "回答逻辑清晰，但缺少具体案例"
- **应用场景**: 定性分析和改进建议

### 2.4 CATEGORICAL类型（分类评价）
- **用途**: 从预定义选项中选择
- **示例**: "优秀/良好/一般/较差"
- **应用场景**: 等级制评估

## 3. 在代码中的核心使用场景

### 3.1 评估提示词生成

```java
// EvaluationServiceImpl.java:463-470
promptBuilder.append("评测标准：\n");
for (EvaluationCriterion criterion : criteria) {
    promptBuilder.append("- ").append(criterion.getName()).append("：")
          .append(criterion.getDescription()).append("\n");
}
```

**作用**: 将评分标准动态嵌入到AI评估的提示词中，指导AI按照特定标准进行评估。

### 3.2 评估结果解析和存储

```java
// EvaluationServiceImpl.java:1111-1140
for (Map<String, Object> scoreMap : scoresList) {
    EvaluationDetail detail = new EvaluationDetail();
    detail.setEvaluation(evaluation);
    
    // 设置评测标准名称
    Object criterionObj = scoreMap.get("criterion");
    if (criterionObj != null) {
        detail.setCriterionName(criterionObj.toString());
    }
    
    // 设置分数
    Object scoreObj = scoreMap.get("score");
    if (scoreObj instanceof Number) {
        detail.setScore(new BigDecimal(scoreObj.toString()));
    }
    
    // 设置评语
    Object commentsObj = scoreMap.get("comments");
    if (commentsObj != null) {
        detail.setComments(commentsObj.toString());
    }
    
    details.add(detail);
}
```

**作用**: 解析AI返回的评估结果，按照评分标准将结果存储到评估详情表中。

### 3.3 按题型获取适用标准

```java
// EvaluationServiceImpl.java:1155-1162
public List<EvaluationCriterion> getCriteriaForQuestionType(QuestionType questionType) {
    return evaluationCriterionRepository.findByQuestionType(questionType);
}

public List<EvaluationCriterion> getCriteriaForQuestionType(QuestionType questionType, int page, int size) {
    return evaluationCriterionRepository.findActiveByQuestionTypeOrderByOrderIndexPaged(questionType, page, size);
}
```

**作用**: 根据问题类型动态获取适用的评分标准，确保评估的针对性。

### 3.4 批量评估中的标准应用

```java
// EvaluationServiceImpl.java:3630-3668
final List<EvaluationCriterion> criteria;
if (criteriaIds != null && !criteriaIds.isEmpty()) {
    // 使用指定的评测标准
    String criteriaQuery = "SELECT * FROM EVALUATION_CRITERIA WHERE ID IN (" + idPlaceholders + ")";
    List<Map<String, Object>> criteriaData = jdbcTemplate.queryForList(criteriaQuery, criteriaIds.toArray());
    
    criteria = new ArrayList<>();
    for (Map<String, Object> data : criteriaData) {
        EvaluationCriterion criterion = new EvaluationCriterion();
        criterion.setId((Long) data.get("ID"));
        criterion.setName((String) data.get("NAME"));
        criterion.setDescription((String) data.get("DESCRIPTION"));
        // ... 设置其他属性
        criteria.add(criterion);
    }
} else {
    // 使用默认的主观题评测标准
    criteria = getCriteriaForQuestionType(QuestionType.SUBJECTIVE);
}
```

**作用**: 在批量评估中，支持用户自定义选择评分标准或使用默认标准。

## 4. 评分标准的层次结构

### 4.1 版本控制机制
- **父子关系**: `PARENT_CRITERION_ID`字段实现版本继承
- **变更追踪**: `CREATED_CHANGE_LOG_ID`记录每次修改
- **软删除**: `DELETED_AT`字段支持逻辑删除

### 4.2 权重系统
- **权重计算**: `WEIGHT`字段定义各标准的重要程度
- **加权平均**: 最终分数 = Σ(标准分数 × 权重) / Σ权重
- **灵活调整**: 可根据评估需求调整权重分配

## 5. 实际应用示例

### 5.1 示例数据分析

从`insert_sample_data.sql`中可以看到系统预设了4个基础评分标准：

```sql
INSERT INTO EVALUATION_CRITERIA (NAME, DESCRIPTION, DATA_TYPE, SCORE_RANGE, APPLICABLE_QUESTION_TYPES, CREATED_BY_USER_ID) VALUES
('专业性', '答案在医学专业方面的准确性和规范性', 'SCORE', '0-10', '["SUBJECTIVE", "MULTIPLE_CHOICE", "SINGLE_CHOICE"]', 1),
('完整性', '答案是否完整覆盖了问题的各个方面', 'SCORE', '0-10', '["SUBJECTIVE", "MULTIPLE_CHOICE"]', 1),
('逻辑性', '答案的逻辑结构是否清晰', 'SCORE', '0-10', '["SUBJECTIVE"]', 1),
('实用性', '答案是否具有实际应用价值', 'SCORE', '0-10', '["SUBJECTIVE", "SIMPLE_FACT"]', 1);
```

### 5.2 评估流程示例

1. **标准选择**: 根据问题类型自动选择或用户指定评分标准
2. **提示词生成**: 将标准描述嵌入AI评估提示词
3. **AI评估**: AI根据标准给出分数和评语
4. **结果解析**: 系统解析AI返回的JSON格式结果
5. **数据存储**: 将评估结果存储到`EVALUATION_DETAILS`表

### 5.3 JSON格式示例

AI返回的评估结果格式：
```json
{
  "总分": 8.5,
  "criteria_scores": [
    {
      "criterion": "专业性",
      "score": 9,
      "comments": "医学术语使用准确，概念表述专业"
    },
    {
      "criterion": "完整性", 
      "score": 8,
      "comments": "覆盖了主要要点，但缺少部分细节"
    },
    {
      "criterion": "逻辑性",
      "score": 9,
      "comments": "结构清晰，条理分明"
    },
    {
      "criterion": "实用性",
      "score": 8,
      "comments": "具有较好的临床指导价值"
    }
  ],
  "overall_comments": "整体回答质量较高，专业性强",
  "improvement_suggestions": "建议补充更多具体案例"
}
```

## 6. 系统设计优势

### 6.1 灵活性
- **多数据类型**: 支持分数、布尔、文本、分类四种评估方式
- **题型适配**: 不同题型可使用不同的评分标准
- **权重调整**: 可根据需求调整各标准的重要性

### 6.2 可扩展性
- **版本控制**: 支持评分标准的迭代升级
- **动态配置**: 可在运行时添加新的评分标准
- **批量应用**: 支持批量评估中的标准选择

### 6.3 一致性
- **标准化评估**: 确保所有评估使用统一标准
- **可追溯性**: 完整记录评估过程和依据
- **质量保证**: 通过标准化提高评估质量

## 7. 与其他表的关联关系

### 7.1 核心关联
- **EVALUATION_DETAILS**: 存储具体的评分结果
- **EVALUATIONS**: 关联到具体的评估记录
- **CHANGE_LOG**: 记录标准的变更历史
- **USERS**: 记录标准的创建者

### 7.2 数据流向
```
EVALUATION_CRITERIA → 生成评估提示词 → AI评估 → EVALUATIONS → EVALUATION_DETAILS
```

## 8. 最佳实践建议

### 8.1 标准设计
- **明确性**: 标准描述应清晰明确，避免歧义
- **可操作性**: 标准应具有可操作的评估指标
- **完整性**: 标准体系应覆盖评估的各个维度

### 8.2 权重配置
- **平衡性**: 避免某个标准权重过高
- **针对性**: 根据问题类型调整权重分配
- **动态性**: 定期评估和调整权重设置

### 8.3 版本管理
- **渐进式**: 新版本应向后兼容
- **文档化**: 详细记录版本变更原因
- **测试验证**: 新标准应经过充分测试

## 9. 总结

评分标准表是整个LLM评估系统的核心，它通过标准化的评估维度和灵活的配置机制，实现了对AI回答质量的客观、全面、可追溯的评估。其设计充分考虑了医学问答的专业性要求，支持多种评估方式和动态配置，为构建高质量的AI评测平台提供了坚实的基础。 