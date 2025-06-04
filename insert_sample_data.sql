-- =============================================
-- 插入示例数据
-- =============================================

SET NAMES utf8mb4;

-- 插入用户数据
INSERT INTO `USERS` (
    `USERNAME`,
    `PASSWORD`,
    `NAME`,
    `ROLE`,
    `CONTACT_INFO`
) VALUES (
    'admin',
    '$2a$10$rJELrG6LpF1WC1UPD8kZPeYwZR0dDiXXJ5qpkWP0JKZWzKFg3ydJi',
    '系统管理员',
    'ADMIN',
    'admin@example.com'
),
(
    'expert1',
    '$2a$10$rJELrG6LpF1WC1UPD8kZPeYwZR0dDiXXJ5qpkWP0JKZWzKFg3ydJi',
    '张医生',
    'EXPERT',
    'expert1@example.com'
),
(
    'expert2',
    '$2a$10$rJELrG6LpF1WC1UPD8kZPeYwZR0dDiXXJ5qpkWP0JKZWzKFg3ydJi',
    '李医生',
    'EXPERT',
    'expert2@example.com'
),
(
    'curator1',
    '$2a$10$rJELrG6LpF1WC1UPD8kZPeYwZR0dDiXXJ5qpkWP0JKZWzKFg3ydJi',
    '王编辑',
    'CURATOR',
    'curator1@example.com'
);

-- 插入标签数据
INSERT INTO `TAGS` (
    `TAG_NAME`,
    `TAG_TYPE`,
    `DESCRIPTION`,
    `CREATED_BY_USER_ID`
) VALUES (
    '内科',
    '科室',
    '内科相关问题',
    1
),
(
    '外科',
    '科室',
    '外科相关问题',
    1
),
(
    '心脏病',
    '疾病',
    '心脏病相关问题',
    1
),
(
    '高血压',
    '疾病',
    '高血压相关问题',
    1
),
(
    '糖尿病',
    '疾病',
    '糖尿病相关问题',
    1
),
(
    '诊断',
    '类型',
    '诊断相关问题',
    1
),
(
    '治疗',
    '类型',
    '治疗相关问题',
    1
),
(
    '预防',
    '类型',
    '预防相关问题',
    1
);

-- 插入标准问题数据
INSERT INTO `STANDARD_QUESTIONS` (
    `QUESTION_TEXT`,
    `QUESTION_TYPE`,
    `DIFFICULTY`,
    `CREATED_BY_USER_ID`
) VALUES (
    '高血压的主要症状有哪些？',
    'SUBJECTIVE',
    'MEDIUM',
    1
),
(
    '糖尿病患者的日常饮食应该注意什么？',
    'SUBJECTIVE',
    'MEDIUM',
    1
),
(
    '以下哪些是冠心病的典型症状？\nA. 胸痛\nB. 气短\nC. 出汗\nD. 恶心',
    'MULTIPLE_CHOICE',
    'MEDIUM',
    1
),
(
    '正常人的空腹血糖值范围是多少？\nA. 3.9-6.1 mmol/L\nB. 7.0-8.0 mmol/L\nC. 2.0-3.5 mmol/L\nD. 8.0-10.0 mmol/L',
    'SINGLE_CHOICE',
    'EASY',
    1
),
(
    '成人正常心率范围是多少？',
    'SIMPLE_FACT',
    'EASY',
    1
),
(
    '人体正常体温是多少摄氏度？',
    'SIMPLE_FACT',
    'EASY',
    1
),
(
    '正常成人血压的理想范围是多少？',
    'SIMPLE_FACT',
    'EASY',
    1
),
(
    'BMI指数超过多少被定义为肥胖？',
    'SIMPLE_FACT',
    'EASY',
    1
),
(
    '人体中最大的器官是什么？',
    'SIMPLE_FACT',
    'EASY',
    1
);

