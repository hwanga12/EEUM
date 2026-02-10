package org.ssafy.eeum.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 외부 API 통신을 위한 RestTemplate 설정 클래스입니다.
 * 
 * @summary RestTemplate 설정
 */
@Configuration
public class RestTemplateConfig {

    /**
     * 공통으로 사용할 RestTemplate Bean을 생성합니다.
     * 
     * @summary RestTemplate Bean 생성
     * @return RestTemplate 객체
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
