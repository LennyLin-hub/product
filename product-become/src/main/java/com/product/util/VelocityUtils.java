package com.product.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.product.constant.GenConstants;
import com.product.entity.GenTable;
import com.product.entity.GenTableColumn;
import com.product.utils.DateUtils;
import com.product.utils.StringUtils;
import org.apache.velocity.VelocityContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// Velocity模板引擎工具类
///
/// 核心功能：为代码生成器准备Velocity模板所需的上下文变量，并生成对应的文件路径
///
/// ## 主要功能模块：
///
///     - 1. 上下文准备：prepareContext() - 准备所有模板变量
///     - 2. 模板管理：getTemplateList() - 获取需要生成的模板列表
///     - 3. 文件生成：getFileName() - 根据模板生成目标文件路径
///     - 4. 包管理：getImportList() - 收集需要导入的Java包
///     - 5. 字典管理：getDicts() - 收集页面需要的字典类型
///
/// ## 支持的代码生成模板类型：
///
///     - 单表CRUD（TPL_CRUD）：标准的增删改查功能
///     - 树表结构（TPL_TREE）：支持树形结构的表，如部门、菜单
///     - 主子表（TPL_SUB）：主表+明细表的关联结构
///
/// ## 生成的文件列表：
/// <pre>
/// Java后端：
///   - domain/{ClassName}.java          - 实体类
///   - mapper/{ClassName}Mapper.java    - Mapper接口
///   - service/I{ClassName}Service.java - Service接口
///   - service/impl/{ClassName}ServiceImpl.java - Service实现
///   - controller/{ClassName}Controller.java - Controller控制器
///   - resources/mapper/{ClassName}Mapper.xml - MyBatis映射文件
///   - {BusinessName}Menu.sql           - 菜单SQL
///
/// 前端Vue3：
///   - api/{ModuleName}/{BusinessName}.js      - API接口定义
///   - views/{ModuleName}/{BusinessName}/index.vue - 页面组件
/// </pre>
/// ## 使用示例：
/// <pre>
/// // 1. 准备模板上下文
/// GenTable genTable = new GenTable();
/// genTable.setTableName("sys_user");
/// genTable.setClassName("SysUser");
/// genTable.setModuleName("system");
/// genTable.setBusinessName("user");
/// genTable.setPackageName("com.product.system");
///
/// VelocityContext context = VelocityUtils.prepareContext(genTable);
///
/// // 2. 获取模板列表
/// List&lt;String&gt; templates = VelocityUtils.getTemplateList("crud", "vue");
///
/// // 3. 生成文件路径
/// for (String template : templates) {
///     String fileName = VelocityUtils.getFileName(template, genTable);
///     System.out.println("生成文件: " + fileName);
///     // 输出: main/java/com/product/system/domain/SysUser.java
///     // 输出: main/java/com/product/system/mapper/SysUserMapper.java
///     // ...
/// }
///
/// // 4. 在Velocity模板中可用的变量
/// // $ClassName        - 类名（SysUser）
/// // $className        - 类名首字母小写（sysUser）
/// // $BusinessName     - 业务名首字母大写（User）
/// // $businessName     - 业务名（user）
/// // $moduleName       - 模块名（system）
/// // $packageName      - 包名（com.product.system）
/// // $author           - 作者
/// // $datetime         - 当前日期
/// // $pkColumn         - 主键列对象
/// // $columns          - 所有列对象列表
/// // $table            - 表对象
/// // $permissionPrefix - 权限前缀（system:user）
/// </pre>
///
/// @author fast
/// @see GenTable
/// @see GenTableColumn
/// @see org.apache.velocity.VelocityContext
public class VelocityUtils
{
    /** Java源代码存放路径（相对于项目根目录） */
    private static final String PROJECT_PATH = "main/java";

    /** MyBatis XML映射文件存放路径（相对于resources目录） */
    private static final String MYBATIS_PATH = "main/resources/mapper";

    /** 默认上级菜单ID（系统工具） */
    private static final String DEFAULT_PARENT_MENU_ID = "3";

