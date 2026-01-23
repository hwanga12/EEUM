package org.ssafy.eeum.global.infra.mqtt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MqttService {

    private final MessageChannel mqttOutboundChannel;

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

    @org.springframework.integration.annotation.ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMessage(Message<?> message) {
        String topic = (String) message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
        String payload = (String) message.getPayload();

        log.info("MQTT Message Received - Topic: {}, Payload: {}", topic, payload);

        if ("eeum/sensor/data".equals(topic)) {
            handleSensorData(payload);
        } else if ("eeum/ai/sentiment".equals(topic)) {
            handleSentimentAnalysis(payload);
        } else if ("eeum/family/code".equals(topic)) {
            handleFamilyCodeRequest(payload);
        }
    }

    private void handleSensorData(String payload) {
        // TODO: JSON 파싱 및 DB 저장
        log.info("Processing Sensor Data: {}", payload);
    }

    private void handleSentimentAnalysis(String payload) {
        // TODO: 텍스트 긍부정 판단 로직
        log.info("Processing Sentiment Analysis: {}", payload);
    }

    private void handleFamilyCodeRequest(String payload) {
        // TODO: 가족 코드 조회 및 응답
        log.info("Processing Family Code Request: {}", payload);
    }
}
