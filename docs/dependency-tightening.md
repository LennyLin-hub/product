# Dependency Tightening Notes

本文档记录父 POM 与各模块依赖收敛的调整思路。

## 目标

- 避免父 POM 隐式下发运行时依赖
- 让每个模块只声明自己真正使用的依赖
- 仍保留统一的版本管理能力

## Parent POM（`product`）

- 移除了顶层 `<dependencies>`，避免子模块隐式继承运行时依赖
- 保留 `<dependencyManagement>` 统一管理版本
- 补充了若干常用依赖版本

## 模块依赖调整

### `product-common`

- 显式引入 Web、AOP、Validation、Redis
- 引入 MyBatis-Plus、工具包、JSON、JWT、Excel、OSS
- 保留 `jakarta.servlet-api` 为 `provided`

### `product-domain`

- 保留 `mybatis-plus-spring-boot3-starter`
- 保留 `spring-boot-starter-validation`
- 保留 `lombok`

### `product-core`

- 显式引入 Web、Security、MyBatis-Plus
- 引入 `fastjson2`、`jjwt`、`UserAgentUtils`
- 保留 `lombok`

### `product-framework`

- 显式引入 Web、Security、AOP
- 引入 Druid、MyBatis-Plus、MyBatis-Plus JSqlParser
- 引入通用工具依赖

### `product-auth`

- 显式引入 Web、Security、Validation
- 引入 MyBatis-Plus、`fastjson2`、`kaptcha`
- 保留 `lombok`

### `product-server`

- 仅保留应用组装所需依赖
- 运行时引入 MySQL 驱动
- 测试依赖保留 `spring-boot-starter-test` 和 `spring-security-test`

### `product-become`

- 显式引入生成器所需的 Web、Security、Validation、Druid、MyBatis-Plus、Velocity 等依赖

## 后续建议

- 如果希望 `product-become` 不参与常规构建，可考虑增加 Maven profile
- 若依赖进一步扩展，建议继续在模块内部声明，减少父 POM 的耦合

