-- ============================================================
-- 内置 mock 演示数据（幂等写入：仅当记录不存在时插入，不覆盖运行期的修改）
-- 人员数据模拟“外部系统同步”，标签与绑定关系为本地维护
-- ============================================================

-- 人员（id 模拟外部系统主键；superior_id 构成 4 级组织架构）
INSERT INTO person (id, name, employee_no, department, position, email, phone, superior_id)
SELECT 1001, '张伟', 'E1001', '总经理办公室', '总经理', 'zhangwei@company.com', '13800001001', NULL WHERE NOT EXISTS (SELECT 1 FROM person WHERE id = 1001);
INSERT INTO person (id, name, employee_no, department, position, email, phone, superior_id)
SELECT 1002, '李娜', 'E1002', '研发部', '研发部经理', 'lina@company.com', '13800001002', 1001 WHERE NOT EXISTS (SELECT 1 FROM person WHERE id = 1002);
INSERT INTO person (id, name, employee_no, department, position, email, phone, superior_id)
SELECT 1003, '王强', 'E1003', '市场部', '市场部经理', 'wangqiang@company.com', '13800001003', 1001 WHERE NOT EXISTS (SELECT 1 FROM person WHERE id = 1003);
INSERT INTO person (id, name, employee_no, department, position, email, phone, superior_id)
SELECT 1004, '刘洋', 'E1004', '人事部', '人事部经理', 'liuyang@company.com', '13800001004', 1001 WHERE NOT EXISTS (SELECT 1 FROM person WHERE id = 1004);
INSERT INTO person (id, name, employee_no, department, position, email, phone, superior_id)
SELECT 1005, '陈静', 'E1005', '研发部', '后端工程师', 'chenjing@company.com', '13800001005', 1002 WHERE NOT EXISTS (SELECT 1 FROM person WHERE id = 1005);
INSERT INTO person (id, name, employee_no, department, position, email, phone, superior_id)
SELECT 1006, '杨帆', 'E1006', '研发部', '后端工程师', 'yangfan@company.com', '13800001006', 1002 WHERE NOT EXISTS (SELECT 1 FROM person WHERE id = 1006);
INSERT INTO person (id, name, employee_no, department, position, email, phone, superior_id)
SELECT 1007, '赵磊', 'E1007', '研发部', '前端工程师', 'zhaolei@company.com', '13800001007', 1002 WHERE NOT EXISTS (SELECT 1 FROM person WHERE id = 1007);
INSERT INTO person (id, name, employee_no, department, position, email, phone, superior_id)
SELECT 1008, '孙丽', 'E1008', '研发部', '测试工程师', 'sunli@company.com', '13800001008', 1002 WHERE NOT EXISTS (SELECT 1 FROM person WHERE id = 1008);
INSERT INTO person (id, name, employee_no, department, position, email, phone, superior_id)
SELECT 1009, '周涛', 'E1009', '市场部', '市场专员', 'zhoutao@company.com', '13800001009', 1003 WHERE NOT EXISTS (SELECT 1 FROM person WHERE id = 1009);
INSERT INTO person (id, name, employee_no, department, position, email, phone, superior_id)
SELECT 1010, '吴敏', 'E1010', '市场部', '市场专员', 'wumin@company.com', '13800001010', 1003 WHERE NOT EXISTS (SELECT 1 FROM person WHERE id = 1010);
INSERT INTO person (id, name, employee_no, department, position, email, phone, superior_id)
SELECT 1011, '郑凯', 'E1011', '人事部', '招聘专员', 'zhengkai@company.com', '13800001011', 1004 WHERE NOT EXISTS (SELECT 1 FROM person WHERE id = 1011);
INSERT INTO person (id, name, employee_no, department, position, email, phone, superior_id)
SELECT 1012, '王芳', 'E1012', '人事部', '薪酬绩效专员', 'wangfang@company.com', '13800001012', 1004 WHERE NOT EXISTS (SELECT 1 FROM person WHERE id = 1012);
INSERT INTO person (id, name, employee_no, department, position, email, phone, superior_id)
SELECT 1013, '冯军', 'E1013', '研发部', '架构师', 'fengjun@company.com', '13800001013', 1002 WHERE NOT EXISTS (SELECT 1 FROM person WHERE id = 1013);
INSERT INTO person (id, name, employee_no, department, position, email, phone, superior_id)
SELECT 1014, '陈晨', 'E1014', '研发部', '前端实习生', 'chenchen@company.com', '13800001014', 1007 WHERE NOT EXISTS (SELECT 1 FROM person WHERE id = 1014);
INSERT INTO person (id, name, employee_no, department, position, email, phone, superior_id)
SELECT 1015, '魏东', 'E1015', '市场部', '大客户经理', 'weidong@company.com', '13800001015', 1003 WHERE NOT EXISTS (SELECT 1 FROM person WHERE id = 1015);

