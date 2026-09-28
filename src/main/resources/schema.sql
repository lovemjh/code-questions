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
