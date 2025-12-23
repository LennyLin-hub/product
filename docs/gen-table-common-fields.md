## 代码生成表（gen_table & gen_table_column）常用字段速查

> 目的：说明 `GenTableMapper.xml`/`GenTableColumnMapper.xml` 里出现的字段来源与用途，避免误以为它们必须存在于业务库中的真实业务表。

### 1. 元数据主表 `gen_table`（记录每个业务表的生成配置）
| 字段 | 作用 | 备注 |
| --- | --- | --- |
| `table_id` | 主键 | |
| `table_name` | 业务表名 | |
| `table_comment` | 业务表描述 | |
| `sub_table_name` | 关联子表名 | 主子表场景 |
| `sub_table_fk_name` | 子表外键名 | 主子表场景 |
| `class_name` | 生成的实体名 | PascalCase |
| `tpl_category` | 模板类型 | `crud`/`tree`/`sub` |
| `tpl_web_type` | 前端模板 | element-ui / element-plus |
| `package_name` | 代码生成包路径 | |
| `module_name` | 生成模块名 | |
| `business_name` | 业务名 | |
| `function_name` | 功能名 | |
| `function_author` | 作者 | |
| `gen_type` | 生成方式 | 0 zip / 1 自定义路径 |
| `gen_path` | 生成路径 | 为空默认项目根 |
| `options` | 额外 JSON 配置 | 树/菜单等扩展参数 |
| `remark` | 备注 | |
| `create_time` / `update_time` | 元数据记录的创建/更新时间 | 继承自 `BaseEntity` |

### 2. 元数据明细表 `gen_table_column`（记录业务表的列定义）
| 字段 | 作用 |
| --- | --- |
| `column_id` | 主键 |
| `table_id` | 关联 `gen_table.table_id` |
| `column_name` | 列名 |
| `column_comment` | 列描述 |
| `column_type` | 数据库列类型（含长度，如 varchar(64)） |
| `java_type` | 生成的 Java 类型 |
| `java_field` | 生成的 Java 字段名 |
| `is_pk` | 是否主键 |
| `is_increment` | 是否自增 |
| `is_required` | 是否必填 |
| `is_insert` | 是否参与插入 |
| `is_edit` | 是否参与编辑 |
| `is_list` | 是否出现在列表 |
| `is_query` | 是否可查询 |
| `query_type` | 查询方式（EQ/LIKE/…） |
| `html_type` | 前端控件类型 |
| `dict_type` | 关联字典类型 |
| `sort` | 显示/生成顺序 |
| `create_time` / `update_time` | 元数据记录时间 |

### 3. “公共/超类”字段说明
- **BaseEntity 通用属性**（存在于所有生成元数据实体，但不一定在业务表中）：`createTime`, `updateTime`, `searchValue`, `params`（其中 `params` 用作前端查询扩展条件，不映射数据库列）。
- **超类字段（在生成逻辑中视为通用字段）**：`GenConstants.BASE_ENTITY` 与 `GenConstants.TREE_ENTITY` 定义的字段，如 `createBy`, `createTime`, `updateBy`, `updateTime`, `remark`, `parentId`, `orderNum`, `ancestors`, `children`。这些字段常在模板、校验或忽略列表里出现，用于控制哪些列需要生成。
- **信息_schema 导入字段**：`GenTableColumnMapper.selectDbTableColumnsByName` 从 `information_schema.columns` 取的列（如 `is_nullable`, `column_key`, `extra`）只用于推导 `is_required`、`is_pk`、`is_increment`，不会保存到实体。

### 4. 为什么 Mapper 里会看到“不是业务表的字段”
- `gen_table`/`gen_table_column` 是**代码生成器的元数据表**，并非你的业务表；Mapper SELECT 列表面向这两张表。
- 部分字段（如 `options`、`tpl_web_type`、`gen_path`）仅为生成配置，业务库无需具备对应列。
- BaseEntity/超类字段出现在实体或模板判断中，是为了统一处理审计字段或树形结构，实际业务表可按需选择是否存在。

### 5. 参考位置
- 元数据表实体：`product-domain/src/main/java/com/product/entity/GenTable*.java`
- 常量定义：`product-common/src/main/java/com/product/constant/GenConstants.java`
- Mapper XML：`product-become/src/main/resources/mapper/GenTableMapper.xml`、`GenTableColumnMapper.xml`

使用代码生成时，确认你的实际业务表至少包含需要生成的核心业务列，审计/树形等公共列可选；缺失不会影响元数据表结构，但生成的代码与前端页面会依据这些标志位进行渲染与校验。 
