## 代码生成表查询 SQL 语法错误修复（2025-12-23）

### 现象
- 访问 `/tool/gen/{tableId}` 报 `BadSqlGrammarException`，MySQL 报错指向 `... t.options, t.remark       c.column_id ...` 处缺少逗号。

### 根因
- `product-become/src/main/resources/mapper/GenTableMapper.xml` 中三个查询（按 tableId、按 tableName、查询全部）在选择列时，`t.remark` 与后续 `c.column_id` 之间缺少分隔逗号，导致拼出的 SQL 语法错误。

### 修复
- 为三处 SELECT 列表补上逗号：
  - `... t.options, t.remark, c.column_id ...`

### 验证步骤
1. 重启后端或热加载 Mapper。
2. 再次请求 `/tool/gen/{tableId}`，SQL 应正常执行，不再出现语法错误。

### 影响范围
- 仅影响代码生成模块的表与列联合查询，不涉及其他业务查询。