-- 插入标准客观题答案
INSERT INTO `STANDARD_OBJECTIVE_ANSWERS` (
    `STANDARD_QUESTION_ID`,
    `OPTIONS`,
    `CORRECT_IDS`,
    `DETERMINED_BY_USER_ID`
) VALUES (
    3,
    '[{"id":"A","text":"胸痛"},{"id":"B","text":"气短"},{"id":"C","text":"出汗"},{"id":"D","text":"恶心"}]',
    '["A","B","C"]',
    2
),
(
    4,
    '[{"id":"A","text":"3.9-6.1 mmol/L"},{"id":"B","text":"7.0-8.0 mmol/L"},{"id":"C","text":"2.0-3.5 mmol/L"},{"id":"D","text":"8.0-10.0 mmol/L"}]',
    '["A"]',
    2
);

-- 插入标准主观题答案
INSERT INTO `STANDARD_SUBJECTIVE_ANSWERS` (
    `STANDARD_QUESTION_ID`,
    `ANSWER_TEXT`,
    `SCORING_GUIDANCE`,
    `DETERMINED_BY_USER_ID`
) VALUES (
    1,
    '高血压的主要症状包括：\n1. 头痛，特别是后脑部\n2. 头晕和眩晕\n3. 耳鸣\n4. 心悸\n5. 疲劳\n6. 视物模糊\n7. 失眠\n\n需要注意的是，早期高血压可能没有明显症状，因此定期测量血压很重要。',
    '评分要点：\n1. 症状的完整性（3分）\n2. 症状的准确性（4分）\n3. 补充说明的合理性（3分）',
    2
),
(
    2,
    '糖尿病患者的日常饮食注意事项：\n1. 控制总热量摄入\n2. 定时定量进餐\n3. 主食以复杂碳水化合物为主\n4. 增加膳食纤维的摄入\n5. 限制单糖和双糖的摄入\n6. 适量摄入优质蛋白\n7. 限制饱和脂肪酸的摄入\n8. 补充适量维生素和矿物质',
    '评分要点：\n1. 饮食原则的完整性（4分）\n2. 具体建议的实用性（3分）\n3. 说明的合理性（3分）',
    2
);

-- 插入标准简单题答案
INSERT INTO `STANDARD_SIMPLE_ANSWERS` (
    `STANDARD_QUESTION_ID`,
    `ANSWER_TEXT`,
    `ALTERNATIVE_ANSWERS`,
    `DETERMINED_BY_USER_ID`
) VALUES (
    5,
    '60-100次/分钟',
    '["60-100bpm", "每分钟60到100次"]',
    2
),
(
    6,
    '36.3-37.2°C',
    '["36.3-37.2度", "36.3到37.2摄氏度"]',
    2
),
(
    7,
    '120/80 mmHg',
    '["收缩压120mmHg，舒张压80mmHg", "12/8kPa"]',
    2
),
(
    8,
    '30 kg/m²',
    '["30", "BMI≥30"]',
    2
),
(
    9,
    '皮肤',
    '["表皮", "skin"]',
    2
);

-- 插入评测标准
INSERT INTO `EVALUATION_CRITERIA` (
    `NAME`,
    `DESCRIPTION`,
    `DATA_TYPE`,
    `SCORE_RANGE`,
    `APPLICABLE_QUESTION_TYPES`,
    `CREATED_BY_USER_ID`
) VALUES (
    '专业性',
    '答案在医学专业方面的准确性和规范性',
    'SCORE',
    '0-10',
    '["SUBJECTIVE", "MULTIPLE_CHOICE", "SINGLE_CHOICE"]',
    1
),
(
    '完整性',
    '答案是否完整覆盖了问题的各个方面',
    'SCORE',
    '0-10',
    '["SUBJECTIVE", "MULTIPLE_CHOICE"]',
    1
),
(
    '逻辑性',
    '答案的逻辑结构是否清晰',
    'SCORE',
    '0-10',
    '["SUBJECTIVE"]',
    1
),
(
    '实用性',
    '答案是否具有实际应用价值',
    'SCORE',
    '0-10',
    '["SUBJECTIVE", "SIMPLE_FACT"]',
    1
);

