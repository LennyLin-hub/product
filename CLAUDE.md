# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

这是一个基于Spring Boot 3.5的注塑排程系统，采用Maven多模块架构，使用Java 17开发。项目遵循标准的分层架构模式，包含通用工具、实体对象、基础框架、业务服务等多个模块。

## 模块架构

项目采用11个模块的多层架构设计：

### 基础支撑模块
- **product-common**: 通用工具和基础设施模块
  - 核心工具类：StringUtils, DateUtils, HttpUtils, ServletUtils, SpringUtils
  - 缓存系统：RedisCache (分布式缓存), LocalCache (本地缓存)
  - 安全防护：XSS防护 (XssFilter, XssHttpServletRequestWrapper, @Xss注解)
  - 防重复提交：@RepeatSubmit注解和RepeatSubmitInterceptor拦截器
  - ID生成：IdUtils, UUID, Seq
  - HTTP处理：HttpUtils, HttpHelper
  - 文件处理：FileUploadUtils, AliOssUtil (阿里云OSS文件上传)
  - 核心组件：AjaxResult (统一响应), BaseEntity (基础实体), PageDomain (分页)

- **product-domain**: 实体类和数据传输对象模块
  - 依赖product-common
  - 包含所有业务实体类和DTO
  - 使用MyBatis Plus注解和Lombok简化开发

- **product-cache**: Redis缓存模块
  - MultiCacheUtil: 多级缓存工具 (Caffeine本地缓存 + Redis分布式缓存)
  - @MyRedisCache: 自定义缓存注解，支持SpEL表达式
  - @IpGuard: IP防护注解，防止恶意请求
  - RedisLuaScript: Redis Lua脚本封装
  - MyCacheAspect: 缓存切面实现

- **product-framework**: 基础框架配置模块
  - MybatisConfig: MyBatis Plus配置 (分页插件、别名包扫描、Mapper XML加载)
  - DruidConfig: Druid数据库连接池配置
  - ThreadPoolConfig: 异步任务线程池配置
  - CustomIdGenerator: 自定义ID生成器
  - GlobalExceptionHandler: 全局异常处理器
  - ApplicationConfig: 应用基础配置
  - ResourcesConfig: 静态资源配置
  - OssConfiguration: 阿里云OSS配置
  - I18nConfig: 国际化配置

### 核心业务模块
- **product-core**: 依赖实体的公共模块
  - 依赖product-common和product-domain
  - 包含用户、角色、权限等核心实体
  - 使用UserAgentUtils解析客户端信息
  - JWT令牌管理

- **product-system-api**: 系统API模块
  - 依赖product-core
  - 抽离出来的系统服务接口
  - 为其他模块提供系统服务API

- **product-system**: 字典和系统管理模块
  - 依赖product-system-api
  - 系统字典管理
  - 系统配置管理

- **product-auth**: 认证授权模块
  - 依赖product-system-api
  - Spring Security集成
  - JWT Token认证
  - Kaptcha验证码
  - 登录、登出、权限验证

- **product-become**: 代码生成模块
  - 依赖product-core
  - 基于MyBatis Plus + Velocity模板引擎
  - 支持Java、Vue3、XML代码生成
  - 模板位于：src/main/resources/vm/
    - java/: Entity, Mapper, Service, Controller
    - vue/v3/: Vue3前端组件
    - xml/: MyBatis Mapper XML
    - sql/: SQL脚本
    - js/: JavaScript工具
  - 配置文件：generator.yml (作者、包路径、表前缀等)

- **product-demand**: 产品与订单模块
  - 依赖product-core
  - 客户管理
  - 产品管理
  - 订单管理

### 启动模块
- **product-server**: 应用程序启动模块
  - 依赖所有业务模块 (product-framework, product-auth, product-become, product-system, product-demand)
  - ProductServerApplication: 主启动类
  - 使用@MapperScan扫描所有Mapper接口
  - 排除DataSource自动配置，使用自定义数据源

## 核心技术栈

