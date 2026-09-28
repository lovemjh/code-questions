-- ============================================================
-- 内置 mock 演示数据（MERGE INTO 幂等写入，应用每次启动自动补齐）
-- 人员数据模拟“外部系统同步”，标签与绑定关系为本地维护
-- ============================================================

-- 人员（id 模拟外部系统主键）
MERGE INTO person (id, name, employee_no, department, position, email, phone, superior_id, created_at, updated_at) KEY (id) VALUES (1001, '张伟', 'E1001', '总经理办公室', '总经理', 'zhangwei@company.com', '13800001001', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
MERGE INTO person (id, name, employee_no, department, position, email, phone, superior_id, created_at, updated_at) KEY (id) VALUES (1002, '李娜', 'E1002', '研发部', '研发部经理', 'lina@company.com', '13800001002', 1001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
MERGE INTO person (id, name, employee_no, department, position, email, phone, superior_id, created_at, updated_at) KEY (id) VALUES (1003, '王强', 'E1003', '市场部', '市场部经理', 'wangqiang@company.com', '13800001003', 1001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
MERGE INTO person (id, name, employee_no, department, position, email, phone, superior_id, created_at, updated_at) KEY (id) VALUES (1004, '刘洋', 'E1004', '人事部', '人事部经理', 'liuyang@company.com', '13800001004', 1001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
MERGE INTO person (id, name, employee_no, department, position, email, phone, superior_id, created_at, updated_at) KEY (id) VALUES (1005, '陈静', 'E1005', '研发部', '后端工程师', 'chenjing@company.com', '13800001005', 1002, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
MERGE INTO person (id, name, employee_no, department, position, email, phone, superior_id, created_at, updated_at) KEY (id) VALUES (1006, '杨帆', 'E1006', '研发部', '后端工程师', 'yangfan@company.com', '13800001006', 1002, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
MERGE INTO person (id, name, employee_no, department, position, email, phone, superior_id, created_at, updated_at) KEY (id) VALUES (1007, '赵磊', 'E1007', '研发部', '前端工程师', 'zhaolei@company.com', '13800001007', 1002, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
MERGE INTO person (id, name, employee_no, department, position, email, phone, superior_id, created_at, updated_at) KEY (id) VALUES (1008, '孙丽', 'E1008', '研发部', '测试工程师', 'sunli@company.com', '13800001008', 1002, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
MERGE INTO person (id, name, employee_no, department, position, email, phone, superior_id, created_at, updated_at) KEY (id) VALUES (1009, '周涛', 'E1009', '市场部', '市场专员', 'zhoutao@company.com', '13800001009', 1003, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
MERGE INTO person (id, name, employee_no, department, position, email, phone, superior_id, created_at, updated_at) KEY (id) VALUES (1010, '吴敏', 'E1010', '市场部', '市场专员', 'wumin@company.com', '13800001010', 1003, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
MERGE INTO person (id, name, employee_no, department, position, email, phone, superior_id, created_at, updated_at) KEY (id) VALUES (1011, '郑凯', 'E1011', '人事部', '招聘专员', 'zhengkai@company.com', '13800001011', 1004, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
MERGE INTO person (id, name, employee_no, department, position, email, phone, superior_id, created_at, updated_at) KEY (id) VALUES (1012, '王芳', 'E1012', '人事部', '薪酬绩效专员', 'wangfang@company.com', '13800001012', 1004, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
MERGE INTO person (id, name, employee_no, department, position, email, phone, superior_id, created_at, updated_at) KEY (id) VALUES (1013, '冯军', 'E1013', '研发部', '架构师', 'fengjun@company.com', '13800001013', 1002, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
MERGE INTO person (id, name, employee_no, department, position, email, phone, superior_id, created_at, updated_at) KEY (id) VALUES (1014, '陈晨', 'E1014', '研发部', '前端实习生', 'chenchen@company.com', '13800001014', 1007, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
MERGE INTO person (id, name, employee_no, department, position, email, phone, superior_id, created_at, updated_at) KEY (id) VALUES (1015, '魏东', 'E1015', '市场部', '大客户经理', 'weidong@company.com', '13800001015', 1003, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 全局标签库
MERGE INTO tag (id, name, created_at, updated_at) KEY (id) VALUES (1, '核心成员', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
MERGE INTO tag (id, name, created_at, updated_at) KEY (id) VALUES (2, '高潜力', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
MERGE INTO tag (id, name, created_at, updated_at) KEY (id) VALUES (3, '技术专家', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
MERGE INTO tag (id, name, created_at, updated_at) KEY (id) VALUES (4, '待考核', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
MERGE INTO tag (id, name, created_at, updated_at) KEY (id) VALUES (5, '试用期', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
MERGE INTO tag (id, name, created_at, updated_at) KEY (id) VALUES (6, '优秀员工', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
MERGE INTO tag (id, name, created_at, updated_at) KEY (id) VALUES (7, '骨干', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
MERGE INTO tag (id, name, created_at, updated_at) KEY (id) VALUES (8, '远程办公', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 人员-标签 绑定关系
MERGE INTO person_tag (person_id, tag_id) KEY (person_id, tag_id) VALUES (1001, 1);
MERGE INTO person_tag (person_id, tag_id) KEY (person_id, tag_id) VALUES (1001, 6);
MERGE INTO person_tag (person_id, tag_id) KEY (person_id, tag_id) VALUES (1002, 1);
MERGE INTO person_tag (person_id, tag_id) KEY (person_id, tag_id) VALUES (1002, 3);
MERGE INTO person_tag (person_id, tag_id) KEY (person_id, tag_id) VALUES (1002, 7);
MERGE INTO person_tag (person_id, tag_id) KEY (person_id, tag_id) VALUES (1005, 2);
MERGE INTO person_tag (person_id, tag_id) KEY (person_id, tag_id) VALUES (1005, 3);
MERGE INTO person_tag (person_id, tag_id) KEY (person_id, tag_id) VALUES (1006, 2);
MERGE INTO person_tag (person_id, tag_id) KEY (person_id, tag_id) VALUES (1007, 5);
MERGE INTO person_tag (person_id, tag_id) KEY (person_id, tag_id) VALUES (1008, 6);
MERGE INTO person_tag (person_id, tag_id) KEY (person_id, tag_id) VALUES (1009, 4);
MERGE INTO person_tag (person_id, tag_id) KEY (person_id, tag_id) VALUES (1012, 7);
MERGE INTO person_tag (person_id, tag_id) KEY (person_id, tag_id) VALUES (1013, 3);
MERGE INTO person_tag (person_id, tag_id) KEY (person_id, tag_id) VALUES (1014, 5);
MERGE INTO person_tag (person_id, tag_id) KEY (person_id, tag_id) VALUES (1015, 6);
MERGE INTO person_tag (person_id, tag_id) KEY (person_id, tag_id) VALUES (1015, 8);
