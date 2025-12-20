# Dependency Tightening Notes

Intent: move shared dependencies out of the parent POM and declare them in the modules that actually use them.

## Parent POM (product)
- Removed the top-level `<dependencies>` block so child modules do not inherit runtime dependencies implicitly.
- Kept versions in `<dependencyManagement>` and added managed versions for:
  - `com.baomidou:mybatis-plus-jsqlparser`
  - `org.apache.poi:poi-ooxml`
  - `eu.bitwalker:UserAgentUtils`
  - `com.aliyun.oss:aliyun-sdk-oss`
  - `org.apache.velocity:velocity-engine-core`

## Module dependency changes

### product-common
Added explicit dependencies for code used in `product-common`:
- Spring: `spring-boot-starter-web`, `spring-boot-starter-aop`, `spring-boot-starter-validation`, `spring-boot-starter-data-redis`
- MyBatis-Plus: `mybatis-plus-spring-boot3-starter`
- Utilities: `commons-lang3`, `commons-io`, `lombok`
- JSON / JWT / Excel / OSS: `fastjson2`, `jjwt`, `poi-ooxml`, `aliyun-sdk-oss`
- Kept `jakarta.servlet-api` as `provided`

### product-domain
- Removed unused `fastjson2`.
- Kept explicit `mybatis-plus-spring-boot3-starter`, `spring-boot-starter-validation`, and `lombok`.

### product-core
Added explicit dependencies for framework-level utilities and security helpers:
- Spring: `spring-boot-starter-web`, `spring-boot-starter-security`
- MyBatis-Plus: `mybatis-plus-spring-boot3-starter`
- JSON / JWT / UA: `fastjson2`, `jjwt`, `UserAgentUtils`
- `lombok`

### product-framework
Added explicit dependencies for framework configuration and infrastructure:
- Spring: `spring-boot-starter-web`, `spring-boot-starter-security`, `spring-boot-starter-aop`
- Data / ORM: `druid-spring-boot-3-starter`, `mybatis-plus-spring-boot3-starter`, `mybatis-plus-jsqlparser`
- Utilities: `commons-lang3`, `lombok`

### product-auth
Added explicit dependencies for security, MVC, and mapper layer:
- Spring: `spring-boot-starter-web`, `spring-boot-starter-security`, `spring-boot-starter-validation`
- MyBatis-Plus: `mybatis-plus-spring-boot3-starter`
- JSON / Captcha: `fastjson2`, `kaptcha`
- `lombok`

### product-server
- Reduced to application assembly dependencies: `product-framework`, `product-auth`, `product-become`, `spring-boot-starter`, runtime `mysql-connector-j`, and test deps `spring-boot-starter-test` + `spring-security-test`.

### product-become
- Added explicit dependencies required by code generation (even though the module is not for deployment):
  - Spring: `spring-boot-starter-web`, `spring-boot-starter-security`, `spring-boot-starter-validation`
  - Data / ORM: `druid-spring-boot-3-starter`, `mybatis-plus-spring-boot3-starter`
  - JSON / Templates / Utils: `fastjson2`, `velocity-engine-core`, `commons-io`, `commons-lang3`, `lombok`

## Follow-up (optional)
- If you want to keep `product-become` out of normal builds, we can add a Maven profile or remove it from the default module list.