- **Java 17**: 基础开发语言
- **Spring Boot 3.5.0**: 主框架
- **Spring Security 3.x**: 安全框架
- **MyBatis Plus 3.5.9**: ORM框架 (支持分页、代码生成)
- **Redis**: 缓存和会话管理
- **MySQL 8.x**: 关系型数据库
- **Druid 1.2.23**: 数据库连接池
- **JWT (jjwt 0.9.1)**: 令牌认证
- **FastJSON2 2.0.53**: JSON序列化
- **Velocity 2.3**: 代码生成模板引擎
- **Caffeine 3.2.2**: 本地缓存
- **阿里云OSS 3.17.4**: 文件存储服务
- **Kaptcha 2.3.3**: 验证码生成
- **Apache POI 4.1.2**: Excel处理
- **Lombok**: 代码简化

## 开发命令

### 构建和运行
```bash
# 清理并编译整个项目
mvn clean compile

# 打包项目
mvn clean package

# 跳过测试打包
mvn clean package -DskipTests

# 安装到本地仓库
mvn clean install

# 运行应用
mvn spring-boot:run -pl product-server

# 直接运行JAR包
java -jar product-server/target/product-server-0.0.1-SNAPSHOT.jar
```

### 测试
```bash
# 运行所有测试
mvn test

# 运行指定模块测试
mvn test -pl product-common

# 运行单个测试类
mvn test -Dtest=ProductServerApplicationTests

# 运行单个测试方法
mvn test -Dtest=ProductServerApplicationTests#contextLoads
```

### 开发调试
```bash
# 以开发模式运行
mvn spring-boot:run -pl product-server -Dspring-boot.run.profiles=dev

# 查看依赖树
mvn dependency:tree

# 查看有效POM
mvn help:effective-pom

# 查看插件配置
mvn help:describe -Dplugin=compiler
```

## 配置文件说明

主要配置文件位于product-server/src/main/resources/:

- **application.yml**: 主配置文件
  - 服务端口：8081
  - 文件上传路径配置
  - 验证码类型配置 (math/char)
  - Redis连接配置
  - Token配置 (密钥、有效期)
  - MyBatis配置 (别名包、Mapper XML路径、配置文件路径)
  - XSS防护配置
  - 用户密码重试锁定配置
  - 阿里云OSS配置

- **application-druid.yml**: 数据库连接池配置
  - Druid监控配置
  - 连接池参数配置

- **mybatis-config.xml**: MyBatis配置
  - 行为控制设置
  - 日志实现

- **logback-spring.xml**: 日志配置
  - 控制台输出
  - 文件滚动输出
  - 不同环境日志级别

- **generator.yml**: 代码生成配置
  - 作者名、包路径、表前缀、是否覆盖等配置

## 分层架构设计

项目采用经典的分层架构，每层都有明确的职责：

### Controller层
- 位置：各模块的controller包
- 职责：处理HTTP请求，参数验证，调用Service层
- 注解：@RestController, @RequestMapping, @GetMapping等
- 特性：
  - 使用AjaxResult统一响应格式
  - @RepeatSubmit防重复提交
  - @Xss XSS防护
  - @Anonymous匿名访问接口

### Service层
- 位置：各模块的service包
- 职责：业务逻辑处理，事务管理
- 注解：@Service
- 特性：
  - 接口与实现分离 (XxxService / XxxServiceImpl)
  - 事务注解：@Transactional
  - 可选缓存注解：@MyRedisCache

### Mapper层
- 位置：各模块的mapper包
- 职责：数据访问，SQL执行
- 注解：@Mapper
- 特性：
  - 继承BaseMapper<T>获得基础CRUD
  - 自定义SQL方法
  - 对应的XML文件位于resources/mapper/目录

### Entity层
- 位置：product-domain模块
- 职责：数据模型定义
- 特性：
  - 继承BaseEntity获得通用字段
  - @TableName指定表名
  - @TableId指定主键策略
  - @TableField处理字段映射

## 核心注解说明

### 通用注解
- **@Anonymous**: 标记匿名访问接口 (无需登录)
- **@RepeatSubmit**: 防重复提交注解
  - interval: 间隔时间(毫秒)
  - timeUnit: 时间单位
- **@Xss**: XSS防护注解，用于参数验证
- **@Excel**: Excel导出注解
- **@Excels**: 多个Excel导出注解

### 缓存注解
- **@MyRedisCache**: 自定义Redis缓存注解
  - key: 缓存键 (支持SpEL表达式)
  - expire: 过期时间
