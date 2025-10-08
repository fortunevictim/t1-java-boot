package ru.t1.apupynin.common.aspects.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Aspect
@RequiredArgsConstructor
public class MetricAspect {

    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${t1.metrics.threshold.ms:100}")
    private long thresholdMs;

    @Value("${spring.application.name:service}")
    private String serviceName;

    @Value("${t1.logging.topic:service_logs}")
    private String serviceLogsTopic;

    @Around("@annotation(ru.t1.apupynin.common.aspects.annotation.Metric)")
    public Object measureExecutionTime(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return pjp.proceed();
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed >= thresholdMs) {
                sendWarning(pjp, elapsed);
            }
        }
    }

    private void sendWarning(ProceedingJoinPoint pjp, long elapsedMs) {
        try {
            String signature = pjp.getSignature().toLongString();
            String params;
            try {
                params = objectMapper.writeValueAsString(pjp.getArgs());
            } catch (Exception e) {
                params = "[]";
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("service", serviceName);
            payload.put("type", "WARNING");
            payload.put("event", "SLOW_METHOD");
            payload.put("method", signature);
            payload.put("elapsedMs", elapsedMs);
            payload.put("params", params);

            Message<Map<String, Object>> message = MessageBuilder
                    .withPayload(payload)
                    .setHeader(KafkaHeaders.TOPIC, serviceLogsTopic)
                    .build();

            kafkaTemplate.send(message);
            log.warn("SLOW_METHOD {} took {} ms", signature, elapsedMs);
        } catch (Exception ex) {
            log.error("Failed to send slow method warning", ex);
        }
    }
}