    /// 准备Velocity模板上下文
    ///
    /// 此方法是代码生成的核心，将GenTable对象中的所有属性转换为Velocity模板可用的变量
    ///
    /// ### 注入到上下文的变量列表：
    /// <table border="1">
    ///   <tr><th>变量名</th><th>说明</th><th>示例值</th></tr>
    ///   <tr><td>tplCategory</td><td>模板类型（crud/tree/sub）</td><td>"crud"</td></tr>
    ///   <tr><td>tableName</td><td>数据库表名</td><td>"sys_user"</td></tr>
    ///   <tr><td>functionName</td><td>功能名称</td><td>"用户管理"</td></tr>
    ///   <tr><td>ClassName</td><td>Java类名（大驼峰）</td><td>"SysUser"</td></tr>
    ///   <tr><td>className</td><td>对象名（小驼峰）</td><td>"sysUser"</td></tr>
    ///   <tr><td>moduleName</td><td>模块名</td><td>"system"</td></tr>
    ///   <tr><td>BusinessName</td><td>业务名（大驼峰）</td><td>"User"</td></tr>
    ///   <tr><td>businessName</td><td>业务名（小写）</td><td>"user"</td></tr>
    ///   <tr><td>packageName</td><td>完整包名</td><td>"com.product.system"</td></tr>
    ///   <tr><td>basePackage</td><td>基础包名</td><td>"com.product"</td></tr>
    ///   <tr><td>author</td><td>作者名</td><td>"fast"</td></tr>
    ///   <tr><td>datetime</td><td>当前日期</td><td>"2024-01-01"</td></tr>
    ///   <tr><td>pkColumn</td><td>主键列对象</td><td>GenTableColumn对象</td></tr>
    ///   <tr><td>importList</td><td>需要导入的包集合</td><td>Set&lt;String&gt;</td></tr>
    ///   <tr><td>permissionPrefix</td><td>权限前缀</td><td>"system:user"</td></tr>
    ///   <tr><td>columns</td><td>所有列对象列表</td><td>List&lt;GenTableColumn&gt;</td></tr>
    ///   <tr><td>table</td><td>表对象</td><td>GenTable对象</td></tr>
    ///   <tr><td>dicts</td><td>字典类型列表</td><td>"'sys_user_sex', 'sys_normal_disable'"</td></tr>
    ///   <tr><td>typeUtils</td><td>工具类实例</td><td>VelocityUtils对象</td></tr>
    /// </table>
    /// ### 特殊模板的额外变量：
    ///
    ///     - **树表（TPL_TREE）：**
    ///
    ///   - treeCode - 树编码字段
    ///     - treeParentCode - 父编码字段
    ///     - treeName - 树名称字段
    ///     - expandColumn - 展开按钮列序号
    ///
    ///
    ///     - **主子表（TPL_SUB）：**
    ///
    ///   - subTable - 子表对象
    ///     - subClassName - 子表类名
    ///     - subTableFkName - 子表外键字段名
    ///     - subTableFkClassName - 外键字段类名
    ///
    ///
    ///     - **菜单：**
    ///
    ///   - parentMenuId - 父菜单ID
    ///
    ///
    ///
    ///
    /// @param genTable 业务表对象，包含表结构、配置信息等
    /// @return VelocityContext 包含所有模板变量的上下文对象
    public static VelocityContext prepareContext(GenTable genTable)
    {
        // 提取常用变量，提升代码可读性
        String moduleName = genTable.getModuleName();
        String businessName = genTable.getBusinessName();
        String packageName = genTable.getPackageName();
        String tplCategory = genTable.getTplCategory();
        String functionName = genTable.getFunctionName();

        // 创建Velocity上下文对象
        VelocityContext velocityContext = new VelocityContext();

        // === 基础变量 ===
        velocityContext.put("tplCategory", genTable.getTplCategory());              // 模板类型
        velocityContext.put("tableName", genTable.getTableName());                  // 表名
        velocityContext.put("functionName", StringUtils.isNotEmpty(functionName) ? functionName : "【请填写功能名称】"); // 功能名称
        velocityContext.put("ClassName", genTable.getClassName());                  // 类名（大驼峰）
        velocityContext.put("className", StringUtils.uncapitalize(genTable.getClassName())); // 对象名（小驼峰）
        velocityContext.put("moduleName", genTable.getModuleName());                // 模块名
        velocityContext.put("BusinessName", StringUtils.capitalize(genTable.getBusinessName())); // 业务名（大驼峰）
        velocityContext.put("businessName", genTable.getBusinessName());            // 业务名（小写）
        velocityContext.put("basePackage", getPackagePrefix(packageName));          // 基础包名
        velocityContext.put("packageName", packageName);                            // 完整包名
        velocityContext.put("author", genTable.getFunctionAuthor());                // 作者
        velocityContext.put("datetime", DateUtils.getDate());                       // 当前日期

        // === 数据库相关变量 ===
        velocityContext.put("pkColumn", genTable.getPkColumn());                    // 主键列对象
        velocityContext.put("importList", getImportList(genTable));                 // 需要导入的包列表
        velocityContext.put("permissionPrefix", getPermissionPrefix(moduleName, businessName)); // 权限前缀（module:business）
        velocityContext.put("columns", genTable.getColumns());                      // 所有列对象列表
        velocityContext.put("table", genTable);                                     // 表对象本身
        velocityContext.put("dicts", getDicts(genTable));                           // 字典类型列表

        // 将工具类实例注入上下文，使模板可以调用工具方法
        // 用法示例：$typeUtils.getFullJavaType($column.javaType)
        velocityContext.put("typeUtils", new VelocityUtils());

        // === 特殊模板的额外变量 ===
        setMenuVelocityContext(velocityContext, genTable);                          // 设置菜单相关变量

        // 如果是树表，设置树形结构相关变量
        if (GenConstants.TPL_TREE.equals(tplCategory))
        {
            setTreeVelocityContext(velocityContext, genTable);
        }

        // 如果是主子表，设置子表相关变量
        if (GenConstants.TPL_SUB.equals(tplCategory))
        {
            setSubVelocityContext(velocityContext, genTable);
        }

        return velocityContext;
    }

    /// 设置菜单相关变量到上下文
    ///
    /// 用于生成菜单SQL时确定父菜单ID
    ///
    ///
    /// @param context Velocity上下文对象
    /// @param genTable 业务表对象
    public static void setMenuVelocityContext(VelocityContext context, GenTable genTable)
    {
        // 获取表配置选项（JSON格式）
        String options = genTable.getOptions();
        JSONObject paramsObj = JSON.parseObject(options);

        // 获取父菜单ID，如果未配置则使用默认值（3-系统工具）
        String parentMenuId = getParentMenuId(paramsObj);

        // 将父菜单ID注入上下文，用于生成菜单SQL
        context.put("parentMenuId", parentMenuId);
    }

