package com.product.cache.constants;

/**
 * Redis 分布式锁 Key 统一定义
 *
 * 说明：
 * - 避免锁 key 散落在业务代码里
 * - 统一管理命名空间，后续调整前缀或拆分业务时更容易
 * - 目前用于排程防重和热点缓存保护
 */
public final class RedisLockKeys {
    private static final String LOCK_PREFIX = "lock:";

    /**
     * 缓存相关锁
     */
    public static final class Cache {
        private Cache() {
        }

        public static String hotKey(String realKey) {
            return LOCK_PREFIX + "cache:hot:" + realKey;
        }
    }

    /**
     * 排程相关锁
     */
    public static final class Pps {
        private Pps() {
        }

        /**
         * 排程相关锁
         */
        public static final class Schedule {
            private Schedule() {
            }

            public static final String SUBMIT_LOCK_KEY = LOCK_PREFIX + "pps:schedule:submit";
            public static final String EXECUTE_LOCK_KEY = LOCK_PREFIX + "pps:schedule:execute";
            public static final String TIMEOUT_SWEEP_LOCK_KEY = LOCK_PREFIX + "pps:schedule:timeout-sweep";
        }
    }

    private RedisLockKeys() {
    }
}