-- 插入评测者数据
INSERT INTO `EVALUATORS` (
    `NAME`,
    `EVALUATOR_TYPE`,
    `LLM_MODEL_ID`,
    `CREATED_BY_USER_ID`
) VALUES (
    'GPT-4评测器',
    'AI_MODEL',
    NULL,
    1
);

INSERT INTO `EVALUATORS` (
    `NAME`,
    `EVALUATOR_TYPE`,
    `USER_ID`,
    `CREATED_BY_USER_ID`
) VALUES (
    '专家评测组A',
    'HUMAN',
    2,
    1
);

-- 插入问题标签关联
INSERT INTO `STANDARD_QUESTION_TAGS` (
    `STANDARD_QUESTION_ID`,
    `TAG_ID`,
    `CREATED_BY_USER_ID`
) VALUES (
    1,
    4,
    1
), -- 高血压问题关联"高血压"标签
(
    1,
    6,
    1
), -- 高血压问题关联"诊断"标签
(
    2,
    5,
    1
), -- 糖尿病问题关联"糖尿病"标签
(
    2,
    7,
    1
), -- 糖尿病问题关联"治疗"标签
(
    3,
    3,
    1
), -- 冠心病问题关联"心脏病"标签
(
    3,
    6,
    1
), -- 冠心病问题关联"诊断"标签
(
    4,
    5,
    1
), -- 血糖问题关联"糖尿病"标签
(
    4,
    6,
    1
);

-- =============================================
-- 插入示例Prompt数据
-- =============================================

-- 插入回答场景的标签提示词
INSERT INTO `ANSWER_TAG_PROMPTS` (
    `TAG_ID`,
    `NAME`,
    `PROMPT_TEMPLATE`,
    `DESCRIPTION`,
    `PROMPT_PRIORITY`,
    `CREATED_BY_USER_ID`
) VALUES (
    1,
    '内科基础知识prompt',
    '作为一名内科医生，你需要：\n1. 使用准确的医学术语\n2. 解释复杂的内科疾病机制\n3. 强调疾病的系统性表现\n4. 注重药物治疗的详细说明',
    '内科回答的基础指导prompt',
    10,
    1
),
(
    2,
    '外科基础知识prompt',
    '作为一名外科医生，你需要：\n1. 详细描述手术适应症和禁忌症\n2. 解释手术方式的选择依据\n3. 说明手术风险和并发症\n4. 强调围手术期管理要点\n5. 注重手术后康复指导',
    '外科回答的基础指导prompt',
    10,
    1
),
(
    3,
    '心脏病专业prompt',
    '在回答心脏病相关问题时：\n1. 详细说明心血管系统的病理生理变化\n2. 强调症状与体征的关联性\n3. 说明心电图等检查的重要性\n4. 突出用药注意事项和禁忌症',
    '心脏病问题的专业指导prompt',
    20,
    1
),
(
    4,
    '高血压专业prompt',
    '回答高血压相关问题时：\n1. 强调血压值的正常范围和异常标准\n2. 详细说明生活方式的影响\n3. 解释各类降压药物的作用机制\n4. 说明并发症的预防措施',
    '高血压问题的专业指导prompt',
    20,
    1
),
(
    5,
    '糖尿病专业prompt',
    '回答糖尿病相关问题时：\n1. 明确血糖控制目标范围\n2. 强调血糖监测的重要性\n3. 详细说明饮食和运动管理\n4. 解释降糖药物选择原则\n5. 说明常见并发症的预防和处理',
    '糖尿病问题的专业指导prompt',
    20,
    1
),
(
    6,
    '诊断类prompt',
    '对于诊断类问题：\n1. 系统性列举症状和体征\n2. 说明必要的检查项目\n3. 解释鉴别诊断要点\n4. 强调诊断的金标准',
    '诊断类问题的通用prompt',
    30,
    1
),
(
    7,
    '治疗类prompt',
    '对于治疗类问题：\n1. 按照循证医学证据等级排序治疗方案\n2. 详细说明用药方案和注意事项\n3. 解释非药物治疗的重要性\n4. 说明治疗效果评估方法',
    '治疗类问题的通用prompt',
    30,
    1
),
(
    8,
    '预防类prompt',
    '对于预防类问题：\n1. 区分一级预防和二级预防措施\n2. 强调生活方式干预的具体方法\n3. 说明预防保健的关键时间点\n4. 解释风险因素的控制方法\n5. 提供可操作的预防建议',
    '预防类问题的通用prompt',
    30,
    1
);