    /// 设置树形表相关变量到上下文
    ///
    /// 树形表需要额外的配置参数，如树编码字段、父编码字段、树名称字段等
    ///
    /// ### 注入的变量：
    ///
    ///     - treeCode - 树编码字段（转为驼峰命名），如 "deptId"
    ///     - treeParentCode - 父编码字段（转为驼峰命名），如 "parentId"
    ///     - treeName - 树名称字段（转为驼峰命名），如 "deptName"
    ///     - expandColumn - 展开按钮所在的列序号
    ///     - tree_parent_code - 原始父编码字段名（数据库字段）
    ///     - tree_name - 原始树名称字段名（数据库字段）
    ///
    ///
    /// @param context Velocity上下文对象
    /// @param genTable 业务表对象
    public static void setTreeVelocityContext(VelocityContext context, GenTable genTable)
    {
        // 获取表配置选项（JSON格式）
        String options = genTable.getOptions();
        JSONObject paramsObj = JSON.parseObject(options);

        // 提取树形结构配置字段，并转换为驼峰命名
        String treeCode = getTreecode(paramsObj);           // 树编码，如：dept_id -> deptId
        String treeParentCode = getTreeParentCode(paramsObj); // 父编码，如：parent_id -> parentId
        String treeName = getTreeName(paramsObj);           // 树名称，如：dept_name -> deptName

        // 将处理后的字段注入上下文（驼峰命名，用于Java代码）
        context.put("treeCode", treeCode);
        context.put("treeParentCode", treeParentCode);
        context.put("treeName", treeName);

        // 计算展开按钮应显示在哪一列
        context.put("expandColumn", getExpandColumn(genTable));

        // 如果存在父编码配置，将原始字段名也注入（用于SQL等场景）
        if (paramsObj.containsKey(GenConstants.TREE_PARENT_CODE))
        {
            context.put("tree_parent_code", paramsObj.getString(GenConstants.TREE_PARENT_CODE));
        }

        // 如果存在树名称配置，将原始字段名也注入
        if (paramsObj.containsKey(GenConstants.TREE_NAME))
        {
            context.put("tree_name", paramsObj.getString(GenConstants.TREE_NAME));
        }
    }

    /// 设置主子表相关变量到上下文
    ///
    /// 主子表结构需要额外注入子表的相关信息，用于生成关联查询和表单
    ///
    /// ### 注入的变量：
    ///
    ///     - subTable - 子表对象
    ///     - subTableName - 子表数据库表名
    ///     - subTableFkName - 子表外键字段名（数据库字段）
    ///     - subTableFkClassName - 外键字段类名（驼峰命名）
    ///     - subTableFkclassName - 外键字段对象名（首字母小写）
    ///     - subClassName - 子表类名（大驼峰）
    ///     - subclassName - 子表对象名（小驼峰）
    ///     - subImportList - 子表需要导入的包列表
    ///
    /// ### 使用场景示例：
    /// <pre>
    /// 主表：订单表（order_info）
    /// 子表：订单明细表（order_item）
    /// 外键：order_id
    ///
    /// 生成的主表实体类中会包含：
    /// private List&lt;OrderItem&gt; orderItemList;  // 子表列表
    ///
    /// 生成的Controller会包含关联查询方法：
    /// public AjaxResult getInfo(@PathVariable Long id) {
    ///     OrderInfo order = orderService.selectOrderInfoById(id);
    ///     List&lt;OrderItem&gt; itemList = orderService.selectOrderItemByOrderId(id);
    ///     return success(order).put("itemList", itemList);
    /// }
    /// </pre>
    ///
    /// @param context Velocity上下文对象
    /// @param genTable 主表对象
    public static void setSubVelocityContext(VelocityContext context, GenTable genTable)
    {
        // 获取子表对象
        GenTable subTable = genTable.getSubTable();
        String subTableName = genTable.getSubTableName();
        String subTableFkName = genTable.getSubTableFkName();
        String subClassName = genTable.getSubTable().getClassName();

        // 将外键字段名转换为驼峰命名
        // 例如：order_id -> orderId
        String subTableFkClassName = StringUtils.convertToCamelCase(subTableFkName);

        // === 注入子表相关信息 ===
        context.put("subTable", subTable);                              // 子表完整对象
        context.put("subTableName", subTableName);                      // 子表表名
        context.put("subTableFkName", subTableFkName);                  // 外键字段名（原始）
        context.put("subTableFkClassName", subTableFkClassName);        // 外键类名（驼峰）
        context.put("subTableFkclassName", StringUtils.uncapitalize(subTableFkClassName)); // 外键对象名（小驼峰）
        context.put("subClassName", subClassName);                      // 子表类名（大驼峰）
        context.put("subclassName", StringUtils.uncapitalize(subClassName)); // 子表对象名（小驼峰）
        context.put("subImportList", getImportList(genTable.getSubTable())); // 子表导入包列表
    }

