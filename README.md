f# 注塑排程系统

一个基于Spring Boot 3.5的注塑生产排程管理系统，采用Maven多模块架构，提供从订单管理到生产排程、生产执行的全流程解决方案。

## 项目简介

本系统针对注塑行业的生产管理需求，实现了以下核心功能：

- **订单管理**：客户订单录入、查询、导入导出、状态跟踪
- **排程要素**：
  - 交期（EDD - Earliest Due Date）：订单必须交付的日期
  - 优先级：支持插单/加急等特殊需求
  - 订单状态：NEW（新建）→ CONFIRMED（已确认）→ IN_PRODUCTION（生产中）→ DONE（完成）
- **生产排程**：基于EDD和优先级的自动排程算法
- **生产执行**：工单管理、生产进度跟踪、任务事件处理
- **主数据管理**：机台信息、日历管理等基础数据维护

## 技术栈

### 后端技术
- **Java 17**：基础开发语言
- **Spring Boot 3.5.0**：主框架
- **Spring Security 3.x**：安全框架
- **MyBatis Plus 3.5.9**：ORM框架（支持分页、代码生成）
- **Redis**：缓存和会话管理
- **MySQL 8.x**：关系型数据库
- **Druid 1.2.23**：数据库连接池
- **JWT (jjwt 0.9.1)**：令牌认证
- **FastJSON2 2.0.53**：JSON序列化
- **Velocity 2.3**：代码生成模板引擎
- **Caffeine 3.2.2**：本地缓存
- **阿里云OSS 3.17.4**：文件存储服务
- **Kaptcha 2.3.3**：验证码生成
- **Apache POI 4.1.2**：Excel处理
- **Lombok**：代码简化

### 模块架构（14个模块）

#### 基础支撑模块
- **product-common**：通用工具和基础设施
  - 核心工具类：StringUtils, DateUtils, HttpUtils, ServletUtils
  - 缓存系统：RedisCache, LocalCache, MultiCacheUtil
  - 安全防护：XSS防护、防重复提交
  - 文件处理：FileUploadUtils, AliOssUtil
  - 核心组件：AjaxResult, BaseEntity, PageDomain

- **product-domain**：实体类和数据传输对象
  - 包含所有业务实体类和DTO
  - 使用MyBatis Plus注解和Lombok

- **product-cache**：Redis缓存模块
  - @MyRedisCache：自定义缓存注解
  - @IpGuard：IP防护注解
  - 多级缓存工具（Caffeine + Redis）

- **product-framework**：基础框架配置
  - MyBatis Plus配置
  - Druid数据源配置
  - 线程池配置
  - 全局异常处理
  - 静态资源配置

#### 核心业务模块
- **product-core**：依赖实体的公共模块
  - 用户、角色、权限等核心实体
  - JWT令牌管理

- **product-system-api**：系统API模块
  - 系统服务接口抽象

- **product-system**：系统管理模块
  - 字典管理
  - 系统配置管理

- **product-auth**：认证授权模块
  - Spring Security集成
  - JWT Token认证
  - 登录、登出、权限验证
  - Kaptcha验证码

- **product-become**：代码生成模块
  - 基于MyBatis Plus + Velocity
  - 支持Java、Vue3、XML代码生成
  - 模板位置：`src/main/resources/vm/`

- **product-demand**：产品与订单模块
  - 客户管理
  - 产品管理
  - 订单管理（Excel导入导出、分页查询、批量操作）

- **product-pps**：生产计划排程模块
  - 生产批次管理
  - 任务分配
  - 工序任务管理
  - 排程算法实现

- **product-master**：主数据管理模块
  - 机台信息管理
  - 日历管理
  - 基础资源配置

- **product-execute**：生产执行模块
  - 任务事件管理
  - 生产进度跟踪

#### 启动模块
- **product-server**：应用程序启动模块
  - 依赖所有业务模块
  - 主启动类：ProductServerApplication

## 快速开始

### 环境要求
- **JDK**：17+
- **Maven**：3.6+
- **MySQL**：8.0+
- **Redis**：6.0+

### 安装步骤

1. **克隆项目**
```bash
git clone <repository-url>
cd product
```

