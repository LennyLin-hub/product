package com.product.pps.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * PPS 定时任务配置
 *
 * 说明：
 * - 仅开启调度能力，不承载具体业务逻辑
 * - 具体任务放在业务 Service 中，便于复用与测试
 */
@Configuration
@EnableScheduling
public class PpsSchedulingConfig {
}