    /// 获取模板文件列表
    ///
    /// 根据模板类型（CRUD/树表/主子表）返回需要生成的所有模板文件路径
    ///
    /// ### 通用模板（所有类型都会生成）：
    ///
    ///     - vm/java/domain.java.vm - 实体类模板
    ///     - vm/java/mapper.java.vm - Mapper接口模板
    ///     - vm/java/service.java.vm - Service接口模板
    ///     - vm/java/serviceImpl.java.vm - Service实现类模板
    ///     - vm/java/controller.java.vm - Controller控制器模板
    ///     - vm/xml/mapper.xml.vm - MyBatis XML映射文件模板
    ///     - vm/sql/sql.vm - 菜单SQL模板
    ///     - vm/js/api.js.vm - 前端API接口模板
    ///
    /// ### 特定类型额外模板：
    ///
    ///     - **CRUD（TPL_CRUD）：**
    ///
    ///   - vm/vue/v3/index.vue.vm - 标准列表页面模板
    ///
    ///
    ///     - **树表（TPL_TREE）：**
    ///
    ///   - vm/vue/v3/index-tree.vue.vm - 树形列表页面模板（支持展开/收起）
    ///
    ///
    ///     - **主子表（TPL_SUB）：**
    ///
    ///   - vm/vue/v3/index.vue.vm - 主表页面模板（包含子表编辑）
    ///     - vm/java/sub-domain.java.vm - 子表实体类模板
    ///
    ///
    ///
    /// ### 使用示例：
    /// <pre>
    /// // 获取CRUD模板列表
    /// List&lt;String&gt; templates = VelocityUtils.getTemplateList("crud", "vue");
    /// // 返回：[vm/java/domain.java.vm, vm/java/mapper.java.vm, ..., vm/vue/v3/index.vue.vm]
    ///
    /// // 获取树表模板列表
    /// List&lt;String&gt; templates = VelocityUtils.getTemplateList("tree", "vue");
    /// // 返回：[vm/java/domain.java.vm, vm/java/mapper.java.vm, ..., vm/vue/v3/index-tree.vue.vm]
    ///
    /// // 获取主子表模板列表
    /// List&lt;String&gt; templates = VelocityUtils.getTemplateList("sub", "vue");
    /// // 返回：[vm/java/domain.java.vm, ..., vm/vue/v3/index.vue.vm, vm/java/sub-domain.java.vm]
    /// </pre>
    ///
    /// @param tplCategory 模板类型（crud/tree/sub）
    /// @param tplWebType 前端类型（目前固定使用vue/v3）
    /// @return 模板文件路径列表
    public static List<String> getTemplateList(String tplCategory, String tplWebType)
    {
        // 当前固定使用Vue3模板
        String useWebType = "vm/vue/v3";
//        if ("element-plus".equals(tplWebType))
//        {
//            useWebType = "vm/vue/v3";
//        }

        List<String> templates = new ArrayList<String>();

        // === 通用Java后端模板 ===
        templates.add("vm/java/domain.java.vm");          // 实体类
        templates.add("vm/java/mapper.java.vm");          // Mapper接口
        templates.add("vm/java/service.java.vm");         // Service接口
        templates.add("vm/java/serviceImpl.java.vm");     // Service实现
        templates.add("vm/java/controller.java.vm");      // Controller控制器

        // === 配置文件模板 ===
        templates.add("vm/xml/mapper.xml.vm");            // MyBatis XML映射
        templates.add("vm/sql/sql.vm");                   // 菜单SQL

        // === 前端API模板 ===
        templates.add("vm/js/api.js.vm");                 // API接口定义

        // === 根据模板类型添加特定模板 ===
        if (GenConstants.TPL_CRUD.equals(tplCategory))
        {
            // CRUD模板：标准列表页面
            templates.add(useWebType + "/index.vue.vm");
        }
        else if (GenConstants.TPL_TREE.equals(tplCategory))
        {
            // 树表模板：树形列表页面（支持展开/收起）
            templates.add(useWebType + "/index-tree.vue.vm");
        }
        else if (GenConstants.TPL_SUB.equals(tplCategory))
        {
            // 主子表模板：主表页面 + 子表实体类
            templates.add(useWebType + "/index.vue.vm");          // 主表页面
            templates.add("vm/java/sub-domain.java.vm");          // 子表实体类
        }

        return templates;
    }

