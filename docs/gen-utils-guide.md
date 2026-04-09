# GenUtils 代码生成规则指南

本文档基于 `product-become/src/main/java/com/product/become/util/GenUtils.java`，用于说明代码生成器在表元数据初始化阶段的规则。

## 适用范围

- 代码生成表导入
- 字段类型推导
- 前端控件和查询条件生成
- 模板元数据填充

## 1. 表级初始化 `initTable(GenTable genTable, String operName)`

- `ClassName`：表名转驼峰并首字母大写，可按配置去除前缀
- `packageName`：取自 `GenConfig.getPackageName()`
- `moduleName`：包名最后一段
- `businessName`：表名最后一个下划线段
- `functionName`：从表注释中去除“表”字
- `functionAuthor`：取自 `GenConfig.getAuthor()`

## 2. 列级初始化 `initColumnField(GenTableColumn column, GenTable table)`

- `javaField`：列名转驼峰
- `javaType`：默认 `String`
- `queryType`：默认 `EQ`
- `isInsert`：默认可插入
- `isEdit/isList/isQuery`：默认开启，主键和忽略字段除外

### 类型推导规则

- 字符串/文本：长度较大或文本型时使用 `textarea`，否则使用 `input`
- 时间型：`javaType=Date`，`htmlType=datetime`
- 数字型：优先根据精度判断 `BigDecimal`、`Integer`、`Long`

### 常见后缀规则

- `name`：`queryType=LIKE`
- `status`：`htmlType=radio`
- `type` / `sex`：`htmlType=select`
- `image`：`imageUpload`
- `file`：`fileUpload`
- `content`：`editor`

## 3. 辅助方法

- `convertClassName`：去前缀并转驼峰
- `getDbType`：提取字段基础类型
- `getColumnLength`：提取字段长度
- `arraysContains`：判断数组是否包含元素
- `replaceText`：清理表注释中的“表”字

## 4. 生成流程中的作用

1. 导入表时读取 `information_schema`
2. 调用 `initTable` 和 `initColumnField` 完成元数据填充
3. 元数据进入 Velocity 模板，驱动 Java、XML、前端页面生成

## 5. 相关引用

- 代码位置：`product-become/src/main/java/com/product/become/util/GenUtils.java`
- 配置来源：`product-become/src/main/java/com/product/become/config/GenConfig.java`
- 常量来源：`product-common/src/main/java/com/product/common/constant/GenConstants.java`
- 元数据实体：`product-domain/src/main/java/com/product/domain/entity/GenTable.java`
- 元数据实体：`product-domain/src/main/java/com/product/domain/entity/GenTableColumn.java`

