# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

这是一个基于Spring Boot 3.5的注塑排程系统，采用Maven多模块架构，使用Java 17开发。项目遵循标准的分层架构模式，包含通用工具、实体对象和业务服务三个核心模块。

## 模块架构

- **product-common**: 通用工具和基础设施模块
  - 核心工具类：StringUtils, DateUtils, HttpUtils, ServletUtils
  - 缓存系统：LocalCache, RedisCache, CacheConstants
  - 安全防护：XSS防护、防重复提交机制
  - ID生成：IdUtils, UUID, Seq
  - HTTP处理：HttpUtils, HttpHelper

- **product-pojo**: 实体类和数据传输对象模块
  - AjaxResult：统一API响应格式

- **product-server**: 业务逻辑和应用程序入口
  - ProductServerApplication：主启动类
  - 防重复提交：@RepeatSubmit注解和拦截器实现
  - 业务逻辑层和服务层

## 核心技术栈

- **Java 17**: 基础开发语言
- **Spring Boot 3.5.0**: 主框架
- **Spring Security 3.x**: 安全框架
- **MyBatis Plus 3.5.15**: ORM框架
- **Redis**: 缓存和会话管理
- **MySQL 8.x**: 关系型数据库
- **Druid 1.2.23**: 数据库连接池
- **JWT**: 令牌认证
- **FastJSON2**: JSON序列化

## 开发命令

### 构建和运行
```bash
# 编译整个项目
mvn clean compile

# 打包项目
mvn clean package

# 跳过测试打包
mvn clean package -DskipTests

# 运行应用
mvn spring-boot:run -pl product-server

# 安装到本地仓库
mvn clean install
```

### 测试
```bash
# 运行所有测试
mvn test

# 运行指定模块测试
mvn test -pl product-common

# 运行单个测试类
mvn test -Dtest=TestClassName
```

### 开发调试
```bash
# 以开发模式运行
mvn spring-boot:run -pl product-server -Dspring-boot.run.profiles=dev

# 查看依赖树
mvn dependency:tree

# 重新编译并运行
mvn clean compile spring-boot:run -pl product-server
```

## 配置文件说明

- **application.yml**: 主配置文件，包含服务端口、Redis、JWT、MyBatis等配置
- **application-druid.yml**: 数据库连接池配置，使用Druid
- **mybatis-config.xml**: MyBatis ORM框架配置
- **logback-spring.xml**: 日志配置文件

## 核心业务概念

### 生产流程
1. **计划(Plan)**: 订单 → 路线 → 批次 → 任务
2. **派工(Dispatch)**: 资源模型 + 排程结果
3. **执行(Execute)**: 报工事件 → 实时进度/追溯
4. **异常(Exception)**: 停机/保养/缺人等 → 触发重排
5. **完工(Finish)**: 任务DONE → 批次DONE → 订单DONE

### 核心数据模型
- CustomerOrder/OrderLine: 订单管理
- ProductRoute/RouteOperation: 工艺路线
- ProductionBatch/OperationTask: 生产批次和任务
- Resource/Machine/Mold: 资源管理
- TaskAssignment: 派工排程结果

## 安全特性

### XSS防护
- 使用XssFilter和XssHttpServletRequestWrapper进行请求过滤
- XssValidator进行输入验证
- @Xss注解用于方法参数验证

### 防重复提交
- 基于Redis的分布式锁机制
- @RepeatSubmit注解标记需要防重复提交的接口
- RepeatSubmitInterceptor拦截器实现具体逻辑

## 开发规范

### 代码结构
- Controller层：处理HTTP请求，使用@RestContriller
- Service层：业务逻辑层，使用@Service
- Mapper层：数据访问层，使用@Mapper
- Entity层：实体类，对应数据库表结构

### 异常处理
- 使用UtilException抛出工具类异常
- AjaxResult统一响应格式
- 全局异常处理器建议配置在启动模块

### 缓存使用
- 优先使用RedisCache进行分布式缓存
- LocalCache用于单机缓存场景
- 遵循CacheConstants中定义的缓存键命名规范

### API设计
- 遵循RESTful设计原则
- 统一使用AjaxResult返回结果
- JWT Token认证机制
- 合理使用Spring Security进行权限控制

## 环境要求

- **JDK**: 17+
- **Maven**: 3.6+
- **MySQL**: 8.0+
- **Redis**: 6.0+
- **IDE**: IntelliJ IDEA 或 Eclipse

## 数据库连接

默认数据库配置在application-druid.yml中，包含：
- 连接池配置
- 超时设置
- 空闲连接检测
- 性能监控配置

## 日志配置

使用logback-spring.xml进行日志配置，支持：
- 控制台输出
- 文件滚动输出
- 不同环境日志级别控制
- 日志格式自定义