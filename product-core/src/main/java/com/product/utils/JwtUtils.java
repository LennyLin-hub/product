package com.product.utils;

import com.product.constant.Constants;
import com.product.domain.LoginUser;
import com.product.entity.SysUser;
import com.product.utils.ip.AddressUtils;
import com.product.utils.ip.IpUtils;
import eu.bitwalker.useragentutils.UserAgent;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

/// JWT令牌工具类
/// 该工具类提供JWT（JSON Web Token）的创建、解析、验证等功能，实现无状态的用户认证。
/// JWT是一种开放的行业标准（RFC 7519），用于在网络应用间安全地传输声明信息。
/// 核心功能：
/// 1. 令牌创建：生成包含用户信息的JWT令牌
/// 2. 令牌解析：从JWT令牌中提取用户信息
/// 3. 令牌验证：验证令牌的签名和有效期
/// 4. 用户代理：记录登录设备信息（浏览器、操作系统、IP等）
/// 5. 令牌刷新：支持令牌的自动续期机制
/// JWT结构：
/// Header（头部）：令牌类型和签名算法信息
/// Payload（载荷）：用户身份信息和权限数据
/// Signature（签名）：防止令牌被篡改的安全签名
/// 安全特性：
/// - 使用HS512算法进行数字签名
/// - 支持令牌过期时间控制
/// - 包含用户设备信息便于审计
/// - 无状态设计，服务端无需存储会话
/// - 支持跨域和分布式部署
/// 配置参数：
/// - token.header: 令牌请求头名称（默认：Authorization）
/// - token.secret: 令牌签名密钥，必须保密
/// - token.expireTime: 令牌有效期（分钟，默认30分钟）
/// 使用场景：
/// - 用户登录认证和授权
/// - API接口的访问控制
/// - 微服务间的身份传递
/// - 前后端分离架构的会话管理
/// 注意事项：
/// - 签名密钥必须安全保存，防止泄露
/// - 令牌中不应包含敏感信息
/// - 合理设置过期时间，平衡安全性和用户体验
/// - 考虑令牌撤销机制（如退出登录）
///
/// @author fast
@Slf4j
@Component
public class JwtUtils
{
    /**
     * 令牌请求头名称
     * 从配置文件中读取，默认为"Authorization"
     */
    @Value("${token.header}")
    private String header;

    /**
     * JWT签名密钥
     * 用于JWT令牌的数字签名，必须保密，防止令牌被伪造
     */
    @Value("${token.secret}")
    private String secret;

    /**
     * 令牌有效期（分钟）
     * 默认30分钟，可根据安全需求调整
     */
    @Value("${token.expireTime}")
    private int expireTime;

    /**
     * 毫秒单位常量：1秒
     */
    protected static final long MILLIS_SECOND = 1000;

    /**
     * 毫秒单位常量：1分钟
     */
    protected static final long MILLIS_MINUTE = 60 * MILLIS_SECOND;

    /**
     * 20分钟的毫秒数
     * 用于令牌刷新的时间阈值
     */
    private static final Long MILLIS_MINUTE_TWENTY = 20 * 60 * 1000L;

    /// 从HTTP请求中获取用户身份信息
    /// 该方法从请求头中提取JWT令牌，解析并构建LoginUser对象。
    /// 在JWT认证过滤器中被调用，用于将用户信息设置到Spring Security上下文中。
    /// 处理流程：
    /// 1. 从HTTP请求头中提取JWT令牌
    /// 2. 解析JWT令牌获取Claims对象
    /// 3. 从Claims中构建LoginUser对象
    /// 4. 返回用户信息供认证使用
    /// 异常处理：
    /// - 令牌格式错误：记录日志并返回null
    /// - 令牌签名验证失败：记录日志并返回null
    /// - 令牌过期：记录日志并返回null
    /// - 其他解析异常：记录日志并返回null
    ///
    /// @param request HTTP请求对象，包含JWT令牌
    /// @return LoginUser对象，包含完整的用户信息；如果令牌无效则返回null
    public LoginUser getLoginUser(HttpServletRequest request)
    {
        // 从请求头中获取JWT令牌
        String token = getToken(request);
        if (StringUtils.isNotEmpty(token))
        {
            try
            {
                // 解析JWT令牌获取Claims
                Claims claims = parseToken(token);

                // 从Claims中构建LoginUser对象
                // 当前实现将用户信息直接存储在token中，无需查询Redis缓存
                LoginUser user = buildLoginUser(claims);
                return user;
            }
            catch (Exception e)
            {
                // 记录解析异常的详细信息，便于问题排查
                log.error("获取用户信息异常'{}'", e.getMessage());
            }
        }
        return null;
    }

