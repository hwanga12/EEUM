package org.ssafy.eeum.global.infra.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.ssafy.eeum.domain.iot.service.SensorEventService;
import org.ssafy.eeum.domain.iot.service.DeviceStatusService;
import org.ssafy.eeum.domain.iot.event.IotDeviceEvent;
import org.ssafy.eeum.global.auth.jwt.JwtProvider;
import org.springframework.context.event.EventListener;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class MqttService {

    private final MessageChannel mqttOutboundChannel;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final SensorEventService sensorEventService;
    private final DeviceStatusService deviceStatusService;
    private final JwtProvider jwtProvider;

    public void publish(String topic, String payload) {
        try {
            Message<String> message = MessageBuilder
                    .withPayload(payload)
                    .setHeader(MqttHeaders.TOPIC, topic)
                    .setHeader(MqttHeaders.QOS, 1)
                    .build();

            mqttOutboundChannel.send(message);
            log.info("MQTT Publish Success - Topic: {}, Payload: {}", topic, payload);
        } catch (Exception e) {
            log.error("MQTT Publish Failed - Topic: {}, Error: {}", topic, e.getMessage());
        }
    }

    public void sendToIot(Integer groupId, String category, String jsonPayload) {
        // 사용 형식 : eeum/group/{groupId}/{category}
        String topic = String.format("eeum/group/%d/%s", groupId, category);
        publish(topic, jsonPayload);
    }

    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMessage(Message<?> message) {
        String topic = (String) message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
        Object payloadObj = message.getPayload();
        String payload;

        if (payloadObj instanceof byte[]) {
            payload = new String((byte[]) payloadObj);
        } else {
            payload = payloadObj.toString();
        }

        log.info("MQTT Message Received - Topic: {}, Payload: {}", topic, payload);

        try {
            if ("eeum/sync".equals(topic)) {
                handleSync(payload);
            } else if ("eeum/response".equals(topic)) {
                handleResponse(payload);
            } else if ("eeum/event".equals(topic)) {
                handleEvent(payload);
            } else if ("eeum/update".equals(topic)) {
                handleUpdate(payload);
            }
        } catch (Exception e) {
            log.error("Error handling MQTT message for topic {}: {}", topic, e.getMessage());
        }
    }

    private void handleSync(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String token = getTokenFromNode(node);
            Integer groupId = validateTokenAndGetGroupId(token);

            String masterSerialNumber = node.path("serial_number").asText();
            JsonNode linkArray = node.path("link");

            if (linkArray.isArray()) {
                for (JsonNode linkNode : linkArray) {
                    String slaveSerial = linkNode.path("id").asText();
                    boolean alive = linkNode.path("alive").asBoolean();

                    // DB에 상태 저장
                    deviceStatusService.updateDeviceStatus(
                            groupId, masterSerialNumber, slaveSerial, alive);
                }
            }
            log.info("Handled Status Sync for Family: {}", groupId);
        } catch (Exception e) {
            log.warn("Failed to handle sync: {}", e.getMessage());
        }
    }

    private void handleResponse(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String token = getTokenFromNode(node);
            Integer groupId = validateTokenAndGetGroupId(token);

            String deviceEventId = node.path("id").asText();
            String serialNumber = node.path("serial_number").asText();
            String sttContent = node.path("stt_content").asText();

            // 이벤트 ID로 센서 이벤트 조회 후 FallEvent 업데이트
            sensorEventService.linkVoiceResponseToEvent(serialNumber, deviceEventId, sttContent);

            log.info("Handled Voice Response: Family={}, SN={}, EventId={}, Content={}",
                    groupId, serialNumber, deviceEventId, sttContent);
        } catch (Exception e) {
            log.warn("Failed to handle response: {}", e.getMessage());
        }
    }

    private void handleEvent(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String token = getTokenFromNode(node);
            Integer groupId = validateTokenAndGetGroupId(token);

            String kind = node.path("kind").asText();
            String deviceEventId = node.path("id").asText();
            String serialNumber = node.path("serial_number").asText();
            String location = node.path("location").asText();
            String eventType = node.path("event").asText();
            double startedAtTimestamp = node.path("started_at").asDouble();
            double detectedAtTimestamp = node.path("detected_at").asDouble();

            LocalDateTime startedAt = convertTimestamp(startedAtTimestamp);
            LocalDateTime detectedAt = convertTimestamp(detectedAtTimestamp);

            // 센서 이벤트 저장 (중복 방지 포함)
            sensorEventService.handleSensorEvent(
                    groupId, deviceEventId, serialNumber, kind, eventType,
                    location, startedAt, detectedAt, payload);

            log.info("Handled Sensor Event: eventId={}, type={}, serial={}",
                    deviceEventId, eventType, serialNumber);
        } catch (Exception e) {
            log.warn("Failed to handle event: {}", e.getMessage());
        }
    }

    private LocalDateTime convertTimestamp(double timestamp) {
        return Instant.ofEpochMilli((long) (timestamp * 1000))
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    private void handleUpdate(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String token = getTokenFromNode(node);
            Integer groupId = validateTokenAndGetGroupId(token);

            String kind = node.path("kind").asText();
            String id = node.path("id").asText();
            int updateCnt = node.path("update_cnt").asInt();

            log.info("Handled Update Acknowledgement from IoT: Family={}, Kind={}, Count={}, TargetId={}",
                    groupId, kind, updateCnt, id);
        } catch (Exception e) {
            log.warn("Failed to handle IoT update acknowledgement: {}", e.getMessage());
        }
    }

    @EventListener
    public void handleDeviceEvent(IotDeviceEvent event) {
        try {
            String topic = String.format("eeum/device/%s/sync", event.getSerialNumber());
            String type = event.getType();

            java.util.Map<String, String> payload = new java.util.HashMap<>();
            payload.put("type", type.equals("UPDATE") ? "LOCATION_UPDATE" : "DEVICE_DELETE");
            if (event.getLocation() != null) {
                payload.put("location", event.getLocation());
            }

            String jsonPayload = objectMapper.writeValueAsString(payload);
            publish(topic, jsonPayload);
            log.info("Sent Device Sync Event via MQTT: Serial={}, Type={}", event.getSerialNumber(), type);
        } catch (Exception e) {
            log.error("Failed to send Device Sync Event via MQTT: {}", e.getMessage());
        }
    }

    private String getTokenFromNode(JsonNode node) {
        if (node.has("token"))
            return node.path("token").asText();
        if (node.has("toekn"))
            return node.path("toekn").asText();
        throw new IllegalArgumentException("Token is missing");
    }

    private Integer validateTokenAndGetGroupId(String token) {
        if (token == null)
            throw new IllegalArgumentException("Token is null");
        String jwt = token.startsWith("Bearer ") ? token.substring(7) : token;
        if (!jwtProvider.validateToken(jwt)) {
            throw new org.ssafy.eeum.global.error.exception.CustomException(
                    org.ssafy.eeum.global.error.model.ErrorCode.INVALID_TOKEN);
        }
        org.springframework.security.core.Authentication auth = jwtProvider.getAuthentication(jwt);
        Object principal = auth.getPrincipal();
        if (principal instanceof org.ssafy.eeum.global.auth.model.DeviceDetails) {
            return ((org.ssafy.eeum.global.auth.model.DeviceDetails) principal).getGroupId();
        }
        throw new IllegalArgumentException("Invalid token type for IoT");
    }
}
