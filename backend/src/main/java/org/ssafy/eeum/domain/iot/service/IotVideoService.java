package org.ssafy.eeum.domain.iot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.ssafy.eeum.global.infra.s3.S3Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IotVideoService {

    private final S3Service s3Service;

    // 1. 비디오 업로드용 Presigned URL 발급
    public String getUploadUrl(Integer groupId) {
        String fileName = String.format("videos/%d/%s.mp4", groupId, UUID.randomUUID());
        return s3Service.generatePresignedUrl(fileName, "video/mp4");
    }

    // 2. 업로드 완료 처리 (DB 기록 등)
    public void completeVideoUpload(Integer groupId, String s3Key) {
        // TODO: DB에 영상 정보(s3Key, 등록 시간 등)를 저장하는 로직 추가
    }
}
