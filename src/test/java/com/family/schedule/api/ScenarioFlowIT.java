package com.family.schedule.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.APIResponse;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TC-IT（結合） と TC-SC（シナリオ）のうち、
 * 画面不要な部分を API 経由で検証する。
 */
class ScenarioFlowIT extends BaseApiTest {

    private final ObjectMapper om = new ObjectMapper();

    @Test
    void TC_IT_01_1_登録の結合_APIとDB() throws Exception {
        // 画面操作に相当: +追加 → 誰が=長女, 日付=今日, 内容="部活" → 保存
        APIResponse res = post("/api/schedules",
                Map.of("memberId", 3, "date", "2026-04-24", "content", "部活"));
        assertThat(res.status()).isEqualTo(201);

        // DB に1件入っている
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM schedules WHERE member_id=3 AND date='2026-04-24' AND content='部活'",
                Integer.class);
        assertThat(count).isEqualTo(1);

        // 後続GETで読める
        APIResponse list = get("/api/schedules?from=2026-04-24&to=2026-04-25");
        assertThat(om.readTree(list.text()).size()).isEqualTo(1);
    }

    @Test
    void TC_IT_02_1_編集の結合_DB反映() throws Exception {
        APIResponse c = post("/api/schedules",
                Map.of("memberId", 3, "date", "2026-04-24", "content", "部活"));
        Long id = om.readTree(c.text()).get("id").asLong();

        APIResponse u = put("/api/schedules/" + id,
                Map.of("memberId", 3, "date", "2026-04-24", "content", "部活（19時まで）"));
        assertThat(u.status()).isEqualTo(200);

        String dbContent = jdbc.queryForObject(
                "SELECT content FROM schedules WHERE id=?", String.class, id);
        assertThat(dbContent).isEqualTo("部活（19時まで）");
    }

    @Test
    void TC_IT_02_2_編集の結合_担当者変更() throws Exception {
        APIResponse c = post("/api/schedules",
                Map.of("memberId", 3, "date", "2026-04-24", "content", "部活"));
        Long id = om.readTree(c.text()).get("id").asLong();

        put("/api/schedules/" + id,
                Map.of("memberId", 4, "date", "2026-04-24", "content", "部活"));

        Integer dbMember = jdbc.queryForObject(
                "SELECT member_id FROM schedules WHERE id=?", Integer.class, id);
        assertThat(dbMember).isEqualTo(4);
    }

    @Test
    void TC_IT_03_1_削除の結合_DBから消える() throws Exception {
        APIResponse c = post("/api/schedules",
                Map.of("memberId", 5, "date", "2026-04-24", "content", "サッカー教室"));
        Long id = om.readTree(c.text()).get("id").asLong();

        APIResponse d = delete("/api/schedules/" + id);
        assertThat(d.status()).isEqualTo(204);

        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM schedules WHERE id=?", Integer.class, id);
        assertThat(count).isEqualTo(0);
    }

    @Test
    void TC_IT_04_1_一覧表示_今日2件明日1件() throws Exception {
        post("/api/schedules", Map.of("memberId", 1, "date", "2026-04-24", "content", "在宅"));
        post("/api/schedules", Map.of("memberId", 3, "date", "2026-04-24", "content", "部活"));
        post("/api/schedules", Map.of("memberId", 4, "date", "2026-04-25", "content", "ピアノ"));

        APIResponse res = get("/api/schedules?from=2026-04-24&to=2026-04-25");
        JsonNode arr = om.readTree(res.text());
        assertThat(arr.size()).isEqualTo(3);
    }

    @Test
    void TC_IT_04_2_一覧表示_0件() throws Exception {
        APIResponse res = get("/api/schedules?from=2026-04-24&to=2026-04-25");
        assertThat(res.status()).isEqualTo(200);
        assertThat(om.readTree(res.text()).size()).isEqualTo(0);
    }

    @Test
    void TC_IT_05_1_フリック相当_日付範囲変更() throws Exception {
        post("/api/schedules", Map.of("memberId", 3, "date", "2026-04-26", "content", "塾"));

        // 初期：2026-04-24,25 → ヒットなし
        APIResponse r1 = get("/api/schedules?from=2026-04-24&to=2026-04-25");
        assertThat(om.readTree(r1.text()).size()).isEqualTo(0);

        // 左フリック相当：2026-04-25,26
        APIResponse r2 = get("/api/schedules?from=2026-04-25&to=2026-04-26");
        JsonNode arr = om.readTree(r2.text());
        assertThat(arr.size()).isEqualTo(1);
        assertThat(arr.get(0).get("content").asText()).isEqualTo("塾");
    }

    // --- TC-SC シナリオテスト（APIレベル版） ---

    @Test
    void TC_SC_06_サロゲートペアと長文削除シナリオ() throws Exception {
        // 絵文字100コードポイント登録
        String long100 = "🏃".repeat(100);
        APIResponse c = post("/api/schedules",
                Map.of("memberId", 3, "date", "2026-04-24", "content", long100));
        assertThat(c.status()).isEqualTo(201);

        // 101 はエラー
        APIResponse over = post("/api/schedules",
                Map.of("memberId", 3, "date", "2026-04-24", "content", "🏃".repeat(101)));
        assertThat(over.status()).isEqualTo(400);

        // 20文字以上の内容の予定を登録 → 削除 → APIが通る（UIはフロント）
        String content = "ピアノ教室（先週の続きから基礎練習をたっぷり）";
        APIResponse c2 = post("/api/schedules",
                Map.of("memberId", 4, "date", "2026-04-24", "content", content));
        Long id = om.readTree(c2.text()).get("id").asLong();
        APIResponse d = delete("/api/schedules/" + id);
        assertThat(d.status()).isEqualTo(204);
    }

    @Test
    void TC_SC_01_3step_シナリオ_APIで検証() throws Exception {
        // お母さんが 長男 の予定を登録（画面3ステップ相当を1 POST で）
        APIResponse res = post("/api/schedules",
                Map.of("memberId", 5, "date", "2026-04-24", "content", "学童お迎え"));
        assertThat(res.status()).isEqualTo(201);
        assertThat(om.readTree(res.text()).get("memberName").asText()).isEqualTo("長男");
    }
}
