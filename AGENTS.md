# Repository Guidelines

## 项目结构与模块
- 根 `pom.xml` 为 Maven 聚合工程（Java 17），包含三大模块：`product-common`（公共配置/工具/过滤器/异常）、`product-pojo`（DTO/VO/实体）、`product-server`（Spring Boot 应用与资源）。
- 主代码放在各模块的 `src/main/java`；测试在 `src/test/java`。MyBatis XML、应用配置、日志配置位于 `product-server/src/main/resources`（含 `application.yml`、`application-druid.yml`、`mybatis/mybatis-config.xml`、`logback-spring.xml`）。

## 构建、测试与本地运行
- `mvn clean install`：编译并安装全部模块到本地仓库。
- `mvn clean package -DskipTests`：快速打包生成可运行 Jar。
- `mvn test`：运行单元/集成测试。
- `mvn spring-boot:run -pl product-server -Dspring.profiles.active=dev`：启动后端 API（按需切换 profile，如 `dev`/`prod`）。
- 如需仅运行某模块测试：`mvn -pl product-pojo test`。

## 代码风格与命名
- 语言：Java 17；缩进 4 空格，建议行长 ≤120 字符。
- 包名小写（`com.product...`）；类名 PascalCase，方法/字段 camelCase。
- Mapper XML 放在 `resources/mybatis`，SQL 语句 ID 与对应方法同名；保持命名语义化（如 `selectByUserId`）。
- 优先复用已有工具类（如 `com.product.utils`），避免重复实现。

## 测试指南
- 框架：Spring Boot Test + JUnit 5；安全相关使用 `spring-security-test`。
- 测试类命名以 `*Test` 结尾并与被测类包路径对应。
- 测试数据使用内存或专用测试库，避免写入共享数据库；敏感信息通过环境变量或测试配置注入。
- 修改核心逻辑时，补充断言覆盖正常流与异常流；目标是保持关键分支可测试。

## 提交与 Pull Request
- 提交信息保持动词开头、简短明确：如 `add product cache`、`fix order pagination`。
- PR 必备：变更摘要、测试说明（列出执行的 `mvn` 命令）、关联的 issue/需求编号；涉及输出/接口变更时附截图或示例请求/响应。
- 控制粒度：一项功能或缺陷一份 PR；避免夹带无关重构。

## 安全与配置
- 不要提交密钥/密码；使用环境变量或未入库的 `application-*.yml`/`.env` 文件。
- 数据源与连接池参数集中于 `application-druid.yml`；根据部署环境调整最大连接数与超时。
- 日志级别在 `logback-spring.xml` 中配置，生产环境禁止长期启用 DEBUG。

## MyBatis 配置注意
- 自定义 `MybatisConfig` 时，需使用 MyBatis-Plus 提供的 `MybatisSqlSessionFactoryBean`（而非原生 `SqlSessionFactoryBean`），否则 MyBatis-Plus 自动注入的基础 CRUD 映射会缺失，出现诸如 `Invalid bound statement: BaseMapper#selectList` 的错误。
- 如果自定义 `MybatisSqlSessionFactoryBean`，记得显式注入 `GlobalConfig` 并设置 `MetaObjectHandler`（如 `MyMetaObjectHandler`），否则 MyBatis-Plus 的自动填充（`createTime`/`updateTime` 等）不会生效，可能导致更新时间为 null 触发约束错误。
