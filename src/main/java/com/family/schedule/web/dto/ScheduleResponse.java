package com.family.schedule.web.dto;

import com.family.schedule.domain.Schedule;

import java.time.LocalDate;

public record ScheduleResponse(
        Long id,
        Integer memberId,
        String memberName,
        LocalDate date,
        String content) {

    public static ScheduleResponse of(Schedule s, String memberName) {
        return new ScheduleResponse(s.getId(), s.getMemberId(), memberName, s.getDate(), s.getContent());
    }
}
