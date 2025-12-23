package com.product.cache.script;

import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * @Auther: chuan
 * @Date: 2025/12/22 - 12 - 22 - 00:52
 * @Description: com.product.script
 * @version: 1.0
 */
@Component
public class RedisLuaScript {
    // 释放锁的脚本：判断值是否相等，相等则删除
    public static final String UNLOCK_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) " +
                    "else return 0 end";

    public static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    // 封禁脚本
    // KEYS[1]: 计数 Key (ip:count:...)
    // KEYS[2]: 封禁 Key (ip:ban:...)
    // ARGV[1]: 限制次数
    // ARGV[2]: 窗口时间
    // ARGV[3]: 封禁时长
    public static final String IP_LIMIT_LUA =
            "local count = redis.call('get', KEYS[1]) " +
            "if count and tonumber(count) >= tonumber(ARGV[1]) then " +
            "  redis.call('set', KEYS[2], '1', 'EX', ARGV[3]) " +
            "  return 0 " +
            "end " +
            "local res = redis.call('incr', KEYS[1]) " +
            "if res == 1 then " +
            "  redis.call('expire', KEYS[1], ARGV[2]) " +
            "end " +
            "return 1"; // 返回 1 表示放行 0表示封禁

    public static final DefaultRedisScript<Long> IP_LIMIT_SCRIPT;

    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setScriptText(UNLOCK_LUA);
        UNLOCK_SCRIPT.setResultType(Long.class);
        IP_LIMIT_SCRIPT = new DefaultRedisScript<>();
        IP_LIMIT_SCRIPT.setScriptText(IP_LIMIT_LUA);
        IP_LIMIT_SCRIPT.setResultType(Long.class);
    }
}
