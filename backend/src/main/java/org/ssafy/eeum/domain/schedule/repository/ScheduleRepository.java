package org.ssafy.eeum.domain.schedule.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.ssafy.eeum.domain.schedule.entity.Schedule;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Integer> {
    List<Schedule> findByGroupIdAndStartAtLessThanEqualAndEndAtGreaterThanEqualAndDeletedAtIsNull(
            Integer groupId, LocalDate endDate, LocalDate startDate);
}