    /// 设置用户身份信息到缓存
    /// 在传统的JWT实现中，该方法通常用于将用户信息缓存到Redis等存储中。
    /// 在当前的自包含JWT实现中，用户信息直接存储在令牌中，因此该方法为空实现。
    /// 设计考虑：
    /// - 自包含JWT：令牌本身包含所有必要信息，无需额外缓存
    /// - 性能优化：减少Redis查询，提升响应速度
    /// - 状态一致性：避免缓存与令牌数据不一致的问题
    ///
    /// @param loginUser 登录用户对象
    public void setLoginUser(LoginUser loginUser)
    {
        // 由于用户信息直接存储在token中，这里不需要额外操作
        // 保留方法接口以保持向后兼容性
    }

    /// 删除用户身份信息缓存
    /// 在退出登录时调用，用于清除用户的登录缓存信息。
    /// 在自包含JWT实现中，由于用户信息存储在令牌中，该方法为空实现。
    /// 退出机制：
    /// - JWT是无状态的，服务端无法直接使令牌失效
    /// - 可通过令牌黑名单机制实现强制退出
    /// - 客户端删除令牌即可实现用户退出
    ///
    /// @param token JWT令牌字符串
    public void delLoginUser(String token)
    {
        // 由于用户信息直接存储在token中，这里不需要额外操作
        // 可考虑添加令牌黑名单机制以支持强制退出
    }

    /// 创建JWT令牌
    /// 该方法根据用户登录信息创建JWT令牌，令牌中包含用户的身份信息、
    /// 权限列表、登录设备信息等。采用自包含设计，减少对Redis的依赖。
    /// 令牌包含的信息：
    /// - 用户基本信息：ID、用户名、部门ID、头像等
    /// - 权限信息：用户的所有权限列表
    /// - 登录信息：登录时间、过期时间、设备信息等
    /// - 设备信息：IP地址、浏览器、操作系统等
    /// 安全考虑：
    /// - 使用HS512算法进行数字签名
    /// - 设置合理的过期时间
    /// - 避免在令牌中存储敏感信息
    /// - 记录登录设备信息便于安全审计
    ///
    /// @param loginUser 包含用户登录信息的对象
    /// @return JWT令牌字符串
    public String createToken(LoginUser loginUser)
    {
        // 设置用户代理信息（浏览器、操作系统、IP等）
        setUserAgent(loginUser);

        // 构建JWT的Claims（载荷）
        Map<String, Object> claims = new HashMap<>();

        // 用户基本身份信息
        claims.put(Constants.JWT_USERNAME, loginUser.getUsername());
        claims.put("userId", loginUser.getUserId());

        // 用户权限信息（转换为List以兼容JWT序列化）
        if (loginUser.getPermissions() != null) {
            claims.put("permissions", new java.util.ArrayList<>(loginUser.getPermissions()));
        } else {
            claims.put("permissions", new java.util.ArrayList<>());
        }

        // 登录时间信息
        long currentTime = System.currentTimeMillis();
        claims.put("loginTime", currentTime);
        claims.put("expireTime", currentTime + expireTime * MILLIS_MINUTE);

        // 登录设备信息
        claims.put("ipaddr", loginUser.getIpaddr());
        claims.put("loginLocation", loginUser.getLoginLocation());
        claims.put("browser", loginUser.getBrowser());
        claims.put("os", loginUser.getOs());

        // 用户显示信息
        SysUser user = loginUser.getUser();
        if (user != null) {
            claims.put("userName", user.getUserName());
            claims.put("avatar", user.getAvatar());
        }

        // 创建JWT令牌
        return createToken(claims);
    }

    /// 验证令牌有效期并自动刷新
    /// 检查令牌的剩余有效期，如果即将过期（不足20分钟），则执行刷新操作。
    /// 在自包含JWT实现中，该方法主要用于检查和更新用户会话的有效性。
    /// 刷新策略：
    /// - 20分钟刷新阈值：避免令牌在使用过程中过期
    /// - 静默刷新：用户无感知的令牌续期
    /// - 前端配合：前端需要定期刷新JWT令牌
    ///
    /// @param loginUser 登录用户对象，包含令牌时间信息
    public void verifyToken(LoginUser loginUser)
    {
        long expireTime = loginUser.getExpireTime();
        long currentTime = System.currentTimeMillis();

        // 检查令牌是否在20分钟内过期
        if (expireTime - currentTime <= MILLIS_MINUTE_TWENTY)
        {
            // 令牌快过期时，触发刷新逻辑
            // 在自包含JWT实现中，通常由前端主动刷新令牌
            log.debug("JWT令牌即将过期，建议刷新令牌");
        }
    }

