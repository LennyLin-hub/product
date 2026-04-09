# MySQL Docker 迁移说明

## 目标

将当前本机运行的 MySQL 迁移到 Docker 管理，方式与 `deploy/elk/docker-compose.yml` 保持一致，但建议单独维护在 `deploy/mysql` 下，避免数据库和日志系统的生命周期强耦合。

## 可行性评估

可以迁移，且风险可控。

- 当前后端通过 `product-server/src/main/resources/application-druid.yml` 连接 `jdbc:mysql://localhost:3306/product`。
- 如果 MySQL 容器对宿主机暴露 `3306` 端口，后端继续运行在宿主机上时，只需要覆盖连接参数，无需改 Java 代码。
- 如果后续后端也容器化，连接地址从 `localhost` 改成 compose 服务名 `mysql` 即可。

## 目录约定

- `docker-compose.yml`: MySQL 容器编排
- `init/`: 初始化 SQL。仅在空数据目录首次启动时自动执行
- `conf.d/`: 额外 MySQL 配置
- `backup/`: 导出备份文件存放目录

## 迁移策略

建议采用“并行准备 + 一次切换”的方式。

1. 备份当前本地数据库

```bash
mysqldump -h 127.0.0.1 -P 3306 -uroot -p \
  --single-transaction \
  --routines \
  --triggers \
  --events \
  product > deploy/mysql/backup/product-$(date +%F-%H%M%S).sql
```

2. 准备容器参数

在 `deploy/mysql` 目录创建 `.env`，例如：

```dotenv
TZ=Asia/Taipei
MYSQL_PORT=3306
MYSQL_ROOT_PASSWORD=change-me-in-env
MYSQL_DATABASE=product
MYSQL_USER=product
MYSQL_PASSWORD=change-me-in-env
```

3. 启动 MySQL 容器

```bash
docker compose -f deploy/mysql/docker-compose.yml up -d
```

4. 导入备份数据

```bash
docker exec -i product-mysql \
  mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" product < deploy/mysql/backup/your-backup.sql
```

5. 校验容器内数据

```bash
docker exec -it product-mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" -e "SHOW DATABASES;"
docker exec -it product-mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" -D product -e "SHOW TABLES;"
```

6. 切换应用连接

后端运行在宿主机时，推荐用环境变量覆盖：

```bash
export SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3306/product?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia%2FTaipei'
export SPRING_DATASOURCE_USERNAME='root'
export SPRING_DATASOURCE_PASSWORD='change-me-in-env'
```

然后启动后端：

```bash
mvn spring-boot:run -pl product-server -Dspring.profiles.active=dev
```

## 切换窗口建议

- 业务低峰期执行
- 先做一次全量导出导入
- 切换前短暂停写，做最后一次增量差异补齐
- 校验核心表数量、关键业务数据、登录和写接口
- 确认无误后再停止宿主机上的旧 MySQL

## 风险点

- `init/` 目录只会在 MySQL 数据目录为空时执行，已有数据卷不会重复初始化。
- 宿主机如果已有本地 MySQL 占用 `3306`，需要先停掉旧实例，或者把容器映射到其他端口，例如 `3307:3306`。
- 仓库中的数据库密码目前是明文配置，建议迁移时同步改为 `.env` 或运行时环境变量，不要继续写死在源码里。
- `lower_case_table_names=1` 需要在空实例初始化时生效，已存在数据目录时再改可能无效。

## 是否要并入 ELK 的 compose

技术上可以，但默认不建议。

- 适合并入同一个 compose 的情况：开发机一键启动完整依赖栈。
- 不适合并入同一个 compose 的情况：数据库和日志系统变更频率、资源占用、恢复策略完全不同。

更稳妥的做法是：

- `deploy/mysql/docker-compose.yml` 单独管理数据库
- `deploy/elk/docker-compose.yml` 继续单独管理日志
- 需要“一键启动”时，再增加一个顶层 compose 统一引用
