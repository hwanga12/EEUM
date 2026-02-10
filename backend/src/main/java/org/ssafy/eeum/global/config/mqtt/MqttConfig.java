package org.ssafy.eeum.global.config.mqtt;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.inbound.Mqttv5PahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.Mqttv5PahoMessageHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.UUID;

/**
 * MQTT 통신(Eclipse Paho V5)을 위한 설정 클래스입니다.
 * 센서 데이터 수신 및 제어 메시지 송신을 위한 채널과 어댑터를 설정합니다.
 * 
 * @summary MQTT 서비스 설정
 */
@Slf4j
@Configuration
public class MqttConfig {

    @Value("${spring.mqtt.url}")
    private String url;

    @Value("${spring.mqtt.client-id}")
    private String clientId;

    @Value("${spring.mqtt.default-topic}")
    private String defaultTopic;

    @Value("${spring.mqtt.username}")
    private String username;

    @Value("${spring.mqtt.password}")
    private String password;

    @Value("${spring.mqtt.inbound-topics}")
    private String[] inboundTopics;

    /**
     * MQTT 연결 옵션을 설정합니다. SSL/TLS 설정 및 사용자 인증 정보를 포함합니다.
     * 
     * @summary MQTT 연결 옵션 설정
     * @return MqttConnectionOptions 객체
     */
    @Bean
    public MqttConnectionOptions mqttConnectionOptions() {
        MqttConnectionOptions options = new MqttConnectionOptions();

        options.setServerURIs(new String[] { url });
        options.setUserName(username);
        options.setPassword(password.getBytes(StandardCharsets.UTF_8));

        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] {
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            }, new java.security.SecureRandom());
            options.setSocketFactory(sslContext.getSocketFactory());
        } catch (Exception e) {
            log.error("MQTT SSL/TLS 설정 중 오류가 발생했습니다: {}", e.getMessage());
        }

        options.setCleanStart(true);
        options.setKeepAliveInterval(60);
        options.setAutomaticReconnect(true);

        return options;
    }

    /**
     * MQTT 메시정 유입을 위한 입력 채널을 생성합니다.
     * 
     * @summary MQTT 입력 채널 생성
     * @return MessageChannel 객체
     */
    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    /**
     * MQTT 인바운드 어댑터를 설정하여 브로커로부터 메시지를 수신합니다.
     * 
     * @summary MQTT 인바운드 어댑터 설정
     * @param mqttConnectionOptions 연결 옵션
     * @return Mqttv5PahoMessageDrivenChannelAdapter 객체
     */
    @Bean
    public Mqttv5PahoMessageDrivenChannelAdapter inbound(MqttConnectionOptions mqttConnectionOptions) {
        String uniqueInboundId = clientId + "-in-" + UUID.randomUUID().toString().substring(0, 5);

        Mqttv5PahoMessageDrivenChannelAdapter adapter = new Mqttv5PahoMessageDrivenChannelAdapter(
                mqttConnectionOptions, uniqueInboundId,
                inboundTopics);

        adapter.setCompletionTimeout(5000);
        adapter.setQos(1);
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }

    /**
     * MQTT 메시지 전송을 위한 출력 채널을 생성합니다.
     * 
     * @summary MQTT 출력 채널 생성
     * @return MessageChannel 객체
     */
    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    /**
     * MQTT 아웃바운드 어댑터를 설정하여 브로커로 메시지를 전송합니다.
     * 
     * @summary MQTT 아웃바운드 어댑터 설정
     * @param mqttConnectionOptions 연결 옵션
     * @return MessageHandler 객체
     */
    @Bean
    public MessageHandler mqttOutbound(MqttConnectionOptions mqttConnectionOptions) {
        String uniqueOutboundId = clientId + "-out-" + UUID.randomUUID().toString().substring(0, 5);

        Mqttv5PahoMessageHandler messageHandler = new Mqttv5PahoMessageHandler(mqttConnectionOptions, uniqueOutboundId);

        messageHandler.setAsync(true);
        messageHandler.setDefaultTopic(defaultTopic);

        return messageHandler;
    }
}
