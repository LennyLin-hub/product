# 代码生成表查询 SQL 语法错误修复

记录代码生成模块中一次典型的 SQL 拼接错误，便于后续排查类似问题。

## 现象

- 访问 `/tool/gen/{tableId}` 时抛出 `BadSqlGrammarException`
- MySQL 报错位置指向 `t.remark` 与 `c.column_id` 之间缺少逗号

## 根因

- `product-become/src/main/resources/mapper/GenTableMapper.xml` 中的多个查询语句，在 `t.remark` 后漏写了分隔逗号

## 修复

- 为相关 `SELECT` 列表补上逗号
- 修复后 SQL 片段应为 `... t.options, t.remark, c.column_id ...`

## 验证步骤

1. 重启后端或重新加载 Mapper
2. 再次访问 `/tool/gen/{tableId}`
3. 确认不再出现 SQL 语法错误

## 影响范围

- 仅影响代码生成模块的表与列联合查询
- 不影响其他业务查询

