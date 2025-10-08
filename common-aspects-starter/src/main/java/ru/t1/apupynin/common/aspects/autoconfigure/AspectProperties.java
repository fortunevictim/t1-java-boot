package ru.t1.apupynin.common.aspects.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "t1")
public class AspectProperties {
    private String loggingTopic = "service_logs";
    private long metricsThresholdMs = 100;
    private long cacheTtlMs = 60000;

    public String getLoggingTopic() {
        return loggingTopic;
    }

    public void setLoggingTopic(String loggingTopic) {
        this.loggingTopic = loggingTopic;
    }

    public long getMetricsThresholdMs() {
        return metricsThresholdMs;
    }

    public void setMetricsThresholdMs(long metricsThresholdMs) {
        this.metricsThresholdMs = metricsThresholdMs;
    }

    public long getCacheTtlMs() {
        return cacheTtlMs;
    }

    public void setCacheTtlMs(long cacheTtlMs) {
        this.cacheTtlMs = cacheTtlMs;
    }
}


