-- ============================================================
-- 企业内部人员管理系统 表结构（H2 文件数据库，MODE=MySQL）
-- IF NOT EXISTS + 每次启动执行，保证可重复初始化
-- ============================================================

-- 人员表（基础信息由外部系统同步，只读；superior_id 为本地可维护的上级关系，NULL 表示顶级节点）
CREATE TABLE IF NOT EXISTS person (
    id          BIGINT       NOT NULL,
    name        VARCHAR(50)  NOT NULL,
    employee_no VARCHAR(30)  NOT NULL,
    department  VARCHAR(100),
    position    VARCHAR(100),
    email       VARCHAR(100),
    phone       VARCHAR(30),
    superior_id BIGINT,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_person PRIMARY KEY (id)
);

-- 全局标签库
CREATE TABLE IF NOT EXISTS tag (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    name       VARCHAR(50) NOT NULL,
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