-- 全局标签库
INSERT INTO tag (id, name)
SELECT 1, '核心成员' WHERE NOT EXISTS (SELECT 1 FROM tag WHERE id = 1);
INSERT INTO tag (id, name)
SELECT 2, '高潜力' WHERE NOT EXISTS (SELECT 1 FROM tag WHERE id = 2);
INSERT INTO tag (id, name)
SELECT 3, '技术专家' WHERE NOT EXISTS (SELECT 1 FROM tag WHERE id = 3);
INSERT INTO tag (id, name)
SELECT 4, '待考核' WHERE NOT EXISTS (SELECT 1 FROM tag WHERE id = 4);
INSERT INTO tag (id, name)
SELECT 5, '试用期' WHERE NOT EXISTS (SELECT 1 FROM tag WHERE id = 5);
INSERT INTO tag (id, name)
SELECT 6, '优秀员工' WHERE NOT EXISTS (SELECT 1 FROM tag WHERE id = 6);
INSERT INTO tag (id, name)
SELECT 7, '骨干' WHERE NOT EXISTS (SELECT 1 FROM tag WHERE id = 7);
INSERT INTO tag (id, name)
SELECT 8, '远程办公' WHERE NOT EXISTS (SELECT 1 FROM tag WHERE id = 8);

-- 人员-标签 绑定关系
INSERT INTO person_tag (person_id, tag_id) SELECT 1001, 1 WHERE NOT EXISTS (SELECT 1 FROM person_tag WHERE person_id = 1001 AND tag_id = 1);
INSERT INTO person_tag (person_id, tag_id) SELECT 1001, 6 WHERE NOT EXISTS (SELECT 1 FROM person_tag WHERE person_id = 1001 AND tag_id = 6);
INSERT INTO person_tag (person_id, tag_id) SELECT 1002, 1 WHERE NOT EXISTS (SELECT 1 FROM person_tag WHERE person_id = 1002 AND tag_id = 1);
INSERT INTO person_tag (person_id, tag_id) SELECT 1002, 3 WHERE NOT EXISTS (SELECT 1 FROM person_tag WHERE person_id = 1002 AND tag_id = 3);
INSERT INTO person_tag (person_id, tag_id) SELECT 1002, 7 WHERE NOT EXISTS (SELECT 1 FROM person_tag WHERE person_id = 1002 AND tag_id = 7);
INSERT INTO person_tag (person_id, tag_id) SELECT 1005, 2 WHERE NOT EXISTS (SELECT 1 FROM person_tag WHERE person_id = 1005 AND tag_id = 2);
INSERT INTO person_tag (person_id, tag_id) SELECT 1005, 3 WHERE NOT EXISTS (SELECT 1 FROM person_tag WHERE person_id = 1005 AND tag_id = 3);
INSERT INTO person_tag (person_id, tag_id) SELECT 1006, 2 WHERE NOT EXISTS (SELECT 1 FROM person_tag WHERE person_id = 1006 AND tag_id = 2);
INSERT INTO person_tag (person_id, tag_id) SELECT 1007, 5 WHERE NOT EXISTS (SELECT 1 FROM person_tag WHERE person_id = 1007 AND tag_id = 5);
INSERT INTO person_tag (person_id, tag_id) SELECT 1008, 6 WHERE NOT EXISTS (SELECT 1 FROM person_tag WHERE person_id = 1008 AND tag_id = 6);
INSERT INTO person_tag (person_id, tag_id) SELECT 1009, 4 WHERE NOT EXISTS (SELECT 1 FROM person_tag WHERE person_id = 1009 AND tag_id = 4);
INSERT INTO person_tag (person_id, tag_id) SELECT 1012, 7 WHERE NOT EXISTS (SELECT 1 FROM person_tag WHERE person_id = 1012 AND tag_id = 7);
INSERT INTO person_tag (person_id, tag_id) SELECT 1013, 3 WHERE NOT EXISTS (SELECT 1 FROM person_tag WHERE person_id = 1013 AND tag_id = 3);
INSERT INTO person_tag (person_id, tag_id) SELECT 1014, 5 WHERE NOT EXISTS (SELECT 1 FROM person_tag WHERE person_id = 1014 AND tag_id = 5);
INSERT INTO person_tag (person_id, tag_id) SELECT 1015, 6 WHERE NOT EXISTS (SELECT 1 FROM person_tag WHERE person_id = 1015 AND tag_id = 6);
INSERT INTO person_tag (person_id, tag_id) SELECT 1015, 8 WHERE NOT EXISTS (SELECT 1 FROM person_tag WHERE person_id = 1015 AND tag_id = 8);

-- 角色（管理员、普通用户）
INSERT INTO role (id, name, code) SELECT 1, '管理员', 'ADMIN' WHERE NOT EXISTS (SELECT 1 FROM role WHERE id = 1);
INSERT INTO role (id, name, code) SELECT 2, '普通用户', 'USER' WHERE NOT EXISTS (SELECT 1 FROM role WHERE id = 2);

-- 用户（密码均为 123456，使用 BCrypt 加密）
INSERT INTO sys_user (id, username, password_hash, display_name)
SELECT 1, 'admin', '$2a$10$jPAmlvmBK6sLwjpaCi5Wk.DqISxEMB.UMmLgcSYRza3xc7JXc2qi2', '系统管理员'
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 1);
INSERT INTO sys_user (id, username, password_hash, display_name)
SELECT 2, 'user', '$2a$10$jPAmlvmBK6sLwjpaCi5Wk.DqISxEMB.UMmLgcSYRza3xc7JXc2qi2', '普通员工'
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 2);

-- 用户-角色关联
INSERT INTO sys_user_role (user_id, role_id) SELECT 1, 1 WHERE NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id = 1 AND role_id = 1);
INSERT INTO sys_user_role (user_id, role_id) SELECT 2, 2 WHERE NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id = 2 AND role_id = 2);
