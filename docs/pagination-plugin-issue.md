# MyBatis-Plus 分页 `total` 为 0 的修复记录

本文档记录一次分页插件未生效导致 `total` 统计异常的问题。

## 现象

- 调用 `/system/dict/type/list` 等分页接口时，只看到普通 `SELECT`
- 接口返回的 `TableDataInfo.total` 为 `0`，但 `rows` 正常

## 根因

- 自定义了 `MybatisSqlSessionFactoryBean`
- 但未将 `MybatisPlusInterceptor` 注册到 `SqlSessionFactory`
- 导致 `PaginationInnerInterceptor` 未生效

## 修复方案

在 `sqlSessionFactory` 中显式注入并注册分页拦截器：

```java
@Bean
public SqlSessionFactory sqlSessionFactory(DataSource dataSource,
                                           MybatisPlusInterceptor mybatisPlusInterceptor) throws Exception {
    ...
    sessionFactory.setPlugins(new Interceptor[]{mybatisPlusInterceptor});
    return sessionFactory.getObject();
}
```

## 验证步骤

1. 重启后端
2. 请求 `/system/dict/type/list`
3. 检查日志中是否同时出现 `SELECT ... LIMIT ...` 和对应的 `COUNT(*)`

## 影响范围

- 所有依赖 MyBatis-Plus 分页的接口

## 备注

- 新增其他 MyBatis-Plus 插件时，统一通过 `setPlugins` 注册
- 插件顺序建议保持通用插件在前，分页插件靠后

