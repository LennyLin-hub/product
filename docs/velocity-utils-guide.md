## VelocityUtils 模板变量与生成输出指南（基于 product-become/src/main/java/com/product/util/VelocityUtils.java）

### 1) prepareContext：模板可用变量一览
- **基础信息**：`tplCategory`（crud/tree/sub）、`tableName`、`functionName`（为空时提示占位）、`ClassName`/`className`、`moduleName`、`BusinessName`/`businessName`、`packageName`、`basePackage`（去掉末级包）、`author`、`datetime`。  
- **权限/字典/导入**：`permissionPrefix`（`moduleName:businessName`）、`dicts`（需要的字典类型集合字符串）、`importList`（生成 Java 需导入的包，含日期/BigDecimal/List）。  
- **结构引用**：`pkColumn`、`columns`、`table`、`subTable`（主子表时）、`subImportList`。  
- **树/菜单/子表上下文**：  
  - `parentMenuId`（默认 3，可由 `options.parentMenuId` 覆盖）。  
  - 树模板：`treeCode`、`treeParentCode`、`treeName`、`expandColumn`，以及原值 key `tree_parent_code`、`tree_name`（兼容模板占位）。  
  - 子表模板：`subTableName`、`subTableFkName`、`subTableFkClassName`/`subTableFkclassName`、`subClassName`/`subclassName`、`subImportList`。  
- **工具注入**：`typeUtils`（实例本类，可在模板中调用静态逻辑如 `getFullJavaType`）。

### 2) 模板清单 `getTemplateList(tplCategory, tplWebType)`
- 通用输出：`vm/java`(domain/mapper/service/serviceImpl/controller)、`vm/xml/mapper.xml.vm`、`vm/sql/sql.vm`、`vm/js/api.js.vm`。  
- 前端页面：默认使用 `vm/vue/v3/index.vue.vm`；`tree` 用 `index-tree.vue.vm`；`sub` 额外生成 `sub-domain.java.vm`。  
- 目前前端类型参数 `tplWebType` 未分支，仅固定 v3 目录（如需区分 element-plus 可在此扩展）。

### 3) 生成文件命名 `getFileName(template, genTable)`
- Java 代码根：`main/java/{package}/...`；Mapper XML：`main/resources/mapper/{module}/{ClassName}Mapper.xml`；前端：`vue/api/{module}/{business}.js` 与 `vue/views/{module}/{business}/index.vue`；SQL 菜单脚本：`{businessName}Menu.sql`。  
- 子表模板 `sub-domain.java.vm` 路径与主域相同，文件名取子表类名。

### 4) 导入包 `getImportList`
- 根据列类型追加：`Date` + `JsonFormat`；`BigDecimal`；若存在子表则加 `java.util.List`。忽略超级字段（BaseEntity/TreeEntity）。

### 5) 字典收集 `getDicts` / `addDicts`
- 遍历主表与子表列，凡 `htmlType` 属于 `select/radio/checkbox` 且设置了 `dictType`，收集为 `'dictType'` 形式的去重列表，模板可直接生成前端字典依赖。

### 6) 权限前缀 `getPermissionPrefix`
- 组合规则：`{moduleName}:{businessName}`，供前端/后端权限标识复用。

### 7) 类型/实体全名工具
- `getFullJavaType`：把简写类型映射到完整类名（缺省返回 `java.lang.Object` 或原值）。  
- `getFullEntityName`：当包名未以 `.domain` 结尾时自动补上 `.domain`。

### 8) 树 & 菜单辅助
- `getParentMenuId`：从 `options` 中取 `parentMenuId`，缺省 `"3"`。  
- `getTreecode`/`getTreeParentCode`/`getTreeName`：从 `options` 取并转驼峰；缺省空字符串。  
- `getExpandColumn`：按列表显示的列顺序定位树形表格的展开列序号。

### 9) 子表辅助
- `setSubVelocityContext` 提供外键名的驼峰/首字母小写形式，便于模板生成关联属性与查询。

### 10) 使用建议
- 若需切换前端技术栈（如区分 element-ui / element-plus），扩展 `getTemplateList` 选择不同模板目录。  
- 权限/菜单/树配置依赖 `gen_table.options` 中的 JSON（字段名见 `GenConstants`）；调整前请确保导入的元数据已包含这些选项。  
- 模板新增占位变量时，可在 `prepareContext` 中补充，保持集中管理。