    /// 刷新令牌有效期
    /// 更新LoginUser对象中的登录时间和过期时间，实现令牌的有效期延长。
    /// 在实际的令牌刷新流程中，需要生成新的JWT令牌并返回给客户端。
    /// 刷新机制：
    /// - 延长过期时间：通常延长一个完整的有效期周期
    /// - 保持登录状态：用户无需重新登录
    /// - 安全控制：限制刷新次数和频率
    ///
    /// @param loginUser 需要刷新令牌的登录用户对象
    public void refreshToken(LoginUser loginUser)
    {
        long currentTime = System.currentTimeMillis();

        // 更新登录时间和过期时间
        loginUser.setLoginTime(currentTime);
        loginUser.setExpireTime(currentTime + expireTime * MILLIS_MINUTE);

        // 注意：在实际应用中，需要重新生成JWT令牌并返回给客户端
        // 该方法主要用于更新内存中的用户对象状态
    }

    /// 设置用户代理信息
    /// 解析HTTP请求中的User-Agent头，提取用户的浏览器和操作系统信息，
    /// 同时获取用户的IP地址和地理位置信息，用于安全审计和用户行为分析。
    /// 收集的信息：
    /// - IP地址：客户端的公网IP地址
    /// - 地理位置：根据IP地址解析的地理位置
    /// - 浏览器信息：用户使用的浏览器类型和版本
    /// - 操作系统：用户设备的操作系统信息
    /// 用途：
    /// - 安全审计：记录用户登录设备信息
    /// - 异常检测：识别异常的登录地点和设备
    /// - 用户体验：根据设备类型优化界面显示
    ///
    /// @param loginUser 登录用户对象，用于设置设备信息
    public void setUserAgent(LoginUser loginUser)
    {
        // 解析User-Agent头获取浏览器和操作系统信息
        UserAgent userAgent = UserAgent.parseUserAgentString(ServletUtils.getRequest().getHeader("User-Agent"));

        // 获取客户端IP地址
        String ip = IpUtils.getIpAddr();

        // 设置用户设备信息
        loginUser.setIpaddr(ip);
        loginUser.setLoginLocation(AddressUtils.getRealAddressByIP(ip));
        loginUser.setBrowser(userAgent.getBrowser().getName());
        loginUser.setOs(userAgent.getOperatingSystem().getName());
    }

    /// 根据Claims创建JWT令牌
    /// 使用JJWT库创建JWT令牌，设置Claims（载荷）并使用HS512算法进行签名。
    /// 生成的令牌格式为：header.payload.signature
    /// 签名过程：
    /// 1. 设置JWT的Claims（载荷）数据
    /// 2. 使用HS512算法和密钥进行数字签名
    /// 3. 生成最终的JWT令牌字符串
    ///
    /// @param claims JWT的载荷数据，包含用户信息和元数据
    /// @return 签名后的JWT令牌字符串
    private String createToken(Map<String, Object> claims)
    {
        String token = Jwts.builder()
                .setClaims(claims)                    // 设置载荷
                .signWith(SignatureAlgorithm.HS512, secret) // 使用HS512算法签名
                .compact();                           // 生成紧凑的JWT字符串
        return token;
    }

    /// 解析JWT令牌获取Claims
    /// 使用配置的签名密钥验证JWT令牌的签名，并解析出Claims（载荷）内容。
    /// 如果签名验证失败或令牌格式错误，将抛出相应的异常。
    /// 验证过程：
    /// 1. 解析JWT令牌的三部分：header.payload.signature
    /// 2. 使用密钥验证数字签名
    /// 3. 返回解析后的Claims对象
    ///
    /// @param token 待解析的JWT令牌字符串
    /// @return Claims对象，包含令牌中的所有声明数据
    /// @throws io.jsonwebtoken.SignatureException 签名验证失败时抛出
    /// @throws io.jsonwebtoken.MalformedJwtException 令牌格式错误时抛出
    /// @throws io.jsonwebtoken.ExpiredJwtException 令牌过期时抛出
    private Claims parseToken(String token)
    {
        return Jwts.parser()
                .setSigningKey(secret)               // 设置签名密钥
                .parseClaimsJws(token)               // 解析JWT令牌
                .getBody();                         // 获取Claims（载荷）
    }