2. **配置数据库**
```sql
CREATE DATABASE product CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

3. **修改配置文件**
编辑 `product-server/src/main/resources/application.yml`：
```yaml
spring:
  data:
    redis:
      host: localhost        # Redis地址
      port: 6379             # Redis端口
      password: 123456       # Redis密码
      database: 0

server:
  port: 8081                 # 服务端口

product:
  profile: /home/chuan/project/backend/product/uploadPath  # 文件上传路径

alioss:
  endpoint: oss-cn-beijing.aliyuncs.com    # 阿里云OSS端点
  access-key-id: YOUR_ACCESS_KEY           # 访问密钥ID
  access-key-secret: YOUR_SECRET_KEY       # 访问密钥
  bucket-name: YOUR_BUCKET                 # 存储桶名称
```

编辑 `product-server/src/main/resources/application-druid.yml`：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/product?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8
    username: root          # 数据库用户名
    password: your_password # 数据库密码
```

4. **编译项目**
```bash
# 清理并编译
mvn clean compile

# 打包项目
mvn clean package

# 跳过测试打包
mvn clean package -DskipTests

# 安装到本地仓库
mvn clean install
```

5. **运行应用**
```bash
# 使用Maven运行
mvn spring-boot:run -pl product-server

# 或直接运行JAR包
java -jar product-server/target/product-server-0.0.1-SNAPSHOT.jar
```

6. **访问应用**
```
应用地址：http://localhost:8081
```

## 开发指南

### 分层架构

项目采用经典的分层架构：

```
Controller层 → Service层 → Mapper层 → Entity层
     ↓           ↓          ↓         ↓
  处理HTTP    业务逻辑    数据访问   数据模型
   请求      事务管理    SQL执行
```

#### 各层职责
- **Controller层**：处理HTTP请求，参数验证，调用Service层
- **Service层**：业务逻辑处理，事务管理
- **Mapper层**：数据访问，SQL执行
- **Entity层**：数据模型定义
- **VO层**：视图对象，用于API返回和关联查询

### 核心注解说明

#### 通用注解
- `@Anonymous`：标记匿名访问接口（无需登录）
- `@RepeatSubmit`：防重复提交注解
- `@Xss`：XSS防护注解
- `@Excel`：Excel导出注解
- `@BizIdPrefix`：业务ID前缀注解

#### 缓存注解
- `@MyRedisCache`：自定义Redis缓存注解（支持SpEL表达式）
- `@IpGuard`：IP防护注解

### 开发新功能模块

#### 1. 创建模块结构
```
product-xxx/
├── pom.xml
└── src/main/
    ├── java/com/product/xxx/
    │   ├── controller/
    │   ├── service/
    │   ├── service/impl/
    │   └── mapper/
    └── resources/
        └── mapper/
```

#### 2. 定义实体类
位置：product-domain模块的entity包

```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("table_name")
public class EntityName extends BaseEntity {
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    @Excel(name = "字段名")
    @TableField(value = "field_name")
    private String fieldName;
}
```

#### 3. 创建VO对象（如需关联查询）
```java
@Data
public class EntityNameVO extends EntityName {
    private String relatedFieldName;  // 扩展的关联字段
}
```

#### 4. 创建Mapper接口
```java
@Mapper
public interface EntityNameMapper extends BaseMapper<EntityName> {
    Page<EntityNameVO> selectEntityNamePage(
        Page<EntityNameVO> page,
        @Param("entity") EntityName entity
    );
}
```

#### 5. 编写Mapper XML（如需自定义SQL）
位置：src/main/resources/mapper/EntityNameMapper.xml

#### 6. 创建Service接口和实现
```java
// 接口
public interface IEntityNameService extends IService<EntityName> {
    Page<EntityNameVO> selectEntityNamePage(Page<EntityNameVO> page, EntityName entity);
}

// 实现类
@Service
public class EntityNameServiceImpl
    extends ServiceImpl<EntityNameMapper, EntityName>
    implements IEntityNameService {
    @Override
    public Page<EntityNameVO> selectEntityNamePage(Page<EntityNameVO> page, EntityName entity) {
        return baseMapper.selectEntityNamePage(page, entity);
    }
}
```

#### 7. 创建Controller
```java
@RestController
@RequestMapping("/xxx/entity")
public class EntityNameController extends BaseController {
    @Autowired
    private IEntityNameService entityNameService;

    @GetMapping("/list")
    public TableDataInfo list(EntityName entity) {
        Page<EntityNameVO> page = PageUtils.buildPage();
        return getDataTable(entityNameService.selectEntityNamePage(page, entity));
    }
}
```

