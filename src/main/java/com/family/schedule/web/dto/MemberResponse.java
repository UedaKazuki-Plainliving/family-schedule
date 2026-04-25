package com.family.schedule.web.dto;

import com.family.schedule.domain.Member;

public record MemberResponse(Integer id, String name, Integer displayOrder) {
    public static MemberResponse of(Member m) {
        return new MemberResponse(m.getId(), m.getName(), m.getDisplayOrder());
    }
}
