package org.ssafy.eeum.domain.schedule.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ssafy.eeum.domain.schedule.dto.ScheduleRequestDTO;
import org.ssafy.eeum.domain.schedule.dto.ScheduleResponseDTO;
import org.ssafy.eeum.domain.schedule.entity.RepeatType;
import org.ssafy.eeum.domain.schedule.entity.Schedule;
import org.ssafy.eeum.domain.schedule.repository.ScheduleRepository;
import org.ssafy.eeum.domain.user.entity.User;
import org.ssafy.eeum.global.error.exception.CustomException;
import org.ssafy.eeum.global.error.model.ErrorCode;
import org.ssafy.eeum.global.infra.redis.RedisService;
import org.ssafy.eeum.global.util.CalendarUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {
    private final ScheduleRepository scheduleRepository;
    private final RedisService redisService;

    // 월간 일정 조회 (캐시 적용)
    public List<ScheduleResponseDTO> getMonthlySchedules(Integer familyId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        YearMonth targetMonth = YearMonth.of(year, month);
        String cacheKey = "family:" + familyId + ":schedule:" + targetMonth;

        List<ScheduleResponseDTO> cachedData = redisService.getList(cacheKey, ScheduleResponseDTO.class);
        if (cachedData != null) {
            return cachedData;
        }

        List<ScheduleResponseDTO> result = calculateMonthlySchedules(familyId, targetMonth);

        redisService.setList(cacheKey, result, Duration.ofDays(1));
        return result;
    }

    // DB 데이터를 읽어 해당 월의 실제 날짜들로 확장
    private List<ScheduleResponseDTO> calculateMonthlySchedules(Integer familyId, YearMonth ym) {
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<Schedule> candidates = scheduleRepository.findCandidates(familyId, start, end);

        Set<String> modifiedMask = candidates.stream()
                .filter(s -> s.getParentId() != null)
                .map(s -> s.getParentId() + "_" + s.getStartAt())
                .collect(Collectors.toSet());

        List<ScheduleResponseDTO> result = new ArrayList<>();

        for (Schedule s : candidates) {

            List<LocalDate> occurrences = calculateOccurrenceDates(s, start, end);

            for (LocalDate date : occurrences) {
                String virtualId = (s.getRepeatType() == RepeatType.NONE) ? s.getId().toString() : s.getId() + "_" + date;

                if (!modifiedMask.contains(s.getId() + "_" + date)) {
                    result.add(convertToResponse(s, virtualId, date, s.getParentId() != null));
                }
            }
        }
        return result.stream().sorted(Comparator.comparing(ScheduleResponseDTO::getStartAt)).toList();
    }

    // 반복 규칙과 음력 설정을 고려하여 해당 월 내의 발생 날짜들 계산
    private List<LocalDate> calculateOccurrenceDates(Schedule s, LocalDate start, LocalDate end) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate baseDate = s.getStartAt();
        LocalDate limitDate = s.getRecurrenceEndAt();
        LocalDate targetDate;

        if (s.getRepeatType() == RepeatType.NONE) {
            if (Boolean.TRUE.equals(s.getIsLunar())) {
                targetDate = CalendarUtils.convertLunarToSolar(baseDate);
            } else {
                targetDate = baseDate;
            }
        } else {
            if (Boolean.TRUE.equals(s.getIsLunar())) {
                targetDate = CalendarUtils.convertLunarToSolar(baseDate.withYear(start.getYear()));
            } else {
                targetDate = baseDate.withYear(start.getYear());
            }
        }

        // 반복 없음 (NONE)
        if (s.getRepeatType() == RepeatType.NONE) {
            if (!targetDate.isBefore(start) && !targetDate.isAfter(end)) {
                dates.add(targetDate);
            }
        }

        // 매달 반복 (MONTHLY)
        else if (s.getRepeatType() == RepeatType.MONTHLY) {
            int day = Math.min(baseDate.getDayOfMonth(), start.lengthOfMonth());
            LocalDate occurrence = start.withDayOfMonth(day);

            if (!occurrence.isBefore(baseDate) && (limitDate == null || !occurrence.isAfter(limitDate))) {
                dates.add(occurrence);
            }
        }

        // 매년 반복 (YEARLY)
        else if (s.getRepeatType() == RepeatType.YEARLY) {
            boolean isWithinRange = !targetDate.isBefore(start) && !targetDate.isAfter(end);
            boolean isAfterStart = !targetDate.isBefore(baseDate);
            boolean isBeforeLimit = (limitDate == null || !targetDate.isAfter(limitDate));

            if (isWithinRange && isAfterStart && isBeforeLimit) {
                dates.add(targetDate);
            }
        }

        return dates;
    }

    private ScheduleResponseDTO convertToResponse(Schedule s, String id, LocalDate date, boolean isModified) {
        return ScheduleResponseDTO.builder()
                .scheduleId(id)
                .title(s.getTitle())
                .startAt(date)
                .endAt(date)
                .categoryType(s.getCategoryType())
                .description(s.getDescription())
                .visitorName(s.getVisitorName())
                .visitPurpose(s.getVisitPurpose())
                .isVisited(s.getIsVisited())
                .repeatType(s.getRepeatType())
                .isLunar(s.getIsLunar())
                .isModified(isModified)
                .targetPerson(s.getTargetPerson())
                .build();
    }

    // 일정 등록
    @Transactional
    public void createSchedule(Integer familyId, User creator, ScheduleRequestDTO dto) {
        Schedule schedule = Schedule.builder()
                .groupId(familyId)
                .creator(creator)
                .title(dto.getTitle())
                .startAt(dto.getStartAt())
                .endAt(dto.getEndAt())
                .categoryType(dto.getCategoryType())
                .description(dto.getDescription())
                .repeatType(dto.getRepeatType())
                .isLunar(dto.getIsLunar())
                .targetPerson(dto.getTargetPerson())
                .visitorName(dto.getVisitorName())
                .visitPurpose(dto.getVisitPurpose())
                .isVisited(false)
                .build();

        scheduleRepository.save(schedule);
        invalidateCache(familyId, dto.getStartAt());
    }

    // 일정 수정
    @Transactional
    public void updateSchedule(Integer familyId, String scheduleId, ScheduleRequestDTO dto) {
        if (scheduleId.contains("_")) {
            String[] parts = scheduleId.split("_");
            Integer parentId = Integer.parseInt(parts[0]);
            LocalDate targetDate = LocalDate.parse(parts[1]);

            Schedule schedule = scheduleRepository.findByParentIdAndStartAtAndDeletedAtIsNull(parentId, targetDate)
                    .orElseGet(() -> {
                        Schedule parent = scheduleRepository.findById(parentId)
                                .orElseThrow(() -> new CustomException(ErrorCode.ENTITY_NOT_FOUND));
                        return Schedule.builder()
                                .groupId(parent.getGroupId())
                                .creator(parent.getCreator())
                                .parentId(parentId)
                                .categoryType(parent.getCategoryType())
                                .build();
                    });

            schedule.update(
                    dto.getTitle(),
                    dto.getStartAt(),
                    dto.getEndAt(),
                    null,
                    dto.getDescription(),
                    dto.getVisitorName(),
                    dto.getVisitPurpose(),
                    RepeatType.NONE,
                    dto.getIsLunar(),
                    dto.getTargetPerson()
            );

            scheduleRepository.save(schedule);
            invalidateCache(familyId, targetDate);
            invalidateCache(familyId, dto.getStartAt());

        } else {
            Schedule schedule = scheduleRepository.findById(Integer.parseInt(scheduleId))
                    .orElseThrow(() -> new CustomException(ErrorCode.ENTITY_NOT_FOUND));

            LocalDate oldDate = schedule.getStartAt();

            schedule.update(
                    dto.getTitle(),
                    dto.getStartAt(),
                    dto.getEndAt(),
                    dto.getRecurrenceEndAt(),
                    dto.getDescription(),
                    dto.getVisitorName(),
                    dto.getVisitPurpose(),
                    dto.getRepeatType(),
                    dto.getIsLunar(),
                    dto.getTargetPerson()
            );

            invalidateCache(familyId, oldDate);
            invalidateCache(familyId, dto.getStartAt());
        }
    }

    // 일정 삭제
    public void deleteSchedule(Integer familyId, String scheduleId) {
        if (scheduleId.contains("_")) {
            String[] parts = scheduleId.split("_");
            Integer parentId = Integer.parseInt(parts[0]);
            LocalDate targetDate = LocalDate.parse(parts[1]);

            Schedule exclusion = Schedule.builder()
                    .groupId(familyId)
                    .parentId(parentId)
                    .startAt(targetDate)
                    .endAt(targetDate)
                    .repeatType(RepeatType.NONE)
                    .title("EXCLUDED")
                    .build();
            scheduleRepository.save(exclusion);
            scheduleRepository.delete(exclusion);

            invalidateCache(familyId, targetDate);
        } else {
            Schedule schedule = scheduleRepository.findById(Integer.parseInt(scheduleId))
                    .orElseThrow(() -> new CustomException(ErrorCode.ENTITY_NOT_FOUND));
            scheduleRepository.delete(schedule);
            invalidateCache(familyId, schedule.getStartAt());
        }
    }

    private void invalidateCache(Integer familyId, LocalDate date) {
        String key = "family:" + familyId + ":schedule:" + YearMonth.from(date);
        redisService.deleteData(key);
    }
}