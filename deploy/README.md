# 部署文档总索引

这里汇总仓库中的容器化部署说明，当前主要覆盖 MySQL 与 ELK。

## 一、数据库部署

- [MySQL Docker 迁移说明](./mysql/README.md)
- [MySQL 环境变量示例](./mysql/.env)

## 二、日志部署

- [ELK 日志系统部署指南](./elk/README.md)

## 三、阅读建议

1. 先读 `deploy/mysql/README.md`，确认数据库容器化切换方式。
2. 再读 `deploy/elk/README.md`，确认日志采集与索引配置。
3. 如果要本地一键启动，可把两个目录的 compose 文件作为基础再做顶层聚合。

