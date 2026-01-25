package org.ssafy.eeum.domain.family.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ssafy.eeum.domain.family.dto.CreateFamilyRequestDto;
import org.ssafy.eeum.domain.family.dto.CreateFamilyResponseDto;
import org.ssafy.eeum.domain.family.dto.FamilyMemberDto;
import org.ssafy.eeum.domain.family.dto.FamilyMemberDetailResponseDto;
import org.ssafy.eeum.domain.family.dto.FamilySimpleResponseDto;
import org.ssafy.eeum.domain.family.dto.LeaveFamilyResponseDto;
import org.ssafy.eeum.domain.family.dto.UpdateFamilyRequestDto;
import org.ssafy.eeum.domain.family.dto.UpdateFamilyResponseDto;
import org.ssafy.eeum.domain.family.service.FamilyService;
import org.ssafy.eeum.global.auth.model.CustomUserDetails;

import java.util.List;

@Tag(name = "family", description = "가족 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/families")
public class FamilyController {

    private final FamilyService familyService;

    @Operation(summary = "가족 그룹 생성", description = "새로운 가족 그룹을 생성합니다.")
    @PostMapping
    public ResponseEntity<CreateFamilyResponseDto> createFamily(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody CreateFamilyRequestDto createFamilyRequestDto) {
        String userId = userDetails.getUsername();
        CreateFamilyResponseDto responseDto = familyService.createFamily(userId, createFamilyRequestDto);
        return ResponseEntity.ok(responseDto);
    }

    @Operation(summary = "가족 목록 조회", description = "현재 유저가 속한 모든 가족 그룹의 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<FamilySimpleResponseDto>> getMyFamilies(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String userId = userDetails.getUsername();
        List<FamilySimpleResponseDto> responseDto = familyService.findMyFamilies(userId);
        return ResponseEntity.ok(responseDto);
    }

    @Operation(summary = "가족 그룹 멤버 목록 조회", description = "특정 가족 그룹에 속한 모든 멤버의 목록을 조회합니다.")
    @GetMapping("/{familyId}/members")
    public ResponseEntity<List<FamilyMemberDto>> getFamilyMembers(
            @PathVariable Long familyId) {
        List<FamilyMemberDto> responseDto = familyService.getFamilyMembers(familyId);
        return ResponseEntity.ok(responseDto);
    }

    @Operation(summary = "가족 그룹 멤버 상세 조회", description = "특정 가족 그룹 멤버의 상세 정보를 조회합니다.")
    @GetMapping("/{familyId}/members/{memberUserId}")
    public ResponseEntity<FamilyMemberDetailResponseDto> getFamilyMemberDetails(
            @PathVariable Long familyId,
            @PathVariable Long memberUserId) {
        FamilyMemberDetailResponseDto responseDto = familyService.getFamilyMemberDetails(familyId, memberUserId);
        return ResponseEntity.ok(responseDto);
    }

    @Operation(summary = "가족 그룹 탈퇴/삭제", description = "가족 그룹에서 탈퇴하거나, 대표자일 경우 그룹을 삭제합니다.")
    @DeleteMapping("/{familyId}/leave")
    public ResponseEntity<LeaveFamilyResponseDto> leaveFamily(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long familyId) {
        String userId = userDetails.getUsername();
        LeaveFamilyResponseDto responseDto = familyService.leaveFamily(userId, familyId);
        return ResponseEntity.ok(responseDto);
    }

    @Operation(summary = "가족 그룹 설정 수정", description = "가족 그룹의 이름, 피부양자 설정, 피부양자 건강 정보, 멤버 응급 우선순위를 수정합니다.")
    @PutMapping("/{familyId}")
    public ResponseEntity<UpdateFamilyResponseDto> updateFamily(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long familyId,
            @RequestBody UpdateFamilyRequestDto requestDto) {
        String userId = userDetails.getUsername();
        UpdateFamilyResponseDto responseDto = familyService.updateFamily(userId, familyId, requestDto);
        return ResponseEntity.ok(responseDto);
    }

    @Operation(summary = "가족 그룹 멤버 삭제", description = "가족 대표자가 다른 멤버를 그룹에서 삭제합니다.")
    @DeleteMapping("/{familyId}/members/{memberUserId}")
    public ResponseEntity<Void> deleteFamilyMember(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long familyId,
            @PathVariable Long memberUserId) {
        String authenticatedUserId = userDetails.getUsername();
        familyService.deleteFamilyMember(authenticatedUserId, familyId, memberUserId);
        return ResponseEntity.noContent().build();
    }
}