    /// 根据模板文件生成目标文件的完整路径
    ///
    /// 将模板文件路径转换为生成后Java/Vue文件的存放路径
    ///
    /// ### 路径规则：
    /// <table border="1">
    ///   <tr><th>模板</th><th>生成的文件路径</th><th>说明</th></tr>
    ///   <tr><td>domain.java.vm</td><td>main/java/{package}/domain/{ClassName}.java</td><td>实体类</td></tr>
    ///   <tr><td>sub-domain.java.vm</td><td>main/java/{package}/domain/{SubClassName}.java</td><td>子表实体类</td></tr>
    ///   <tr><td>mapper.java.vm</td><td>main/java/{package}/mapper/{ClassName}Mapper.java</td><td>Mapper接口</td></tr>
    ///   <tr><td>service.java.vm</td><td>main/java/{package}/service/I{ClassName}Service.java</td><td>Service接口</td></tr>
    ///   <tr><td>serviceImpl.java.vm</td><td>main/java/{package}/service/impl/{ClassName}ServiceImpl.java</td><td>Service实现</td></tr>
    ///   <tr><td>controller.java.vm</td><td>main/java/{package}/controller/{ClassName}Controller.java</td><td>Controller</td></tr>
    ///   <tr><td>mapper.xml.vm</td><td>main/resources/mapper/{ModuleName}/{ClassName}Mapper.xml</td><td>MyBatis XML</td></tr>
    ///   <tr><td>sql.vm</td><td>{BusinessName}Menu.sql</td><td>菜单SQL</td></tr>
    ///   <tr><td>api.js.vm</td><td>vue/api/{ModuleName}/{BusinessName}.js</td><td>API文件</td></tr>
    ///   <tr><td>index.vue.vm</td><td>vue/views/{ModuleName}/{BusinessName}/index.vue</td><td>页面组件</td></tr>
    /// </table>
    /// ### 使用示例：
    /// <pre>
    /// GenTable genTable = new GenTable();
    /// genTable.setPackageName("com.product.system");
    /// genTable.setModuleName("system");
    /// genTable.setClassName("SysUser");
    /// genTable.setBusinessName("user");
    ///
    /// // Java实体类
    /// String fileName = VelocityUtils.getFileName("vm/java/domain.java.vm", genTable);
    /// // 返回：main/java/com/product/system/domain/SysUser.java
    ///
    /// // Mapper接口
    /// fileName = VelocityUtils.getFileName("vm/java/mapper.java.vm", genTable);
    /// // 返回：main/java/com/product/system/mapper/SysUserMapper.java
    ///
    /// // Vue页面
    /// fileName = VelocityUtils.getFileName("vm/vue/v3/index.vue.vm", genTable);
    /// // 返回：vue/views/system/user/index.vue
    /// </pre>
    ///
    /// @param template 模板文件路径（如：vm/java/domain.java.vm）
    /// @param genTable 业务表对象
    /// @return 生成后的文件完整路径
    public static String getFileName(String template, GenTable genTable)
    {
        // 文件名称（返回值）
        String fileName = "";

        // 提取常用配置
        String packageName = genTable.getPackageName();       // 包名：com.product.system
        String moduleName = genTable.getModuleName();         // 模块名：system
        String className = genTable.getClassName();           // 类名：SysUser
        String businessName = genTable.getBusinessName();     // 业务名：user

        // 构建基础路径
        String javaPath = PROJECT_PATH + "/" + StringUtils.replace(packageName, ".", "/");  // Java代码路径
        String mybatisPath = MYBATIS_PATH + "/" + moduleName;                                // MyBatis XML路径
        String vuePath = "vue";                                                              // Vue前端路径

        // === 根据模板类型生成对应文件路径 ===
        if (template.contains("domain.java.vm"))
        {
            // 实体类：main/java/com/product/system/domain/SysUser.java
            fileName = StringUtils.format("{}/domain/{}.java", javaPath, className);
        }
        if (template.contains("sub-domain.java.vm") && StringUtils.equals(GenConstants.TPL_SUB, genTable.getTplCategory()))
        {
            // 子表实体类：main/java/com/product/system/domain/OrderItem.java
            fileName = StringUtils.format("{}/domain/{}.java", javaPath, genTable.getSubTable().getClassName());
        }
        else if (template.contains("mapper.java.vm"))
        {
            // Mapper接口：main/java/com/product/system/mapper/SysUserMapper.java
            fileName = StringUtils.format("{}/mapper/{}Mapper.java", javaPath, className);
        }
        else if (template.contains("service.java.vm"))
        {
            // Service接口：main/java/com/product/system/service/ISysUserService.java
            fileName = StringUtils.format("{}/service/I{}Service.java", javaPath, className);
        }
        else if (template.contains("serviceImpl.java.vm"))
        {
            // Service实现：main/java/com/product/system/service/impl/SysUserServiceImpl.java
            fileName = StringUtils.format("{}/service/impl/{}ServiceImpl.java", javaPath, className);
        }
        else if (template.contains("controller.java.vm"))
        {
            // Controller：main/java/com/product/system/controller/SysUserController.java
            fileName = StringUtils.format("{}/controller/{}Controller.java", javaPath, className);
        }
        else if (template.contains("mapper.xml.vm"))
        {
            // MyBatis XML：main/resources/mapper/system/SysUserMapper.xml
            fileName = StringUtils.format("{}/{}Mapper.xml", mybatisPath, className);
        }
        else if (template.contains("sql.vm"))
        {
            // 菜单SQL：userMenu.sql
            fileName = businessName + "Menu.sql";
        }
        else if (template.contains("api.js.vm"))
        {
            // 前端API：vue/api/system/user.js
            fileName = StringUtils.format("{}/api/{}/{}.js", vuePath, moduleName, businessName);
        }
        else if (template.contains("index.vue.vm"))
        {
            // Vue页面：vue/views/system/user/index.vue
            fileName = StringUtils.format("{}/views/{}/{}/index.vue", vuePath, moduleName, businessName);
        }
        else if (template.contains("index-tree.vue.vm"))
        {
            // 树形页面：vue/views/system/user/index.vue
            fileName = StringUtils.format("{}/views/{}/{}/index.vue", vuePath, moduleName, businessName);
        }
        return fileName;
    }

    /// 获取包前缀
    ///
    /// 从完整包名中提取基础包名，去掉最后一层
    ///
    /// ### 使用示例：
    /// <pre>
    /// getPackagePrefix("com.product.system")  → "com.product"
    /// getPackagePrefix("com.product.system.domain")  → "com.product.system"
    /// getPackagePrefix("org.example.module.service")  → "org.example.module"
    /// </pre>
    /// ### 应用场景：
    ///
    ///     - 生成import语句时引用其他模块
    ///     - 生成跨模块的依赖注入
    ///     - 生成Spring配置类的包扫描路径
    ///
    ///
    /// @param packageName 完整包名（如：com.product.system）
    /// @return 包前缀（如：com.product）
    public static String getPackagePrefix(String packageName)
    {
        // 找到最后一个点的位置
        int lastIndex = packageName.lastIndexOf(".");
        // 截取从开头到最后一个点之前的部分
        // 例如：com.product.system → com.product
        return StringUtils.substring(packageName, 0, lastIndex);
    }

