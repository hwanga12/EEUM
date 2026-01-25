package org.ssafy.eeum.domain.schedule.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ssafy.eeum.domain.schedule.dto.ScheduleRequestDTO;
import org.ssafy.eeum.domain.schedule.dto.ScheduleResponseDTO;
import org.ssafy.eeum.domain.schedule.entity.Schedule;
import org.ssafy.eeum.domain.schedule.repository.ScheduleRepository;
import org.ssafy.eeum.domain.user.entity.User;
import org.ssafy.eeum.global.error.exception.CustomException;
import org.ssafy.eeum.global.error.model.ErrorCode;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {
    private final ScheduleRepository scheduleRepository;

    @Transactional
    public void saveSchedule(Integer groupId, User user, ScheduleRequestDTO dto) {
        Schedule schedule = Schedule.builder()
                .groupId(groupId).creator(user).title(dto.getTitle())
                .startAt(dto.getStartAt()).endAt(dto.getEndAt())
                .categoryType(dto.getCategoryType()).repeatType(dto.getRepeatType())
                .isLunar(dto.getIsLunar()).targetPerson(dto.getTargetPerson())
                .visitorName(dto.getVisitorName()).visitPurpose(dto.getVisitPurpose())
                .isVisited(false).build();
        scheduleRepository.save(schedule);
    }

    public List<ScheduleResponseDTO> getSchedules(Integer groupId, LocalDate start, LocalDate end) {
        return scheduleRepository.findByGroupIdAndStartAtLessThanEqualAndEndAtGreaterThanEqualAndDeletedAtIsNull(groupId, end, start)
                .stream().map(s -> ScheduleResponseDTO.builder()
                        .scheduleId(s.getId()).title(s.getTitle()).categoryType(s.getCategoryType())
                        .startAt(s.getStartAt()).endAt(s.getEndAt())
                        .visitorName(s.getVisitorName()).visitPurpose(s.getVisitPurpose())
                        .targetPerson(s.getTargetPerson()).repeatType(s.getRepeatType()).build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteSchedule(Integer scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new CustomException(ErrorCode.ENTITY_NOT_FOUND));
        scheduleRepository.delete(schedule);
    }
}