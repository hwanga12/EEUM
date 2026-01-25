package org.ssafy.eeum.domain.schedule.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.ssafy.eeum.domain.schedule.entity.CategoryType;
import org.ssafy.eeum.domain.schedule.entity.RepeatType;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class ScheduleRequestDTO {
    private String title;
    private LocalDate startAt;
    private LocalDate endAt;
    private CategoryType categoryType;
    private RepeatType repeatType;
    private Boolean isLunar;
    private String targetPerson;
    private String visitorName;
    private String visitPurpose;
}