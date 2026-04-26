package com.family.schedule.service;

import com.family.schedule.web.dto.ScheduleRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleValidatorTest {

    private static final Set<Integer> MEMBERS = Set.of(1, 2, 3, 4, 5);

    @Test
    void 正常リクエストはエラーなし() {
        Map<String, String> e = ScheduleValidator.validate(
                new ScheduleRequest(3, LocalDate.of(2026, 4, 24), "塾"), MEMBERS);
        assertThat(e).isEmpty();
    }

    @Test
    void content_空文字はエラー() {
        Map<String, String> e = ScheduleValidator.validate(
                new ScheduleRequest(1, LocalDate.now(), ""), MEMBERS);
        assertThat(e).containsEntry("content", "内容を入力してください");
    }

    @Test
    void content_空白のみはエラー() {
        Map<String, String> e = ScheduleValidator.validate(
                new ScheduleRequest(1, LocalDate.now(), "   "), MEMBERS);
        assertThat(e).containsEntry("content", "内容を入力してください");
    }

    @Test
    void content_100文字は境界OK() {
        String s = "あ".repeat(100);
        Map<String, String> e = ScheduleValidator.validate(
                new ScheduleRequest(1, LocalDate.now(), s), MEMBERS);
        assertThat(e).isEmpty();
    }

    @Test
    void content_101文字はエラー() {
        String s = "あ".repeat(101);
        Map<String, String> e = ScheduleValidator.validate(
                new ScheduleRequest(1, LocalDate.now(), s), MEMBERS);
        assertThat(e).containsEntry("content", "内容は100文字以内で入力してください");
    }

    @Test
    void content_絵文字100コードポイントはOK() {
        String emoji = "🏃";
        String s = emoji.repeat(100);
        Map<String, String> e = ScheduleValidator.validate(
                new ScheduleRequest(1, LocalDate.now(), s), MEMBERS);
        assertThat(e).isEmpty();
    }

    @Test
    void content_絵文字101コードポイントはエラー() {
        String emoji = "🏃";
        String s = emoji.repeat(101);
        Map<String, String> e = ScheduleValidator.validate(
                new ScheduleRequest(1, LocalDate.now(), s), MEMBERS);
        assertThat(e).containsEntry("content", "内容は100文字以内で入力してください");
    }

    @Test
    void memberId_未指定はエラー() {
        Map<String, String> e = ScheduleValidator.validate(
                new ScheduleRequest(null, LocalDate.now(), "foo"), MEMBERS);
        assertThat(e).containsEntry("memberId", "誰を選んでください");
    }

    @Test
    void memberId_範囲外はエラー() {
        Map<String, String> e = ScheduleValidator.validate(
                new ScheduleRequest(99, LocalDate.now(), "foo"), MEMBERS);
        assertThat(e).containsEntry("memberId", "不正なメンバーです");
    }

    @Test
    void date_未指定はエラー() {
        Map<String, String> e = ScheduleValidator.validate(
                new ScheduleRequest(1, null, "foo"), MEMBERS);
        assertThat(e).containsEntry("date", "日付を入力してください");
    }
}
