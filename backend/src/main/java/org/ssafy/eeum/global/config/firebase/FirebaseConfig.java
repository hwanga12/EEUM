package org.ssafy.eeum.global.config.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Firebase Messaging(FCM) 설정을 위한 클래스입니다.
 * 프로젝트의 serviceAccountKey.json 파일을 사용하여 FirebaseApp을 초기화합니다.
 * 
 * @summary Firebase 서비스 설정
 */
@Configuration
public class FirebaseConfig {

    @Value("${firebase.config-path}")
    private String configPath;

    /**
     * FirebaseApp을 초기화하고 Bean으로 등록합니다.
     * 이미 초기화된 경우 기존 객체를 반환합니다.
     * 
     * @summary FirebaseApp Bean 생성
     * @return 초기화된 FirebaseApp 객체
     * @throws IOException 설정 파일 읽기 실패 시 발생
     */
    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        // 이미 초기화된 앱이 있는지 확인
        List<FirebaseApp> firebaseApps = FirebaseApp.getApps();
        if (firebaseApps != null && !firebaseApps.isEmpty()) {
            for (FirebaseApp app : firebaseApps) {
                if (app.getName().equals(FirebaseApp.DEFAULT_APP_NAME)) {
                    return app;
                }
            }
        }

        InputStream serviceAccount = getClass().getResourceAsStream(configPath);
        if (serviceAccount == null) {
            // 리소스를 찾지 못했을 경우 파일 시스템에서 시도
            serviceAccount = new FileInputStream(configPath);
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        return FirebaseApp.initializeApp(options);
    }
}
