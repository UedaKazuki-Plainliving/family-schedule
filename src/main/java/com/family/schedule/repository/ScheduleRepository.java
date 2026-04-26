package com.family.schedule.repository;

import com.family.schedule.domain.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByDateBetweenAndDeletedAtIsNullOrderByDateAscMemberIdAscIdAsc(LocalDate from, LocalDate to);
    Optional<Schedule> findByIdAndDeletedAtIsNull(Long id);
    Optional<Schedule> findByIdAndDeletedAtIsNotNull(Long id);
    void deleteByMemberIdAndDeletedAtIsNotNull(Integer memberId);
}
