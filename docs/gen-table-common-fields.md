# 代码生成表常用字段速查

本文档说明 `gen_table` 和 `gen_table_column` 两张元数据表在代码生成器中的作用，避免将它们误认为业务表字段。

## 1. `gen_table` 主表

| 字段 | 作用 | 备注 |
| --- | --- | --- |
| `table_id` | 主键 | |
| `table_name` | 业务表名 | |
| `table_comment` | 业务表描述 | |
| `sub_table_name` | 关联子表名 | 主子表场景 |
| `sub_table_fk_name` | 子表外键名 | 主子表场景 |
| `class_name` | 生成的实体名 | PascalCase |
| `tpl_category` | 模板类型 | `crud` / `tree` / `sub` |
| `tpl_web_type` | 前端模板 | 当前主要用于前端模板选择 |
| `package_name` | 代码生成包路径 | |
| `module_name` | 生成模块名 | |
| `business_name` | 业务名 | |
| `function_name` | 功能名 | |
| `function_author` | 作者 | |
| `gen_type` | 生成方式 | 0 zip / 1 自定义路径 |
| `gen_path` | 生成路径 | 为空时默认项目根 |
| `options` | 扩展 JSON 配置 | 树、菜单等参数 |
| `remark` | 备注 | |
| `create_time` / `update_time` | 记录创建/更新时间 | 继承自 `BaseEntity` |

## 2. `gen_table_column` 明细表

| 字段 | 作用 |
| --- | --- |
| `column_id` | 主键 |
| `table_id` | 关联 `gen_table.table_id` |
| `column_name` | 列名 |
| `column_comment` | 列描述 |
| `column_type` | 数据库列类型 |
| `java_type` | 生成的 Java 类型 |
| `java_field` | 生成的 Java 字段名 |
| `is_pk` | 是否主键 |
| `is_increment` | 是否自增 |
| `is_required` | 是否必填 |
| `is_insert` | 是否参与插入 |
| `is_edit` | 是否参与编辑 |
| `is_list` | 是否出现在列表 |
| `is_query` | 是否可查询 |
| `query_type` | 查询方式 |
| `html_type` | 前端控件类型 |
| `dict_type` | 关联字典类型 |
| `sort` | 显示顺序 |
| `create_time` / `update_time` | 记录创建/更新时间 |

## 3. 公共字段说明

- `BaseEntity` 中的审计字段属于通用字段，不一定在每张业务表中都存在
- `GenConstants` 中定义的超类字段用于模板判断和字段忽略
- 从 `information_schema` 读取的列信息主要用于推导主键、自增和必填状态

## 4. 为什么 Mapper 中会看到“非业务表字段”

- 这两张表本身是代码生成器的元数据表
- `options`、`tpl_web_type`、`gen_path` 等字段属于生成配置
- 生成器会根据这些字段决定最终输出什么代码和页面

## 5. 参考位置

- 元数据实体：`product-domain/src/main/java/com/product/domain/entity/GenTable.java`
- 元数据实体：`product-domain/src/main/java/com/product/domain/entity/GenTableColumn.java`
- 常量定义：`product-common/src/main/java/com/product/common/constant/GenConstants.java`
- Mapper XML：`product-become/src/main/resources/mapper/GenTableMapper.xml`
- Mapper XML：`product-become/src/main/resources/mapper/GenTableColumnMapper.xml`

