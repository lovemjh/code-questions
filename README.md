# 企业内部人员管理系统

企业内部人员管理系统：人员（外部同步、只读）+ 上下级组织架构（拖拽调整上级）+ 全局标签库（支持 CSV 导入/导出）+ 人员标签多对多绑定 + 查询（关键词/标签/分页）+ 人员总览（打标大盘 / 标签绑定人次图表）+ 用户认证与权限控制 + 外部人员同步接口（业务 Token 鉴权，批量 upsert + 离职标记）。
需求详见 [开发文档.md](./开发文档.md)，同步接口协议详见 [人员同步接口设计方案.md](./人员同步接口设计方案.md)。

## 技术栈

| 层次 | 技术 |
| ---- | ---- |
| 后端 | JDK 17 / SpringBoot 2.7.18 / MyBatis-Plus 3.5.7 / Spring Security / JWT |
| 数据库 | H2 文件数据库（`./data/personnel.mv.db`，持久化） |
| 前端 | Vue 3 / Vite 5 / Element Plus / vue-router / axios / ECharts |
| 构建 | Maven / npm |

## 项目结构

```
├── pom.xml                        # 后端 Maven 工程（根目录）
├── src/main/java/...              # 后端源码（com.enterprise.personnel）
├── src/main/resources/            # application.yml / schema.sql / data.sql
├── data/                          # H2 数据库文件（运行时生成，不入库）
└── frontend/                      # 前端工程
    └── src/
        ├── api/                   # axios 统一实例 + 各模块 API
        ├── router/                # 路由（含路由守卫）
        ├── utils/                 # 工具函数（Token / 用户信息存取）
        └── views/                 # 页面
```

## 启动方式

### 后端（端口 8080）

```bash
mvn spring-boot:run
```

- H2 控制台：http://localhost:8080/h2-console （JDBC URL：`jdbc:h2:file:./data/personnel;MODE=MySQL`，用户名 `sa`，密码为空）

### 前端（端口 5173）

```bash
cd frontend
npm install
npm run dev
```

- 开发环境通过 Vite 代理将 `/api` 请求转发到后端 `http://localhost:8080`。

### 生产构建

```bash
cd frontend && npm run build   # 产物输出至 frontend/dist
mvn package                    # 后端可执行 jar
```

## 测试账号

| 用户名 | 密码 | 角色 | 权限 |
|--------|------|------|------|
| admin | 123456 | 管理员 | 所有功能 |
| user | 123456 | 普通用户 | 仅查看（人员总览、人员列表、组织架构、标签列表） |

## 权限说明

| 角色 | 权限 |
|------|------|
| 管理员 | 人员查看、上级调整（拖拽交互）、标签管理（含 CSV 导入/导出）、用户管理、角色管理 |
| 普通用户 | 仅查看（人员总览、人员列表、组织架构、标签列表），不可修改 |

补充说明：

- 权限由**角色**决定，与用户名无关：`admin` 只是一个普通用户名，它能管理系统是因为被赋予了 `ADMIN` 角色。
- 管理员调整某用户的角色后**立即生效**（服务端角色缓存同步失效），无需重新登录；账号被删除后其旧 Token 立即失效。
- 未登录或 Token 失效返回 `401`，已登录但角色不足返回 `403`，均返回统一 `Result` 结构。
- 密码长度至少 6 位（新建用户、修改密码时校验）。
- 管理员不能删除自己的账号，也不能移除自己的管理员角色，避免系统失去管理员。

## 外部人员同步接口

- `POST /api/sync/persons`，请求头 `X-Sync-Token`，数组入参（单条 = 数组 1 个元素），单批 ≤ 500 条，逐条 upsert，幂等可重试。
- Token 配置于 `application.yml` 的 `sync.api.token`，可用环境变量 `SYNC_API_TOKEN` 覆盖；**为空时同步接口整体禁用**（403）。
- 同步范围：人员基础字段 + `resigned` 离职标记（null = 忽略字段）；上级关系与业务标签是本地数据，同步永不触碰。
- `resigned=true` 的人员自动绑定系统标签「离职」（不可手动绑定/删除/重命名），组织架构树不展示，其下属提升为顶级节点；复职推 `false` 自动恢复。
- 每次同步逐条写入 `sync_log` 审计表（批次号 / 动作 / 结果 / 来源 IP），不落姓名、手机号等字段值。
- 完整协议、字段约束与错误码见 [人员同步接口设计方案.md](./人员同步接口设计方案.md)。
