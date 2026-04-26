package com.family.schedule.service;

import com.family.schedule.web.dto.ScheduleRequest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ScheduleValidator {

    public static final int MAX_CONTENT_LENGTH = 100;

    public static Map<String, String> validate(ScheduleRequest req, Set<Integer> validMemberIds) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (req == null) {
            errors.put("_", "リクエストが空です");
            return errors;
        }
        if (req.memberId() == null) {
            errors.put("memberId", "誰を選んでください");
        } else if (!validMemberIds.contains(req.memberId())) {
            errors.put("memberId", "不正なメンバーです");
        }
        if (req.date() == null) {
            errors.put("date", "日付を入力してください");
        }
        String content = req.content();
        if (content == null || content.isBlank()) {
            errors.put("content", "内容を入力してください");
        } else if (codePointLength(content) > MAX_CONTENT_LENGTH) {
            errors.put("content", "内容は" + MAX_CONTENT_LENGTH + "文字以内で入力してください");
        }
        return errors;
    }

    static int codePointLength(String s) {
        return s.codePointCount(0, s.length());
    }

    private ScheduleValidator() {}
}
