package com.family.schedule.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.APIResponse;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulesApiIT extends BaseApiTest {

    private final ObjectMapper om = new ObjectMapper();

    // TC-API-02 GET /api/schedules
    @Test
    void TC_API_02_1_予定0件で空配列() throws Exception {
        APIResponse res = get("/api/schedules?from=2026-04-24&to=2026-04-25");
        assertThat(res.status()).isEqualTo(200);
        JsonNode arr = om.readTree(res.text());
        assertThat(arr.size()).isEqualTo(0);
    }

    @Test
    void TC_API_02_2_複数件取得と順序() throws Exception {
        post("/api/schedules", Map.of("memberId", 3, "date", "2026-04-24", "content", "部活"));
        post("/api/schedules", Map.of("memberId", 1, "date", "2026-04-24", "content", "在宅"));
        post("/api/schedules", Map.of("memberId", 2, "date", "2026-04-25", "content", "病院"));
        APIResponse res = get("/api/schedules?from=2026-04-24&to=2026-04-25");
        assertThat(res.status()).isEqualTo(200);
        JsonNode arr = om.readTree(res.text());
        assertThat(arr.size()).isEqualTo(3);
        // 2026-04-24 先、同一日は memberId 昇順(=display_order)
        assertThat(arr.get(0).get("date").asText()).isEqualTo("2026-04-24");
        assertThat(arr.get(0).get("memberId").asInt()).isEqualTo(1);
        assertThat(arr.get(1).get("memberId").asInt()).isEqualTo(3);
        assertThat(arr.get(2).get("date").asText()).isEqualTo("2026-04-25");
    }

    @Test
    void TC_API_02_3_from未指定は400() {
        APIResponse res = get("/api/schedules?to=2026-04-25");
        assertThat(res.status()).isEqualTo(400);
    }

    @Test
    void TC_API_02_4_fromがtoより後なら400() {
        APIResponse res = get("/api/schedules?from=2026-04-26&to=2026-04-25");
        assertThat(res.status()).isEqualTo(400);
    }

    @Test
    void TC_API_02_5_不正日付形式で400() {
        APIResponse res = get("/api/schedules?from=xxx&to=2026-04-25");
        assertThat(res.status()).isEqualTo(400);
    }

    // TC-API-03 POST
    @Test
    void TC_API_03_1_正常登録() throws Exception {
        APIResponse res = post("/api/schedules",
                Map.of("memberId", 3, "date", "2026-04-24", "content", "部活"));
        assertThat(res.status()).isEqualTo(201);
        assertThat(res.headers().get("location")).contains("/api/schedules/");
        JsonNode body = om.readTree(res.text());
        assertThat(body.get("memberName").asText()).isEqualTo("そよ");
        assertThat(body.get("content").asText()).isEqualTo("部活");
    }

    @Test
    void TC_API_03_2_content空は400() throws Exception {
        APIResponse res = post("/api/schedules",
                Map.of("memberId", 3, "date", "2026-04-24", "content", ""));
        assertThat(res.status()).isEqualTo(400);
        JsonNode body = om.readTree(res.text());
        assertThat(body.get("fields").get("content").asText()).isEqualTo("内容を入力してください");
    }

    @Test
    void TC_API_03_3_content空白のみは400() throws Exception {
        APIResponse res = post("/api/schedules",
                Map.of("memberId", 3, "date", "2026-04-24", "content", "   "));
        assertThat(res.status()).isEqualTo(400);
        JsonNode body = om.readTree(res.text());
        assertThat(body.get("fields").get("content").asText()).isEqualTo("内容を入力してください");
    }

    @Test
    void TC_API_03_4_content101文字は400() throws Exception {
        String s = "あ".repeat(101);
        APIResponse res = post("/api/schedules",
                Map.of("memberId", 3, "date", "2026-04-24", "content", s));
        assertThat(res.status()).isEqualTo(400);
        JsonNode body = om.readTree(res.text());
        assertThat(body.get("fields").get("content").asText()).contains("100文字以内");
    }

    @Test
    void TC_API_03_5_content100文字の境界はOK() {
        String s = "あ".repeat(100);
        APIResponse res = post("/api/schedules",
                Map.of("memberId", 3, "date", "2026-04-24", "content", s));
        assertThat(res.status()).isEqualTo(201);
    }

    @Test
    void TC_API_03_6_絵文字100コードポイントOK() {
        String s = "🏃".repeat(100);
        APIResponse res = post("/api/schedules",
                Map.of("memberId", 3, "date", "2026-04-24", "content", s));
        assertThat(res.status()).isEqualTo(201);
    }

    @Test
    void TC_API_03_7_存在しないmemberIdは400() {
        APIResponse res = post("/api/schedules",
                Map.of("memberId", 99, "date", "2026-04-24", "content", "foo"));
        assertThat(res.status()).isEqualTo(400);
    }

    @Test
    void TC_API_03_8_date未指定は400() {
        APIResponse res = post("/api/schedules",
                Map.of("memberId", 3, "content", "foo"));
        assertThat(res.status()).isEqualTo(400);
    }

    @Test
    void TC_API_03_9_memberId未指定は400() {
        APIResponse res = post("/api/schedules",
                Map.of("date", "2026-04-24", "content", "foo"));
        assertThat(res.status()).isEqualTo(400);
    }

    // TC-API-04 PUT
    @Test
    void TC_API_04_1_正常更新() throws Exception {
        APIResponse created = post("/api/schedules",
                Map.of("memberId", 1, "date", "2026-04-24", "content", "在宅"));
        Long id = om.readTree(created.text()).get("id").asLong();

        APIResponse res = put("/api/schedules/" + id,
                Map.of("memberId", 2, "date", "2026-04-25", "content", "出社"));
        assertThat(res.status()).isEqualTo(200);
        JsonNode body = om.readTree(res.text());
        assertThat(body.get("memberId").asInt()).isEqualTo(2);
        assertThat(body.get("date").asText()).isEqualTo("2026-04-25");
        assertThat(body.get("content").asText()).isEqualTo("出社");
    }

    @Test
    void TC_API_04_2_存在しないIDは404() throws Exception {
        APIResponse res = put("/api/schedules/99999",
                Map.of("memberId", 1, "date", "2026-04-24", "content", "foo"));
        assertThat(res.status()).isEqualTo(404);
    }

    @Test
    void TC_API_04_3_content空は400() throws Exception {
        APIResponse created = post("/api/schedules",
                Map.of("memberId", 1, "date", "2026-04-24", "content", "x"));
        Long id = om.readTree(created.text()).get("id").asLong();
        APIResponse res = put("/api/schedules/" + id,
                Map.of("memberId", 1, "date", "2026-04-24", "content", ""));
        assertThat(res.status()).isEqualTo(400);
    }

    // TC-API-05 DELETE
    @Test
    void TC_API_05_1_正常削除() throws Exception {
        APIResponse created = post("/api/schedules",
                Map.of("memberId", 1, "date", "2026-04-24", "content", "x"));
        Long id = om.readTree(created.text()).get("id").asLong();

        APIResponse res = delete("/api/schedules/" + id);
        assertThat(res.status()).isEqualTo(204);
        // 後続GETで消えている
        APIResponse list = get("/api/schedules?from=2026-04-24&to=2026-04-25");
        assertThat(om.readTree(list.text()).size()).isEqualTo(0);
    }

    @Test
    void TC_API_05_2_存在しないIDは404() {
        APIResponse res = delete("/api/schedules/99999");
        assertThat(res.status()).isEqualTo(404);
    }

    @Test
    void TC_API_05_3_2回目の削除は404() throws Exception {
        APIResponse created = post("/api/schedules",
                Map.of("memberId", 1, "date", "2026-04-24", "content", "x"));
        Long id = om.readTree(created.text()).get("id").asLong();
        assertThat(delete("/api/schedules/" + id).status()).isEqualTo(204);
        assertThat(delete("/api/schedules/" + id).status()).isEqualTo(404);
    }
}
