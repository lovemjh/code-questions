-- ============================================================
-- 企业内部人员管理系统 表结构（H2 文件数据库，MODE=MySQL）
-- IF NOT EXISTS + 每次启动执行，保证可重复初始化
-- ============================================================

-- 人员表（基础信息与离职标记由外部系统同步；superior_id 为本地可维护的上级关系，NULL 表示顶级节点；
-- resigned=TRUE 表示离职，组织架构树不展示）
CREATE TABLE IF NOT EXISTS person (
    id          BIGINT       NOT NULL,
    name        VARCHAR(50)  NOT NULL,
    employee_no VARCHAR(30)  NOT NULL,
    department  VARCHAR(100),
    position    VARCHAR(100),
    email       VARCHAR(100),
    phone       VARCHAR(30),
    superior_id BIGINT,
    resigned    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_person PRIMARY KEY (id),
    CONSTRAINT uk_person_employee_no UNIQUE (employee_no)
);

-- 全局标签库（is_system=TRUE 为系统标签，如「离职」，由同步自动维护，接口不可删改）
CREATE TABLE IF NOT EXISTS tag (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    name       VARCHAR(50) NOT NULL,
    is_system  BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_tag PRIMARY KEY (id),
    CONSTRAINT uk_tag_name UNIQUE (name)
);

-- 人员-标签 绑定关系（多对多）
CREATE TABLE IF NOT EXISTS person_tag (
    id         BIGINT    NOT NULL AUTO_INCREMENT,
    person_id  BIGINT    NOT NULL,
    tag_id     BIGINT    NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_person_tag PRIMARY KEY (id),
    CONSTRAINT uk_person_tag UNIQUE (person_id, tag_id)
);

-- 角色表
CREATE TABLE IF NOT EXISTS role (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    name       VARCHAR(50) NOT NULL,
    code       VARCHAR(50) NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_role PRIMARY KEY (id),
    CONSTRAINT uk_role_code UNIQUE (code)
);

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name  VARCHAR(50),
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_sys_user PRIMARY KEY (id),
    CONSTRAINT uk_sys_user_username UNIQUE (username)
);

-- 用户-角色 关联表（多对多）
CREATE TABLE IF NOT EXISTS sys_user_role (
    id      BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT pk_sys_user_role PRIMARY KEY (id),
    CONSTRAINT uk_user_role UNIQUE (user_id, role_id)
);

-- 外部同步审计日志（每次请求一个 batch_no；只记录 id 与结果，不落姓名/手机号等字段值）
CREATE TABLE IF NOT EXISTS sync_log (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    batch_no   VARCHAR(40)  NOT NULL,
    person_id  BIGINT,
    action     VARCHAR(10),
    success    BOOLEAN      NOT NULL,
    error_msg  VARCHAR(500),
    source_ip  VARCHAR(50),
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_sync_log PRIMARY KEY (id)
);