    /// 获取实体类需要导入的包列表
    ///
    /// 根据表字段类型，自动收集需要导入的Java包，用于生成import语句
    ///
    /// ### 导入规则：
    ///
    ///     - **Date类型：**导入 java.util.Date 和 com.fasterxml.jackson.annotation.JsonFormat（用于日期格式化）
    ///     - **BigDecimal类型：**导入 java.math.BigDecimal（精确数值计算）
    ///     - **主子表：**导入 java.util.List（用于子表集合）
    ///
    /// ### 使用示例：
    /// <pre>
    /// 表结构：
    ///   id          bigint         - 主键
    ///   user_name   varchar(50)    - 用户名
    ///   amount      decimal(10,2)  - 金额
    ///   create_time datetime       - 创建时间
    ///
    /// 生成的importList：
    ///   [
    ///     "java.math.BigDecimal",           // 因为有amount字段
    ///     "java.util.Date",                 // 因为有create_time字段
    ///     "com.fasterxml.jackson.annotation.JsonFormat"  // 日期注解
    ///   ]
    ///
    /// 生成的实体类：
    /// import java.math.BigDecimal;
    /// import java.util.Date;
    /// import com.fasterxml.jackson.annotation.JsonFormat;
    ///
    /// public class Order {
    ///     private Long id;
    ///     private String userName;
    ///     private BigDecimal amount;
    ///     @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    ///     private Date createTime;
    /// }
    /// </pre>
    ///
    /// @param genTable 业务表对象
    /// @return 需要导入的包集合（使用HashSet去重）
    public static HashSet<String> getImportList(GenTable genTable)
    {
        List<GenTableColumn> columns = genTable.getColumns();
        GenTable subGenTable = genTable.getSubTable();
        HashSet<String> importList = new HashSet<String>();

        // 如果是主子表结构，需要导入List（用于存储子表数据）
        if (StringUtils.isNotNull(subGenTable))
        {
            importList.add("java.util.List");
        }

        // 遍历所有字段，根据类型收集需要导入的包
        for (GenTableColumn column : columns)
        {
            // 跳过基类字段（如：createBy, createTime等公共字段）
            if (!column.isSuperColumn() && GenConstants.TYPE_DATE.equals(column.getJavaType()))
            {
                // LocalDateTime 类型需要导入时间类和JSON格式化注解
                importList.add("java.time.LocalDateTime");
                importList.add("com.fasterxml.jackson.annotation.JsonFormat");
            }
            else if (!column.isSuperColumn() && GenConstants.TYPE_BIGDECIMAL.equals(column.getJavaType()))
            {
                // BigDecimal类型需要导入数学包
                importList.add("java.math.BigDecimal");
            }
        }
        return importList;
    }

    /// 获取表字段使用的字典类型列表
    ///
    /// 收集所有使用字典的字段，用于生成页面的字典加载语句
    ///
    /// ### 字典类型触发条件：
    ///
    ///     - 字段的dictType不为空
    ///     - 字段的htmlType为：select（下拉框）、radio（单选框）、checkbox（复选框）
    ///
    /// ### 使用示例：
    /// <pre>
    /// 表字段配置：
    ///   status     - dictType="sys_normal_disable", htmlType="select"
    ///   sex        - dictType="sys_user_sex", htmlType="radio"
    ///   user_type  - dictType="sys_user_type", htmlType="select"
    ///
    /// 返回的字典字符串：
    ///   "'sys_normal_disable', 'sys_user_sex', 'sys_user_type'"
    ///
    /// 在Vue模板中使用：
    ///   const dicts = ref(['sys_normal_disable', 'sys_user_sex', 'sys_user_type']);
    ///   getDicts(dicts.value).then(response => {
    ///     // 加载所有字典数据
    ///   });
    /// </pre>
    ///
    /// @param genTable 业务表对象
    /// @return 字典类型字符串，用逗号分隔（如："'dict1', 'dict2'"）
    public static String getDicts(GenTable genTable)
    {
        List<GenTableColumn> columns = genTable.getColumns();
        Set<String> dicts = new HashSet<String>();

        // 收集主表的字典类型
        addDicts(dicts, columns);

        // 如果是主子表，还需要收集子表的字典类型
        if (StringUtils.isNotNull(genTable.getSubTable()))
        {
            List<GenTableColumn> subColumns = genTable.getSubTable().getColumns();
            addDicts(dicts, subColumns);
        }

        // 将字典类型集合拼接成字符串，用逗号分隔
        return StringUtils.join(dicts, ", ");
    }

    /// 添加字段列表中的字典类型到集合
    ///
    /// 遍历字段列表，将符合条件的字典类型添加到集合中
    ///
    /// ### 筛选条件：
    ///
    ///     - 不是基类字段（isSuperColumn() = false）
    ///     - dictType不为空
    ///     - htmlType是 select/radio/checkbox 之一
    ///
    /// ### 支持的HTML控件类型：
    ///
    ///     - **select（下拉框）：**单选，从多个选项中选择一个
    ///     - **radio（单选框）：**单选，以单选按钮形式展示
    ///     - **checkbox（复选框）：**多选，可同时选择多个选项
    ///
    ///
    /// @param dicts 字典类型集合（输出参数）
    /// @param columns 字段列表
    public static void addDicts(Set<String> dicts, List<GenTableColumn> columns)
    {
        for (GenTableColumn column : columns)
        {
            // 检查字段是否配置了字典类型，并且是支持字典的控件类型
            if (!column.isSuperColumn()                     // 不是基类字段
                && StringUtils.isNotEmpty(column.getDictType())  // 字典类型不为空
                && StringUtils.equalsAny(                         // 是支持字典的控件类型
                    column.getHtmlType(),
                    new String[] { GenConstants.HTML_SELECT, GenConstants.HTML_RADIO, GenConstants.HTML_CHECKBOX }))
            {
                // 添加字典类型到集合（加上单引号）
                dicts.add("'" + column.getDictType() + "'");
            }
        }
    }

    /// 获取权限前缀
    ///
    /// 生成权限字符串的前缀，格式为 "模块名:业务名"
    ///
    /// ### 权限命名规范：
    ///
    ///     - 格式：{moduleName}:{businessName}
    ///     - 全部小写
    ///     - 用冒号分隔层级
    ///
    /// ### 使用示例：
    /// <pre>
    /// getPermissionPrefix("system", "user")     → "system:user"
    /// getPermissionPrefix("system", "role")     → "system:role"
    /// getPermissionPrefix("product", "order")   → "product:order"
    /// getPermissionPrefix("system", "menu")     → "system:menu"
    /// </pre>
    /// ### 在权限控制中的应用：
    /// <pre>
    /// // 权限注解
    /// @PreAuthorize("@ss.hasPermi('system:user:list')")
    /// public List&lt;SysUser&gt; list() { ... }
    ///
    /// @PreAuthorize("@ss.hasPermi('system:user:add')")
    /// public AjaxResult add(@RequestBody SysUser user) { ... }
    ///
    /// @PreAuthorize("@ss.hasPermi('system:user:edit')")
    /// public AjaxResult edit(@RequestBody SysUser user) { ... }
    ///
    /// @PreAuthorize("@ss.hasPermi('system:user:remove')")
    /// public AjaxResult remove(@PathVariable Long[] ids) { ... }
    /// </pre>
    ///
    /// @param moduleName 模块名称（如：system, product, order）
    /// @param businessName 业务名称（如：user, role, menu）
    /// @return 权限前缀字符串（如：system:user）
    public static String getPermissionPrefix(String moduleName, String businessName)
    {
        // 格式：模块名:业务名
        // 例如：system:user
        return StringUtils.format("{}:{}", moduleName, businessName);
    }

