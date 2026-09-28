# 企业内部人员管理系统

企业内部人员管理系统：人员（外部同步、只读）+ 上下级组织架构 + 全局标签库 + 人员标签多对多绑定 + 查询（关键词/标签/分页）。
需求详见 [开发文档.md](./开发文档.md)。

## 技术栈

| 层次 | 技术 |
| ---- | ---- |
| 后端 | JDK 17 / SpringBoot 2.7.18 / MyBatis-Plus 3.5.7 |
| 数据库 | H2 文件数据库（`./data/personnel.mv.db`，持久化） |
| 前端 | Vue 3 / Vite 5 / Element Plus / vue-router / axios |
| 构建 | Maven / npm |

## 项目结构

```
├── pom.xml                        # 后端 Maven 工程（根目录）
├── src/main/java/...              # 后端源码（com.enterprise.personnel）
├── src/main/resources/            # application.yml / schema.sql / data.sql
├── data/                          # H2 数据库文件（运行时生成，不入库）
└── frontend/                      # 前端工程
    └── src/
        ├── api/request.js         # axios 统一实例（/api）
        ├── router/                # 路由
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
