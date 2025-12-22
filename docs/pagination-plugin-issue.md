## MyBatis-Plus 分页 total 为 0 的修复记录（2025-12-22）

### 现象
- 调用 `/system/dict/type/list` 等分页接口时，SQL 日志只打印一条 `SELECT ... FROM sys_dict_type`，没有自动的 `COUNT(*)`。
- 接口返回的 `TableDataInfo.total` 为 0，`rows` 列表正常。

### 根因
- 项目自定义了 `MybatisSqlSessionFactoryBean`（`product-framework/src/main/java/com/product/config/MybatisConfig.java`），但未把 `MybatisPlusInterceptor` 注册到 `SqlSessionFactory`。  
- 结果 MyBatis-Plus 的 `PaginationInnerInterceptor` 未生效，`page()` 退化为普通查询，不会追加分页与 count 逻辑。

### 修复方案
在 `sqlSessionFactory` 方法中显式注入并注册分页拦截器：

```java
// MybatisConfig.java
@Bean
public SqlSessionFactory sqlSessionFactory(DataSource dataSource,
                                           MybatisPlusInterceptor mybatisPlusInterceptor) throws Exception {
    ...
    sessionFactory.setPlugins(new Interceptor[]{mybatisPlusInterceptor}); // 确保分页插件生效
    return sessionFactory.getObject();
}
```

### 验证步骤
1. 重新启动后端（如 `mvn -pl product-server -DskipTests spring-boot:run`）。
2. 请求 `/system/dict/type/list`：
   - 日志应出现两条 SQL：`SELECT ... LIMIT ...` 与对应的 `COUNT(*)`。
   - 响应中的 `total` 与数据库记录数一致。

### 影响范围
- 所有依赖 MyBatis-Plus 分页的接口；未注册插件会导致 total 为 0 且可能全量返回数据。

### 备注
- 如新增其它 MP 插件，请统一在此处通过 `setPlugins` 注册，保持插件顺序：通用插件在前，分页插件靠后。