    /// 获取Java类型的完整包名
    ///
    /// 将简写的Java类型转换为完整的包名，用于在Velocity模板中生成import语句
    ///
    /// ### 类型映射表：
    /// <table border="1">
    ///   <tr><th>简写类型</th><th>完整包名</th><th>说明</th></tr>
    ///   <tr><td>String</td><td>java.lang.String</td><td>字符串</td></tr>
    ///   <tr><td>Long</td><td>java.lang.Long</td><td>长整型</td></tr>
    ///   <tr><td>Integer</td><td>java.lang.Integer</td><td>整型</td></tr>
    ///   <tr><td>Double</td><td>java.lang.Double</td><td>双精度浮点</td></tr>
    ///   <tr><td>Float</td><td>java.lang.Float</td><td>单精度浮点</td></tr>
    ///   <tr><td>Boolean</td><td>java.lang.Boolean</td><td>布尔型</td></tr>
    ///   <tr><td>Date</td><td>java.util.Date</td><td>日期</td></tr>
    ///   <tr><td>BigDecimal</td><td>java.math.BigDecimal</td><td>精确数值</td></tr>
    ///   <tr><td>Object</td><td>java.lang.Object</td><td>对象基类</td></tr>
    /// </table>
    /// ### 使用示例：
    /// <pre>
    /// // 在Velocity模板中使用
    /// #foreach($column in $columns)
    ///   $typeUtils.getFullJavaType($column.javaType)
    /// #end
    ///
    /// 生成结果示例：
    /// import java.lang.String;
    /// import java.lang.Long;
    /// import java.util.Date;
    /// import java.math.BigDecimal;
    /// </pre>
    /// ### 注意事项：
    ///
    ///     - java.lang包下的类（String, Long, Integer等）在实际开发中通常不需要显式import
    ///     - 此方法主要用于代码生成器的模板处理
    ///     - 自定义类型会返回原值，需要手动处理import
    ///
    ///
    /// @param javaType Java类型简写（如：String, Long, Date等）
    /// @return 完整包名（如：java.lang.String）
    public static String getFullJavaType(String javaType)
    {
        // 如果类型为空，返回Object
        if (StringUtils.isEmpty(javaType))
        {
            return "java.lang.Object";
        }

        // 类型映射：简写 → 完整包名
        switch (javaType)
        {
            case "String":
                return "java.lang.String";
            case "Long":
                return "java.lang.Long";
            case "Integer":
                return "java.lang.Integer";
            case "Double":
                return "java.lang.Double";
            case "Float":
                return "java.lang.Float";
            case "Boolean":
                return "java.lang.Boolean";
            case "Date":
                return "java.util.Date";
            case "BigDecimal":
                return "java.math.BigDecimal";
            case "Object":
                return "java.lang.Object";
            default:
                // 如果是自定义类型（如：LocalDateTime, User等），返回原值
                // 这些类型需要手动添加到importList中
                return javaType;
        }
    }

    /// 获取实体类的完整包名
    ///
    /// 根据包名和类名生成实体类的完整类路径（包含包名）
    ///
    /// ### 使用示例：
    /// <pre>
    /// getFullEntityName("com.product.system", "SysUser")
    ///   → "com.product.system.domain.SysUser"
    ///
    /// getFullEntityName("com.product.system.domain", "SysUser")
    ///   → "com.product.system.domain.SysUser"
    ///
    /// getFullEntityName("com.product", "OrderInfo")
    ///   → "com.product.domain.OrderInfo"
    /// </pre>
    /// ### 应用场景：
    ///
    ///     - 在Mapper XML中引用实体类
    ///     - 在Service中注入Mapper接口
    ///     - 在Controller中引用实体类
    ///
    ///
    /// @param packageName 包名（如：com.product.system）
    /// @param className 类名（如：SysUser）
    /// @return 完整包名（如：com.product.system.domain.SysUser）
    public static String getFullEntityName(String packageName, String className)
    {
        // 如果包名已经以.domain结尾，直接拼接类名
        if (packageName.endsWith(".domain"))
        {
            return packageName + "." + className;
        }
        else
        {
            // 否则添加.domain后缀再拼接类名
            return packageName + ".domain." + className;
        }
    }

