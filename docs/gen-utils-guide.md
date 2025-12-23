## GenUtils 代码生成规则指南（基于 product-become/src/main/java/com/product/util/GenUtils.java）

### 1. 表级初始化 `initTable(GenTable genTable, String operName)`
- **ClassName**：将表名转为驼峰并首字母大写；可按 `GenConfig.getAutoRemovePre()` + `GenConfig.getTablePrefix()` 去除前缀（支持多前缀逗号分隔），如 `sys_user` → `User`。
- **包/模块/业务命名**：  
  - `packageName` 取自 `GenConfig.getPackageName()`；  
  - `moduleName` 为包名最后一段（例：`com.product.domain` → `domain`）；  
  - `businessName` 为表名最后一个下划线段（例：`sys_user` → `user`）。
- **功能描述/作者**：`functionName` 去掉表注释中的“表”字；`functionAuthor` 来源 `GenConfig.getAuthor()`。

### 2. 列级初始化 `initColumnField(GenTableColumn column, GenTable table)`
- **基本属性**：绑定 `tableId`；`javaField` 由列名转驼峰；默认 `javaType=String`，`queryType=EQ`。
- **类型推导**（参考 `GenConstants`）：
  - 文本/字符串：`column_type` 属于 `COLUMNTYPE_STR`/`COLUMNTYPE_TEXT`，长度 ≥ 500 或文本型 → `htmlType=textarea`，否则 `input`。
  - 时间型：`javaType=Date`，`htmlType=datetime`。
  - 数字型：`htmlType=input`；若有小数位 `>0` → `javaType=BigDecimal`；否则长度 ≤10 → `Integer`；超出 → `Long`。
- **字段标志位默认值**：  
  - `isInsert=1`（全部可插入）；  
  - `isEdit/isList/isQuery=1`，但排除主键及配置的忽略名单：`COLUMNNAME_NOT_EDIT/LIST/QUERY`。
- **查询/控件语义基于列名后缀**：  
  - 以 `name` 结尾 → `queryType=LIKE`；  
  - `status` → 单选框 `htmlType=radio`；  
  - `type` / `sex` → 下拉 `htmlType=select`；  
  - `image` → `imageUpload`；  
  - `file` → `fileUpload`；  
  - `content` → `editor`。

### 3. 辅助工具
- `convertClassName`：按前缀配置批量去前缀后转驼峰。
- `getDbType` / `getColumnLength`：从 `column_type` 中拆出基础类型与长度（如 `varchar(64)` → `varchar`, 长度 64）。
- `arraysContains`：包装 `Arrays.asList().contains`。
- `replaceText`：清理表注释中的“表”字样。

### 4. 生成流程中的作用
1) `importGenTable` 时从 information_schema 读列，调用 `initTable` + `initColumnField` 完成元数据填充。  
2) 这些元数据驱动 Velocity 模板生成：实体类型、前端控件、校验标志、查询条件等均取决于上述规则。  
3) 修改生成策略时，优先调整 `GenConstants`（类型/忽略名单）和 `GenConfig`（包名、前缀、作者等），再视需要微调 `initColumnField` 的后缀规则。

### 5. 相关配置与引用
- 代码位置：`product-become/src/main/java/com/product/util/GenUtils.java`  
- 配置来源：`GenConfig`（包名、作者、前缀、是否去前缀）。  
- 常量来源：`GenConstants`（字段类型分组、控件/查询类型、忽略列表等）。  
- 元数据实体：`GenTable`、`GenTableColumn`（`product-domain/src/main/java/com/product/entity/`）。
