package org.ssafy.eeum.domain.iot.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.ssafy.eeum.domain.iot.service.IotVideoService;
import org.ssafy.eeum.global.common.response.RestApiResponse;
import org.ssafy.eeum.global.config.swagger.SwaggerApiSpec;

@Tag(name = "IoT Video", description = "IoT 기기 영상 업로드 API")
@RestController
@RequestMapping("/api/v1/iot/videos")
@RequiredArgsConstructor
public class IotVideoController {

    private final IotVideoService iotVideoService;

    @SwaggerApiSpec(
            summary = "영상 업로드용 Presigned URL 발급",
            description = "Jetson 기기에서 S3에 영상을 직접 업로드하기 위한 URL을 발급합니다.",
            successMessage = "URL 발급 성공"
    )
    @GetMapping("/presigned-url")
    public RestApiResponse<String> getUrl(@RequestParam Integer groupId) {
        return RestApiResponse.success(iotVideoService.getUploadUrl(groupId));
    }

    @SwaggerApiSpec(
            summary = "영상 업로드 완료 알림",
            description = "기기에서 S3 업로드를 마친 후 서버에 완료 상태를 알립니다.",
            successMessage = "업로드 완료 처리 성공"
    )
    @PostMapping("/complete")
    public RestApiResponse<Void> completeUpload(@RequestParam Integer groupId, @RequestParam String s3Key) {
        iotVideoService.completeVideoUpload(groupId, s3Key);
        return RestApiResponse.success("영상 정보가 성공적으로 기록되었습니다.");
    }
}