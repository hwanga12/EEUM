package org.ssafy.eeum.domain.schedule.dto;

import lombok.Builder;
import lombok.Getter;
import org.ssafy.eeum.domain.schedule.entity.CategoryType;
import org.ssafy.eeum.domain.schedule.entity.RepeatType;

import java.time.LocalDate;

@Getter
@Builder
public class ScheduleResponseDTO {
    private Integer scheduleId;
    private String title;
    private CategoryType categoryType;
    private LocalDate startAt;
    private LocalDate endAt;
    private String visitorName;
    private String visitPurpose;
    private String targetPerson;
    private RepeatType repeatType;
}