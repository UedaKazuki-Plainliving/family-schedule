package com.family.schedule.explore;

import com.family.schedule.e2e.BaseE2ETest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocalStorageExploreTest extends BaseE2ETest {

    @Test
    void EX004_T1_壊れたJSONがLocalStorageにあるとき() {
        context.addInitScript(
                "localStorage.setItem('familySchedule.currentUser','this is not json');");
        page.navigate(baseUrl() + "/");
        page.waitForTimeout(1500);

        String body = page.locator("body").innerText();
        boolean stillUsable = page.locator("#screen-select-user").isVisible()
                || page.locator("#screen-schedule").isVisible();
        snap("EX004_T1_brokenJSON");
        assertThat(stillUsable)
                .as("壊れたJSONでもアプリが使える状態であるべき").isTrue();
        // 失敗時に文字列も残す
        if (!stillUsable) System.err.println("BODY=" + body);
    }

    @Test
    void EX004_T2_存在しないmemberId() {
        context.addInitScript(
                "localStorage.setItem('familySchedule.currentUser','{\"id\":99,\"name\":\"hacker\",\"displayOrder\":99}');");
        page.navigate(baseUrl() + "/");
        page.waitForTimeout(1500);
        // 不正なid=99で起動した場合、何が起きるか
        snap("EX004_T2_invalid_member_id");
        // 期待：それでも画面は表示される（id=99 はサーバーには存在しないが UI は使える）
        boolean ok = page.locator("#screen-schedule").isVisible() || page.locator("#screen-select-user").isVisible();
        assertThat(ok).isTrue();
    }

    @Test
    void EX004_T3_idがnumberではなく文字列() {
        context.addInitScript(
                "localStorage.setItem('familySchedule.currentUser','{\"id\":\"abc\",\"name\":\"x\",\"displayOrder\":1}');");
        page.navigate(baseUrl() + "/");
        page.waitForTimeout(1500);
        snap("EX004_T3_string_id");
        boolean ok = page.locator("#screen-schedule").isVisible() || page.locator("#screen-select-user").isVisible();
        assertThat(ok).isTrue();
    }

    @Test
    void EX004_T4_空のJSON() {
        context.addInitScript(
                "localStorage.setItem('familySchedule.currentUser','{}');");
        page.navigate(baseUrl() + "/");
        page.waitForTimeout(1500);
        snap("EX004_T4_empty_object");
        // 空のJSONを与えた場合、現在のbootコードは currentUser.id が undefined なので S-01 へ
        boolean selectShown = page.locator("#screen-select-user").isVisible();
        assertThat(selectShown)
                .as("不完全なJSONなら利用者選択画面に戻るべき").isTrue();
    }
}