-- 插入回答场景的题型提示词
INSERT INTO `ANSWER_QUESTION_TYPE_PROMPTS` (
    `NAME`,
    `QUESTION_TYPE`,
    `PROMPT_TEMPLATE`,
    `RESPONSE_FORMAT_INSTRUCTION`,
    `RESPONSE_EXAMPLE`,
    `CREATED_BY_USER_ID`
) VALUES (
    '单选题回答prompt',
    'SINGLE_CHOICE',
    '请仔细分析每个选项，选择最准确的一个答案。只需要给出选项，务必别给出分析或者回答格式以外的字',
    '回答格式：\n[选项字母]\n',
    'A\n',
    1
),
(
    '多选题回答prompt',
    'MULTIPLE_CHOICE',
    '请仔细分析所有选项，选择所有正确的答案。只需要给出选项，务必别给出分析或者回答格式以外的字',
    '回答格式：\n[选项字母]\n',
    'A,B,C\n',
    1
),
(
    '简单事实题回答prompt',
    'SIMPLE_FACT',
    '请提供简洁、准确的事实回答。只需要给出答案，务必别给出分析或者回答格式以外的字',
    '回答格式：\n答案：[核心事实]\n',
    '正常成人心率为60-100次/分\n',
    1
),
(
    '主观题回答prompt',
    'SUBJECTIVE',
    '请提供详细、系统的回答。\n需要：\n1. 结构化组织内容\n2. 使用专业准确的医学术语\n3. 提供具体的例子或解释\n4. 注意回答的完整性和逻辑性',
    '回答格式：\n[主要观点]\n1. [要点1]\n2. [要点2]\n...\n补充说明：[其他重要信息]',
    '高血压的主要症状：\n1. 头痛（特别是后枕部）\n2. 头晕\n3. 视物模糊\n补充说明：部分患者可能无明显症状',
    1
);

-- 插入评测场景的标签提示词
INSERT INTO `EVALUATION_TAG_PROMPTS` (
    `TAG_ID`,
    `NAME`,
    `PROMPT_TEMPLATE`,
    `PROMPT_PRIORITY`,
    `CREATED_BY_USER_ID`
) VALUES (
    1,
    '内科评测prompt',
    '评估内科问题回答时，请注意：\n1. 医学术语使用的准确性（0-10分）\n2. 病理生理机制解释的深度（0-10分）\n3. 治疗方案的规范性（0-10分）\n4. 整体专业水平（0-10分）',
    10,
    1
),
(
    3,
    '心脏病评测prompt',
    '评估心脏病相关回答时，重点关注：\n1. 心血管病理生理的解释准确性（0-10分）\n2. 症状体征描述的完整性（0-10分）\n3. 检查方法推荐的合理性（0-10分）\n4. 治疗方案的循证医学支持（0-10分）',
    20,
    1
),
(
    6,
    '诊断评测prompt',
    '评估诊断相关回答时，请考虑：\n1. 诊断思路的清晰度（0-10分）\n2. 鉴别诊断的完整性（0-10分）\n3. 检查建议的必要性（0-10分）\n4. 诊断依据的循证等级（0-10分）',
    30,
    1
);

