package com.family.schedule.web.dto;

import java.time.LocalDate;

public record ScheduleRequest(Integer memberId, LocalDate date, String content) {}
