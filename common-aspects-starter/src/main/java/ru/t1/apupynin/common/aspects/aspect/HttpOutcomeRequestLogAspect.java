package ru.t1.apupynin.common.aspects.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Aspect
@RequiredArgsConstructor
public class HttpOutcomeRequestLogAspect {

    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.application.name:service}")
    private String serviceName;

    @Value("${t1.logging.topic:service_logs}")
    private String serviceLogsTopic;

    @AfterReturning(pointcut = "@annotation(ru.t1.apupynin.common.aspects.annotation.HttpOutcomeRequestLog)", returning = "result")
    public void logHttpOutcomeRequest(JoinPoint joinPoint, Object result) {
        log.info("HTTP_OUTCOME start method={}", joinPoint.getSignature().toShortString());
        try {
            HttpServletRequest request = getCurrentHttpRequest();

            LocalDateTime timestamp = LocalDateTime.now();
            String methodSignature = joinPoint.getSignature().toLongString();
            String uri = request != null ? request.getRequestURI() : "N/A";
            String parameters = getMethodParameters(joinPoint.getArgs());
            String body = getResponseBody(result);

            Map<String, Object> logMessage = createLogMessage(
                    timestamp, methodSignature, uri, parameters, body
            );

            sendToKafka(logMessage);

            log.info("HTTP_OUTCOME done method={} uri={}", methodSignature, uri);

        } catch (Exception e) {
            log.error("Failed to log HTTP outcome request", e);
        }
    }

    private Map<String, Object> createLogMessage(LocalDateTime timestamp, String methodSignature,
                                                 String uri, String parameters, String body) {
        Map<String, Object> message = new HashMap<>();
        message.put("timestamp", timestamp.toString());
        message.put("methodSignature", methodSignature);
        message.put("uri", uri);
        message.put("parameters", parameters);
        message.put("body", body);
        message.put("serviceName", serviceName);
        message.put("requestType", "OUTCOME");
        return message;
    }

    private void sendToKafka(Map<String, Object> logMessage) {
        try {
            Message<Map<String, Object>> message = MessageBuilder
                    .withPayload(logMessage)
                    .setHeader(KafkaHeaders.TOPIC, serviceLogsTopic)
                    .setHeader(KafkaHeaders.KEY, serviceName)
                    .setHeader("type", "INFO")
                    .build();

            kafkaTemplate.send(message);
            log.debug("Successfully sent HTTP outcome log to Kafka topic: {}", serviceLogsTopic);
        } catch (Exception e) {
            log.warn("Failed to send HTTP outcome log to Kafka: {}", e.getMessage());
        }
    }

    private HttpServletRequest getCurrentHttpRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return attributes.getRequest();
        } catch (Exception e) {
            log.debug("No HTTP request context available");
            return null;
        }
    }

    private String getMethodParameters(Object[] args) {
        try {
            if (args == null || args.length == 0) {
                return "[]";
            }
            return objectMapper.writeValueAsString(Arrays.asList(args));
        } catch (Exception e) {
            log.warn("Failed to serialize method parameters", e);
            return "Failed to serialize parameters: " + e.getMessage();
        }
    }

    private String getResponseBody(Object result) {
        try {
            if (result == null) {
                return "null";
            }
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.warn("Failed to serialize response body", e);
            return "Failed to serialize response: " + e.getMessage();
        }
    }
}