-- 插入评测场景的主观题提示词
INSERT INTO `EVALUATION_SUBJECTIVE_PROMPTS` (
    `NAME`,
    `PROMPT_TEMPLATE`,
    `SCORING_INSTRUCTION`,
    `OUTPUT_FORMAT_INSTRUCTION`,
    `CREATED_BY_USER_ID`
) VALUES (
    '标准主观题评测prompt',
    '请对以下回答进行全面评估：\n1. 专业性（医学术语、概念准确性）\n2. 完整性（要点覆盖程度）\n3. 逻辑性（条理性、结构性）\n4. 实用性（临床应用价值）',
    '评分标准：\n专业性（0-25分）：\n- 术语准确：0-10分\n- 概念清晰：0-15分\n\n完整性（0-25分）：\n- 核心要点：0-15分\n- 补充信息：0-10分\n\n逻辑性（0-25分）：\n- 结构完整：0-10分\n- 条理清晰：0-15分\n\n实用性（0-25分）：\n- 临床相关：0-15分\n- 可操作性：0-10分',
    '请按以下格式输出评分：\n{\n  "专业性": {\n    "分数": X,\n    "评语": "..."\n  },\n  "完整性": {\n    "分数": X,\n    "评语": "..."\n  },\n  "逻辑性": {\n    "分数": X,\n    "评语": "..."\n  },\n  "实用性": {\n    "分数": X,\n    "评语": "..."\n  },\n  "总分": X,\n  "总评": "..."\n}',
    1
);

-- 插入prompt组装配置
INSERT INTO `ANSWER_PROMPT_ASSEMBLY_CONFIGS` (
    `NAME`,
    `DESCRIPTION`,
    `BASE_SYSTEM_PROMPT`,
    `TAG_PROMPTS_SECTION_HEADER`,
    `QUESTION_TYPE_SECTION_HEADER`,
    `FINAL_INSTRUCTION`,
    `CREATED_BY_USER_ID`
) VALUES (
    '标准医学回答配置',
    '用于医学问题回答的标准prompt组装配置',
    '你是一个专业的医学AI助手，请基于循证医学和最新指南提供准确、专业的回答。',
    '## 专业知识要求',
    '## 回答要求',
    '请严格按照上述要求进行回答，确保专业性和准确性。',
    1
);

INSERT INTO `EVALUATION_PROMPT_ASSEMBLY_CONFIGS` (
    `NAME`,
    `DESCRIPTION`,
    `BASE_SYSTEM_PROMPT`,
    `TAG_PROMPTS_SECTION_HEADER`,
    `SUBJECTIVE_SECTION_HEADER`,
    `FINAL_INSTRUCTION`,
    `CREATED_BY_USER_ID`
) VALUES (
    '标准医学评测配置',
    '用于医学问题评测的标准prompt组装配置',
    '你是一个专业的医学评测专家，请基于专业知识对回答进行客观、公正的评估。',
    '## 专业评测标准',
    '## 评分要求',
    '请严格按照评分标准进行评估，给出详细的评分依据和建议。',
    1
);

-- 插入数据集版本和映射关系
INSERT INTO `DATASET_VERSIONS` (
    `VERSION_NUMBER`,
    `NAME`,
    `DESCRIPTION`,
    `CREATED_BY_USER_ID`
) VALUES (
    'v1.0.0',
    '医学问答基础数据集',
    '包含基础医学问题的初始数据集',
    1
);

-- 插入数据集-问题映射关系
INSERT INTO `DATASET_QUESTION_MAPPING` (
    `DATASET_VERSION_ID`,
    `STANDARD_QUESTION_ID`,
    `ORDER_IN_DATASET`,
    `CREATED_BY_USER_ID`
) VALUES 
(1, 1, 1, 1),
(1, 2, 2, 1),
(1, 3, 3, 1),
(1, 4, 4, 1),
(1, 5, 5, 1),
(1, 6, 6, 1),
(1, 7, 7, 1),
(1, 8, 8, 1),
(1, 9, 9, 1); 