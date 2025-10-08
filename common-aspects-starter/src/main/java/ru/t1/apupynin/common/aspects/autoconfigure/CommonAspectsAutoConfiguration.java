package ru.t1.apupynin.common.aspects.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import java.util.Map;
import ru.t1.apupynin.common.aspects.aspect.MetricAspect;
import ru.t1.apupynin.common.aspects.aspect.CachedAspect;
import ru.t1.apupynin.common.aspects.aspect.HttpIncomeRequestLogAspect;
import ru.t1.apupynin.common.aspects.aspect.HttpOutcomeRequestLogAspect;
import ru.t1.apupynin.common.aspects.aspect.LogDatasourceErrorAspect;

@AutoConfiguration
@ConditionalOnClass(KafkaTemplate.class)
@EnableConfigurationProperties(AspectProperties.class)
public class CommonAspectsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MetricAspect metricAspect(KafkaTemplate<String, Map<String, Object>> kafkaTemplate,
                                     ObjectMapper objectMapper) {
        return new MetricAspect(kafkaTemplate, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public CachedAspect cachedAspect() {
        return new CachedAspect();
    }

    @Bean
    @ConditionalOnMissingBean
    public HttpIncomeRequestLogAspect httpIncomeRequestLogAspect(
            KafkaTemplate<String, Map<String, Object>> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        return new HttpIncomeRequestLogAspect(kafkaTemplate, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public HttpOutcomeRequestLogAspect httpOutcomeRequestLogAspect(
            KafkaTemplate<String, Map<String, Object>> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        return new HttpOutcomeRequestLogAspect(kafkaTemplate, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public LogDatasourceErrorAspect logDatasourceErrorAspect(
            KafkaTemplate<String, Map<String, Object>> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        return new LogDatasourceErrorAspect(kafkaTemplate, objectMapper);
    }
}


