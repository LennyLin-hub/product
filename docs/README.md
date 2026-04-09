# 项目文档总索引

这里汇总仓库中的项目说明、排程设计、代码生成、问题修复与优化记录，便于按主题快速查阅。

## 一、业务与方案文档

| 文档 | 内容 |
| --- | --- |
| [注塑件加工流程.md](./注塑件加工流程.md) | 注塑工厂从接单到出货的简化生产流程 |
| [生产计划与排程计划系统需求说明书.md](./生产计划与排程计划系统需求说明书.md) | 需求说明书，当前为待补充骨架 |
| [关键配置流程.md](./关键配置流程.md) | 按“计划-派工-执行-异常-完工”组织的配置与数据模型说明 |
| [生产排程解决方案落地现状.md](./生产排程解决方案落地现状.md) | 基于当前代码实现的落地情况、已完成能力与缺口 |
| [自动排程伪代码.md](./自动排程伪代码.md) | 自动排程算法的伪代码描述 |
| [TODO.md](./TODO.md) | 项目优化记录、开发任务表与后续 TODO |

## 二、代码生成与工具文档

| 文档 | 内容 |
| --- | --- |
| [gen-utils-guide.md](./gen-utils-guide.md) | `GenUtils` 的代码生成规则说明 |
| [velocity-utils-guide.md](./velocity-utils-guide.md) | `VelocityUtils` 模板变量与生成输出说明 |
| [gen-table-common-fields.md](./gen-table-common-fields.md) | 代码生成表字段的速查说明 |
| [gen-table-sql-fix.md](./gen-table-sql-fix.md) | 代码生成表查询 SQL 语法修复记录 |

## 三、框架与依赖文档

| 文档 | 内容 |
| --- | --- |
| [pagination-plugin-issue.md](./pagination-plugin-issue.md) | MyBatis-Plus 分页 total 为 0 的修复记录 |
| [dependency-tightening.md](./dependency-tightening.md) | 父 POM 与模块依赖收敛说明 |

## 四、阅读建议

1. 如果想了解业务背景，先读 `注塑件加工流程.md` 和 `关键配置流程.md`。
2. 如果要看需求说明书，先读 `生产计划与排程计划系统需求说明书.md`。
3. 如果想理解当前实现进度，先读 `生产排程解决方案落地现状.md`。
4. 如果要看当前任务清单，先读 `TODO.md`。
5. 如果要修改代码生成器，先读 `gen-utils-guide.md` 和 `velocity-utils-guide.md`。
6. 如果遇到分页或依赖问题，先读 `pagination-plugin-issue.md` 和 `dependency-tightening.md`。
7. 如果要看容器化部署，去读 `../deploy/README.md`。
