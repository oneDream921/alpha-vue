package io.github.onedream921.alphavue.modules.monitor.service;

import io.github.onedream921.alphavue.modules.monitor.vo.RedisMetricsVo;
import org.springframework.stereotype.Service;

/**
 * Redis 指标查询服务。
 */
@Service
public class RedisMetricsService {
    private final RedisMetricsSampler sampler;

    public RedisMetricsService(RedisMetricsSampler sampler) {
        this.sampler = sampler;
    }

    public RedisMetricsVo metrics() {
        return sampler.snapshot();
    }
}
