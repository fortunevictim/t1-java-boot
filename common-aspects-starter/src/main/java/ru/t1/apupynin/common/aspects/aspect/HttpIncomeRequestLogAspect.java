package ru.t1.apupynin.common.aspects.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.BufferedReader;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Aspect
@RequiredArgsConstructor
public class HttpIncomeRequestLogAspect {

    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.application.name:service}")
    private String serviceName;

    @Value("${t1.logging.topic:service_logs}")
    private String serviceLogsTopic;

    @Before("@annotation(ru.t1.apupynin.common.aspects.annotation.HttpIncomeRequestLog)")
    public void logHttpIncomeRequest(JoinPoint joinPoint) {
        log.info("HTTP_INCOME start method={}", joinPoint.getSignature().toShortString());
        try {
            HttpServletRequest request = getCurrentHttpRequest();

            LocalDateTime timestamp = LocalDateTime.now();
            String methodSignature = joinPoint.getSignature().toLongString();
            String uri = request != null ? request.getRequestURI() : "N/A";
            String parameters = getMethodParameters(joinPoint.getArgs());
            String body = getRequestBody(request);

            Map<String, Object> logMessage = createLogMessage(
                    timestamp, methodSignature, uri, parameters, body
            );

            sendToKafka(logMessage);

            log.info("HTTP_INCOME done method={} uri={}", methodSignature, uri);

        } catch (Exception e) {
            log.error("Failed to log HTTP income request", e);
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
        message.put("requestType", "INCOME");
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
            log.debug("Successfully sent HTTP income log to Kafka topic: {}", serviceLogsTopic);
        } catch (Exception e) {
            log.warn("Failed to send HTTP income log to Kafka: {}", e.getMessage());
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

    private String getRequestBody(HttpServletRequest request) {
        if (request == null) {
            return "N/A";
        }

        try {
            StringBuilder body = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
            return body.toString();
        } catch (Exception e) {
            log.debug("Could not read request body: {}", e.getMessage());
            return "Body not available";
        }
    }
}


