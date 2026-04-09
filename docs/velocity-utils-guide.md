# VelocityUtils 模板变量与生成输出指南

本文档基于 `product-become/src/main/java/com/product/become/util/VelocityUtils.java`，用于说明模板上下文、输出文件与辅助方法。

## 1. `prepareContext` 的可用变量

### 基础信息

- `tplCategory`
- `tableName`
- `functionName`
- `ClassName` / `className`
- `moduleName`
- `BusinessName` / `businessName`
- `packageName`
- `basePackage`
- `author`
- `datetime`

### 权限、字典与导入

- `permissionPrefix`
- `dicts`
- `importList`

### 结构引用

- `pkColumn`
- `columns`
- `table`
- `subTable`
- `subImportList`

### 树、菜单与子表上下文

- `parentMenuId`
- `treeCode`
- `treeParentCode`
- `treeName`
- `expandColumn`
- `subTableName`
- `subTableFkName`
- `subTableFkClassName`
- `subClassName`

### 工具注入

- `typeUtils`

## 2. 模板清单 `getTemplateList(tplCategory, tplWebType)`

- Java：`vm/java`
- XML：`vm/xml/mapper.xml.vm`
- SQL：`vm/sql/sql.vm`
- 前端 API：`vm/js/api.js.vm`
- Vue 页面：`vm/vue/v3/index.vue.vm`
- 树形页面：`vm/vue/v3/index-tree.vue.vm`
- 子表场景：额外生成 `sub-domain.java.vm`

## 3. 生成文件命名 `getFileName(template, genTable)`

- Java：输出到 `main/java/{package}/...`
- XML：输出到 `main/resources/mapper/{module}/{ClassName}Mapper.xml`
- 前端 API：输出到 `vue/api/{module}/{business}.js`
- 前端页面：输出到 `vue/views/{module}/{business}/index.vue`
- 菜单脚本：输出为 `{businessName}Menu.sql`

## 4. 导入包 `getImportList`

- `Date` 和 `JsonFormat`
- `BigDecimal`
- 子表场景下的 `java.util.List`

## 5. 字典收集 `getDicts` / `addDicts`

- 遍历主表和子表列
- 对 `select`、`radio`、`checkbox` 类型且设置了 `dictType` 的字段进行收集
- 结果用于生成前端字典依赖

## 6. 权限前缀

- `getPermissionPrefix` 的组合规则为 `{moduleName}:{businessName}`

## 7. 类型与实体全名工具

- `getFullJavaType`：简写类型转完整 Java 类型
- `getFullEntityName`：必要时自动补全 `.domain`

## 8. 树与菜单辅助

- `getParentMenuId`
- `getTreecode`
- `getTreeParentCode`
- `getTreeName`
- `getExpandColumn`

## 9. 子表辅助

- `setSubVelocityContext`：为子表外键生成驼峰与首字母小写形式

## 10. 使用建议

- 新增模板变量时，优先在 `prepareContext` 统一补充
- 若需区分不同前端方案，可扩展 `getTemplateList`
- 模板中的权限、树和菜单信息依赖 `gen_table.options` 的 JSON 配置

