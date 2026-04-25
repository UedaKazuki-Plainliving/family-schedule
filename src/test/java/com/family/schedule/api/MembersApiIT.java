package com.family.schedule.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.APIResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MembersApiIT extends BaseApiTest {

    @Test
    void TC_API_01_1_正常() throws Exception {
        APIResponse res = get("/api/members");
        assertThat(res.status()).isEqualTo(200);
        JsonNode arr = new ObjectMapper().readTree(res.text());
        assertThat(arr.size()).isEqualTo(5);
        assertThat(arr.get(0).get("name").asText()).isEqualTo("お父さん");
        assertThat(arr.get(4).get("name").asText()).isEqualTo("長男");
        int prev = 0;
        for (JsonNode m : arr) {
            int order = m.get("displayOrder").asInt();
            assertThat(order).isGreaterThan(prev);
            prev = order;
        }
    }
}