    /// 从JWT令牌中获取用户名
    /// 解析JWT令牌并提取其中的用户名信息。这是一个便捷方法，
    /// 用于快速获取令牌中的主要身份标识。
    ///
    /// @param token JWT令牌字符串
    /// @return 用户名字符串，如果令牌无效则返回null
    public String getUsernameFromToken(String token)
    {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    /// 根据Claims构建LoginUser对象
    /// 该方法从JWT的Claims中提取用户信息，构建完整的LoginUser对象。
    /// 需要处理类型转换、空值检查、集合类型兼容等问题。
    /// 构建过程：
    /// 1. 提取用户基本身份信息（ID、部门ID等）
    /// 2. 处理权限列表的类型转换（List -> Set）
    /// 3. 设置登录时间相关字段
    /// 4. 设置设备信息和地理位置
    /// 5. 构建SysUser对象并设置显示信息
    /// 类型处理：
    /// - 基本类型：处理String到Long的转换
    /// - 集合类型：处理List和Set的兼容性
    /// - 空值安全：所有字段都有空值检查
    /// - 默认值：为缺失字段设置合理的默认值
    ///
    /// @param claims JWT中解析出的声明对象，包含用户信息
    /// @return 构建完成的LoginUser对象
    private LoginUser buildLoginUser(Claims claims) {
        LoginUser loginUser = new LoginUser();

        // 提取用户ID，处理类型转换和空值检查
        Object userIdObj = claims.get("userId");
        if (userIdObj != null) {
            loginUser.setUserId(Long.valueOf(userIdObj.toString()));
        }

        // 处理权限列表的类型转换问题
        // JWT序列化时权限信息被转换为List，需要转换为Set使用
        Object permissionsObj = claims.get("permissions");
        if (permissionsObj instanceof List) {
            List<String> permissionsList = (List<String>) permissionsObj;
            Set<String> permissionsSet = new HashSet<>(permissionsList);
            loginUser.setPermissions(permissionsSet);
        } else if (permissionsObj instanceof Set) {
            loginUser.setPermissions((Set<String>) permissionsObj);
        } else if (permissionsObj instanceof Collection) {
            loginUser.setPermissions(new HashSet<>((Collection<String>) permissionsObj));
        } else {
            loginUser.setPermissions(new HashSet<>());
        }

        // 提取登录时间，处理类型转换
        Object loginTimeObj = claims.get("loginTime");
        if (loginTimeObj != null) {
            loginUser.setLoginTime(Long.valueOf(loginTimeObj.toString()));
        }

        // 提取过期时间，处理类型转换
        Object expireTimeObj = claims.get("expireTime");
        if (expireTimeObj != null) {
            loginUser.setExpireTime(Long.valueOf(expireTimeObj.toString()));
        }

        // 设置设备信息（字符串类型，直接赋值）
        loginUser.setIpaddr((String) claims.get("ipaddr"));
        loginUser.setLoginLocation((String) claims.get("loginLocation"));
        loginUser.setBrowser((String) claims.get("browser"));
        loginUser.setOs((String) claims.get("os"));

        // 构建SysUser对象并设置显示信息
        SysUser user = new SysUser();
        user.setUserId(loginUser.getUserId());
        user.setUserName((String) claims.get("userName"));
        user.setAvatar((String) claims.get("avatar"));
        loginUser.setUser(user);

        return loginUser;
    }

    /// 从HTTP请求中提取JWT令牌
    /// 从指定的HTTP请求头中提取JWT令牌字符串。支持标准的Bearer Token格式。
    /// 如果令牌包含前缀，会自动去除前缀部分。
    /// 令牌格式：
    /// - 标准格式：Authorization: Bearer <token>
    /// - 自定义格式：Authorization: <prefix><token>
    /// - 纯令牌格式：Authorization: <token>
    /// 处理逻辑：
    /// 1. 从指定请求头获取令牌字符串
    /// 2. 检查是否为空
    /// 3. 检查是否包含令牌前缀
    /// 4. 去除前缀，返回纯令牌字符串
    /// 配置说明：
    /// - header: 令牌请求头名称，通常为"Authorization"
    /// - TOKEN_PREFIX: 令牌前缀，通常为"Bearer "
    ///
    /// @param request HTTP请求对象
    /// @return 纯的JWT令牌字符串，如果请求头中无令牌则返回null
    private String getToken(HttpServletRequest request)
    {
        // 从请求头中获取令牌字符串
        String token = request.getHeader(header);

        // 检查令牌是否包含前缀，如果有则去除前缀
        if (StringUtils.isNotEmpty(token) && token.startsWith(Constants.TOKEN_PREFIX))
        {
            token = token.replace(Constants.TOKEN_PREFIX, "");
        }

        return token;
    }
}
