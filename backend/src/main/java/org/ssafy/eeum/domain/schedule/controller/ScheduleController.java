package org.ssafy.eeum.domain.schedule.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.ssafy.eeum.domain.schedule.dto.ScheduleRequestDTO;
import org.ssafy.eeum.domain.schedule.dto.ScheduleResponseDTO;
import org.ssafy.eeum.domain.schedule.service.ScheduleService;
import org.ssafy.eeum.global.auth.model.CustomUserDetails;
import org.ssafy.eeum.global.common.response.RestApiResponse;
import org.ssafy.eeum.global.config.swagger.SwaggerApiSpec;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Schedule", description = "가족 일정 관리 API")
@RestController
@RequestMapping("/api/families/{familyId}/schedules")
@RequiredArgsConstructor
public class ScheduleController {
    private final ScheduleService scheduleService;

    @SwaggerApiSpec(summary = "일정 전체 조회", description = "기간별 가족 일정을 조회합니다.")
    @GetMapping
    public RestApiResponse<List<ScheduleResponseDTO>> getSchedules(
            @PathVariable Integer familyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return RestApiResponse.success(scheduleService.getSchedules(familyId, startDate, endDate));
    }

    @SwaggerApiSpec(summary = "방문 일정 등록", description = "가족 방문 일정을 등록합니다.")
    @PostMapping("/visit")
    public RestApiResponse<Void> createVisit(
            @PathVariable Integer familyId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody ScheduleRequestDTO request) {
        scheduleService.saveSchedule(familyId, userDetails.getUser(), request);
        return RestApiResponse.success("방문 일정 등록 완료");
    }

    @SwaggerApiSpec(summary = "경조사 일정 등록", description = "생신, 제사 등 경조사 일정을 등록합니다.")
    @PostMapping("/anniversaries")
    public RestApiResponse<Void> createAnniversary(
            @PathVariable Integer familyId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody ScheduleRequestDTO request) {
        scheduleService.saveSchedule(familyId, userDetails.getUser(), request);
        return RestApiResponse.success("경조사 일정 등록 완료");
    }

    @SwaggerApiSpec(summary = "일정 삭제", description = "일정을 삭제합니다.")
    @DeleteMapping("/{scheduleId}")
    public RestApiResponse<Void> deleteSchedule(@PathVariable Integer scheduleId) {
        scheduleService.deleteSchedule(scheduleId);
        return RestApiResponse.success("일정 삭제 완료");
    }
}