### 代码生成器使用

#### 配置代码生成器
编辑 `product-become/src/main/resources/generator.yml`：
```yaml
gen:
  author: product              # 作者名
  packageName: com.product     # 包路径
  autoRemovePre: false         # 自动去除表前缀
  tablePrefix: sys_            # 表前缀
  allowOverwrite: false        # 是否允许覆盖
```

#### 生成内容
- **Java代码**：Entity, Mapper, Mapper.xml, Service, ServiceImpl, Controller
- **Vue3组件**：列表页、详情页
- **SQL脚本**：建表语句

#### 模板位置
- Java模板：`product-become/src/main/resources/vm/java/`
- Vue3模板：`product-become/src/main/resources/vm/vue/v3/`
- XML模板：`product-become/src/main/resources/vm/xml/`

### 缓存策略

项目采用多级缓存架构：

1. **本地缓存（Caffeine）**
   - 速度快，容量有限
   - 适合热点数据
   - 使用MultiCacheUtil自动管理

2. **分布式缓存（Redis）**
   - 容量大，支持集群
   - 适合共享数据
   - 使用RedisCache或@MyRedisCache注解

3. **缓存使用规范**
   - 遵循CacheConstants中定义的缓存键命名
   - 优先使用@MyRedisCache注解
   - 复杂场景使用MultiCacheUtil或RedisCache

### 安全特性

- **XSS防护**：XssFilter + @Xss注解
- **防重复提交**：基于Redis的分布式锁 + @RepeatSubmit注解
- **认证授权**：Spring Security + JWT
- **登录失败锁定**：可配置重试次数和锁定时间

### Excel导入导出

```java
// 导出
List<Entity> list = entityService.selectList(entity);
ExcelUtil<Entity> util = new ExcelUtil<Entity>(Entity.class);
util.exportExcel(response, list, "数据文件名");

// 导入
ExcelUtil<Entity> util = new ExcelUtil<Entity>(Entity.class);
List<Entity> list = util.importExcel(inputStream);
int count = entityService.batchInsert(list);
```

### 分页查询实现

```java
// Controller层
Page<EntityVO> page = PageUtils.buildPage();
return getDataTable(entityService.selectPage(page, entity));

// Service层
Page<EntityVO> selectPage(Page<EntityVO> page, Entity entity);

// Mapper XML
<select id="selectPage" resultMap="EntityMap">
    select * from table_name
    order by create_time desc
</select>
```

## 常用命令

### Maven命令
```bash
# 清理并编译
mvn clean compile

# 打包
mvn clean package

# 跳过测试打包
mvn clean package -DskipTests

# 安装到本地仓库
mvn clean install

# 运行应用
mvn spring-boot:run -pl product-server

# 查看依赖树
mvn dependency:tree

# 运行所有测试
mvn test

# 运行单个测试类
mvn test -Dtest=ProductServerApplicationTests
```

### Git命令
```bash
# 查看状态
git status

# 查看分支
git branch -a

# 查看最近提交
git log --oneline -10
```

## API接口

### 认证相关
- `POST /login`：用户登录
- `POST /logout`：用户登出
- `GET /getInfo`：获取当前用户信息

### 订单管理
- `GET /demand/order/list`：分页查询订单
- `POST /demand/order`：新增订单
- `PUT /demand/order`：修改订单
- `DELETE /demand/order/{orderIds}`：删除订单
- `POST /demand/order/export`：导出Excel
- `POST /demand/order/importData`：导入Excel
- `POST /demand/order/importTemplate`：下载导入模板

### 客户管理
- `GET /demand/customer/list`：分页查询客户
- `POST /demand/customer`：新增客户
- `PUT /demand/customer`：修改客户
- `DELETE /demand/customer/{customerIds}`：删除客户

### 产品管理
- `GET /demand/product/list`：分页查询产品
- `POST /demand/product`：新增产品
- `PUT /demand/product`：修改产品
- `DELETE /demand/product/{productIds}`：删除产品