    /// 获取上级菜单ID
    ///
    /// 从配置选项中获取父菜单ID，如果未配置则使用默认值
    ///
    /// ### 返回逻辑：
    /// <ol>
    ///     - 如果配置了parentMenuId且不为空，返回配置的值
    ///     - 否则返回默认值 "3"（系统工具菜单）
    /// </ol>
    /// ### 使用示例：
    /// <pre>
    /// // 配置了父菜单ID
    /// getParentMenuId({"parentMenuId": "1"})  → "1"
    ///
    /// // 未配置父菜单ID
    /// getParentMenuId({})  → "3"（默认值）
    ///
    /// // 在菜单SQL中使用
    /// INSERT INTO sys_menu VALUES (
    ///   (select max(menu_id) + 1 from sys_menu),
    ///   ${parentMenuId},  -- 使用此方法获取的父菜单ID
    ///   '用户管理',
    ///   ...
    /// );
    /// </pre>
    ///
    /// @param paramsObj 配置选项（JSON对象）
    /// @return 父菜单ID字符串
    public static String getParentMenuId(JSONObject paramsObj)
    {
        // 检查是否配置了父菜单ID
        if (StringUtils.isNotEmpty(paramsObj)
            && paramsObj.containsKey(GenConstants.PARENT_MENU_ID)
            && StringUtils.isNotEmpty(paramsObj.getString(GenConstants.PARENT_MENU_ID)))
        {
            return paramsObj.getString(GenConstants.PARENT_MENU_ID);
        }
        // 未配置则使用默认值（系统工具菜单）
        return DEFAULT_PARENT_MENU_ID;
    }

    /// 获取树编码字段（驼峰命名）
    ///
    /// 从配置中获取树的编码字段，并转换为驼峰命名格式
    ///
    /// ### 使用示例：
    /// <pre>
    /// getTreecode({"treeCode": "dept_id"})     → "deptId"
    /// getTreecode({"treeCode": "category_id"}) → "categoryId"
    /// getTreecode({})                           → ""
    /// </pre>
    ///
    /// @param paramsObj 配置选项（JSON对象）
    /// @return 树编码字段（驼峰命名），未配置则返回空字符串
    public static String getTreecode(JSONObject paramsObj)
    {
        if (paramsObj.containsKey(GenConstants.TREE_CODE))
        {
            // 将下划线命名转换为驼峰命名
            // 例如：dept_id → deptId
            return StringUtils.toCamelCase(paramsObj.getString(GenConstants.TREE_CODE));
        }
        return StringUtils.EMPTY;
    }

    /// 获取树父编码字段（驼峰命名）
    ///
    /// 从配置中获取树的父编码字段，并转换为驼峰命名格式
    ///
    /// ### 使用示例：
    /// <pre>
    /// getTreeParentCode({"treeParentCode": "parent_id"})  → "parentId"
    /// getTreeParentCode({"treeParentCode": "p_id"})       → "pId"
    /// getTreeParentCode({})                                → ""
    /// </pre>
    ///
    /// @param paramsObj 配置选项（JSON对象）
    /// @return 树父编码字段（驼峰命名），未配置则返回空字符串
    public static String getTreeParentCode(JSONObject paramsObj)
    {
        if (paramsObj.containsKey(GenConstants.TREE_PARENT_CODE))
        {
            // 将下划线命名转换为驼峰命名
            // 例如：parent_id → parentId
            return StringUtils.toCamelCase(paramsObj.getString(GenConstants.TREE_PARENT_CODE));
        }
        return StringUtils.EMPTY;
    }

    /// 获取树名称字段（驼峰命名）
    ///
    /// 从配置中获取树的名称字段，并转换为驼峰命名格式
    ///
    /// ### 使用示例：
    /// <pre>
    /// getTreeName({"treeName": "dept_name"})  → "deptName"
    /// getTreeName({"treeName": "cat_name"})   → "catName"
    /// getTreeName({})                          → ""
    /// </pre>
    ///
    /// @param paramsObj 配置选项（JSON对象）
    /// @return 树名称字段（驼峰命名），未配置则返回空字符串
    public static String getTreeName(JSONObject paramsObj)
    {
        if (paramsObj.containsKey(GenConstants.TREE_NAME))
        {
            // 将下划线命名转换为驼峰命名
            // 例如：dept_name → deptName
            return StringUtils.toCamelCase(paramsObj.getString(GenConstants.TREE_NAME));
        }
        return StringUtils.EMPTY;
    }

    /// 获取树形表格展开按钮所在的列序号
    ///
    /// 在树形表格中，需要在指定列前面显示展开/收起按钮
    /// 此方法计算该按钮应该显示在第几列
    ///
    /// ### 计算逻辑：
    /// <ol>
    ///     - 遍历所有在列表中显示的字段（column.isList() = true）
    ///     - 计数直到找到树名称字段所在的列
    ///     - 返回该计数作为展开按钮的位置
    /// </ol>
    /// ### 使用示例：
    /// <pre>
    /// 表字段配置：
    ///   id         - isList=false（不显示）
    ///   dept_name  - isList=true, treeName=true  ← 在此列前显示展开按钮
    ///   status     - isList=true
    ///   create_time - isList=true
    ///
    /// 计算过程：
    ///   第1个显示字段：dept_name → num=1, 找到treeName，返回1
    ///
    /// 在Vue表格中使用：
    ///   &lt;el-table-column type="expand" :column-index="{{expandColumn}}"&gt;
    ///
    /// 展开按钮会显示在第1列（dept_name列）前面
    /// </pre>
    ///
    /// @param genTable 业务表对象
    /// @return 展开按钮应该显示的列序号（从1开始）
    public static int getExpandColumn(GenTable genTable)
    {
        // 获取配置选项
        String options = genTable.getOptions();
        JSONObject paramsObj = JSON.parseObject(options);

        // 获取树名称字段（用于显示节点名称的字段）
        String treeName = paramsObj.getString(GenConstants.TREE_NAME);

        // 计数器：记录当前是第几个显示列
        int num = 0;

        // 遍历所有字段
        for (GenTableColumn column : genTable.getColumns())
        {
            // 只统计在列表中显示的字段
            if (column.isList())
            {
                num++;
                String columnName = column.getColumnName();

                // 如果当前字段是树名称字段，停止计数
                // 展开按钮将显示在此列之前
                if (columnName.equals(treeName))
                {
                    break;
                }
            }
        }
        return num;
    }
}