- **@IpGuard**: IP防护注解

## 缓存策略

项目采用多级缓存架构：

1. **本地缓存 (Caffeine)**
   - 速度快，但容量有限
   - 适合热点数据
   - 使用MultiCacheUtil自动管理

2. **分布式缓存 (Redis)**
   - 容量大，支持集群
   - 适合共享数据
   - 使用RedisCache或@MyRedisCache注解

3. **缓存使用规范**
   - 遵循CacheConstants中定义的缓存键命名
   - 优先使用@MyRedisCache注解 (简化代码)
   - 复杂场景使用MultiCacheUtil或RedisCache

## 安全特性

### XSS防护
- XssFilter: 过滤所有HTTP请求
- XssHttpServletRequestWrapper: 包装请求对象
- @Xss注解: 方法参数级别的XSS验证
- 配置文件中可配置排除路径和匹配路径

### 防重复提交
- 基于Redis的分布式锁机制
- @RepeatSubmit注解标记需要防重复提交的接口
- RepeatSubmitInterceptor拦截器实现
- 支持配置间隔时间和时间单位

### 认证授权
- Spring Security + JWT
- Token存储在Redis中
- 支持Token续期
- 登录失败锁定机制

## 代码生成器使用

### 配置
编辑product-become/src/main/resources/generator.yml:
```yaml
gen:
  author: product              # 作者名
  packageName: com.product     # 包路径
  autoRemovePre: false         # 自动去除表前缀
  tablePrefix: sys_            # 表前缀
  allowOverwrite: false        # 是否允许覆盖
```

### 生成内容
- **Java代码**: Entity, Mapper, Mapper.xml, Service, ServiceImpl, Controller
- **Vue3组件**: 列表页、详情页
- **SQL脚本**: 建表语句

### 模板位置
- Java模板: product-become/src/main/resources/vm/java/
- Vue3模板: product-become/src/main/resources/vm/vue/v3/
- XML模板: product-become/src/main/resources/vm/xml/

## 开发规范

### 命名规范
- 包名：全小写，使用点分隔
- 类名：大驼峰命名法
- 方法名和变量名：小驼峰命名法
- 常量：全大写，下划线分隔

### API设计规范
- 遵循RESTful设计原则
- 统一使用AjaxResult返回
- 响应格式：
  ```json
  {
    "code": 200,
    "msg": "操作成功",
    "data": {}
  }
  ```
- 使用HTTP状态码和业务状态码结合

### 异常处理
- 使用UtilException抛出工具类异常
- 全局异常处理器统一处理
- 异常信息记录日志

### 数据库规范
- 表名：小写，下划线分隔
- 字段名：小写，下划线分隔
- 必须字段：create_time, update_time, create_by, update_by
- 逻辑删除：del_flag (0=正常, 1=删除)

### 事务管理
- Service层方法使用@Transactional
- 查询方法使用@Transactional(readOnly = true)
- 避免在事务中调用外部API

## 环境要求

- **JDK**: 17+
- **Maven**: 3.6+
- **MySQL**: 8.0+
- **Redis**: 6.0+
- **IDE**: IntelliJ IDEA 或 Eclipse (推荐IDEA)

## 数据库配置

默认使用Druid连接池，配置在application-druid.yml中：
- 初始连接数
- 最小/最大连接数
- 获取连接超时时间
- 空闲连接检测
- 慢SQL记录
- StatView监控页面

## 常见问题

### 分页查询不生效
确保：
1. MybatisConfig已配置PaginationInnerInterceptor
2. 使用Page<T>对象作为参数
3. 不要在Mapper XML中手动编写limit

### 缓存注解不生效
确保：
1. 方法是public的
2. 类是Spring管理的Bean (@Service, @Component等)
3. SpEL表达式语法正确

### Mapper接口找不到
确保：
1. @MapperScan配置正确：com.product.**.mapper
2. Mapper接口上有@Mapper注解
3. Mapper.xml文件路径配置正确

### 启动报数据源错误
确保：
1. ProductServerApplication使用了exclude = { DataSourceAutoConfiguration.class }
2. DruidConfig正确配置了数据源
3. MySQL连接依赖已引入