### 主数据管理
- `GET /master/machine/list`：查询机台列表
- `POST /master/machine`：新增机台
- `PUT /master/machine`：修改机台
- `DELETE /master/machine/{machineIds}`：删除机台
- `GET /master/calendar/list`：查询日历列表

### 生产排程
- `GET /pps/batch/list`：查询生产批次
- `POST /pps/batch`：创建生产批次
- `GET /pps/assignment/list`：查询任务分配
- `POST /pps/assignment`：分配任务

### 生产执行
- `GET /execute/event/list`：查询任务事件
- `POST /execute/event`：记录任务事件

## 配置说明

### application.yml主配置
- 服务端口：8081
- 文件上传路径配置
- 验证码类型配置（math/char）
- Redis连接配置
- Token配置（密钥、有效期）
- MyBatis配置
- XSS防护配置
- 用户密码重试锁定配置
- 阿里云OSS配置

### application-druid.yml数据源配置
- Druid监控配置
- 连接池参数配置

### mybatis-config.xml
- MyBatis行为控制设置
- 日志实现

### logback-spring.xml
- 控制台输出
- 文件滚动输出
- 不同环境日志级别

### generator.yml代码生成配置
- 作者名、包路径、表前缀等配置

## 项目进度

### 已完成模块
- ✅ 基础框架搭建
  - Spring Boot + MyBatis Plus + Spring Security + JWT
  - Redis缓存、Druid连接池、阿里云OSS
  - XSS防护、防重复提交、全局异常处理
- ✅ 系统管理功能
  - 用户、角色、权限管理
  - 字典管理、系统配置
- ✅ 认证授权功能
  - 登录、登出、Token管理
  - 验证码、权限校验
- ✅ 代码生成器
  - 支持MyBatis Plus + Velocity
  - 生成Entity、Mapper、Service、Controller、Vue3组件
- ✅ 产品与订单模块（product-demand）
  - 客户管理：Customer CRUD
  - 产品管理：Product CRUD
  - 订单管理：CustomerOrder完整功能
- 🚧 生产排程模块（product-pps）
  - 生产批次管理
  - 任务分配
  - 工序任务管理
- 🚧 主数据管理模块（product-master）
  - 机台信息管理
  - 日历管理
- 🚧 生产执行模块（product-execute）
  - 任务事件管理

### 待开发功能
- ⏳ 排程算法完善
  - EDD算法优化
  - 优先级规则实现
  - 产能规划
- ⏳ 生产执行跟踪
  - 工单管理完善
  - 生产进度实时跟踪
  - 异常处理机制
- ⏳ 报表统计
  - 订单统计分析
  - 生产效率分析
  - 交付达成率统计

## 开发规范

### 命名规范
- **包名**：全小写，使用点分隔
- **类名**：大驼峰命名法
- **方法名和变量名**：小驼峰命名法
- **常量**：全大写，下划线分隔

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

### 数据库规范
- **表名**：小写，下划线分隔
- **字段名**：小写，下划线分隔
- **必须字段**：create_time, update_time, create_by, update_by
- **逻辑删除**：del_flag (0=正常, 1=删除)

### 事务管理
- Service层方法使用@Transactional
- 查询方法使用@Transactional(readOnly = true)
- 避免在事务中调用外部API

## 常见问题

### 分页查询不生效
确保：
1. MybatisConfig已配置PaginationInnerInterceptor
2. 使用Page<T>对象作为参数
3. 不要在Mapper XML中手动编写limit

### 缓存注解不生效
确保：
1. 方法是public的
2. 类是Spring管理的Bean（@Service, @Component等）
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

## 文档目录

- [CLAUDE.md](./CLAUDE.md)：Claude Code项目指南（详细的开发规范和模块说明）
- [docs/](./docs/)：项目文档目录
  - [关键配置流程.md](./docs/关键配置流程.md)
  - [注塑件加工流程.md](./docs/注塑件加工流程.md)
  - [自动排程伪代码.md](./docs/自动排程伪代码.md)
  - [代码生成相关文档](./docs/gen-utils-guide.md)
  - [分页插件问题](./docs/pagination-plugin-issue.md)

## 贡献指南

欢迎提交Issue和Pull Request！

## 许可证

本项目采用私有许可证。

## 联系方式

如有问题，请联系开发团队。

---

**最后更新时间**：2025-01-03
**当前版本**：0.0.1-SNAPSHOT
