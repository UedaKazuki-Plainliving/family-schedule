package com.family.schedule.explore;

import com.family.schedule.e2e.BaseE2ETest;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DateBoundaryExploreTest extends BaseE2ETest {

    @Test
    void EX005_T1_閏日2028_02_29に登録できる() {
        context.request().post(baseUrl() + "/api/schedules",
                RequestOptions.create().setData(Map.of(
                        "memberId", 1, "date", "2028-02-29", "content", "閏日テスト")));
        var res = context.request().get(baseUrl() + "/api/schedules?from=2028-02-29&to=2028-02-29");
        assertThat(res.status()).isEqualTo(200);
        assertThat(res.text()).contains("閏日テスト");
    }

    @Test
    void EX005_T2_存在しない閏日2027_02_29を弾く() {
        var res = context.request().post(baseUrl() + "/api/schedules",
                RequestOptions.create().setData(Map.of(
                        "memberId", 1, "date", "2027-02-29", "content", "存在しない閏日")));
        assertThat(res.status()).isEqualTo(400);
    }

    @Test
    void EX005_T3_2026_13_01は弾かれる() {
        var res = context.request().post(baseUrl() + "/api/schedules",
                RequestOptions.create().setData(Map.of(
                        "memberId", 1, "date", "2026-13-01", "content", "13月")));
        assertThat(res.status()).isEqualTo(400);
    }

    @Test
    void EX005_T4_年またぎフリック_2026_12_31から2027_01_01へ() {
        // 12/31 と 1/1 にデータを入れて、画面で見られるか
        context.request().post(baseUrl() + "/api/schedules",
                RequestOptions.create().setData(Map.of(
                        "memberId", 1, "date", "2026-12-31", "content", "大晦日")));
        context.request().post(baseUrl() + "/api/schedules",
                RequestOptions.create().setData(Map.of(
                        "memberId", 1, "date", "2027-01-01", "content", "元旦")));

        // ブラウザを「2026-12-31」に時刻シフトしたいが、Playwrightの仕様で偽装は限定的。
        // 代わりに API レイヤで両日の取得を確認
        var res = context.request().get(baseUrl() + "/api/schedules?from=2026-12-31&to=2027-01-01");
        assertThat(res.status()).isEqualTo(200);
        String body = res.text();
        assertThat(body).contains("大晦日").contains("元旦");
    }

    @Test
    void EX005_T5_過去極大1900年1月1日の登録() {
        var res = context.request().post(baseUrl() + "/api/schedules",
                RequestOptions.create().setData(Map.of(
                        "memberId", 1, "date", "1900-01-01", "content", "明治33年")));
        assertThat(res.status()).isEqualTo(201);
    }

    @Test
    void EX005_T6_未来極大9999年12月31日の登録() {
        var res = context.request().post(baseUrl() + "/api/schedules",
                RequestOptions.create().setData(Map.of(
                        "memberId", 1, "date", "9999-12-31", "content", "remote future")));
        assertThat(res.status()).isEqualTo(201);
    }
}
