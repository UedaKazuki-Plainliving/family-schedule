package com.family.schedule.e2e;

import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** TC-ST 画面遷移テスト相当 */
class ScreenTransitionE2ETest extends BaseE2ETest {

    @Test
    void T01_初回起動はS01() {
        page.navigate(baseUrl() + "/");
        assertThat(page.locator("#screen-select-user")).isVisible();
    }

    @Test
    void T02_LocalStorageありはS02() {
        context.addInitScript(
                "localStorage.setItem('familySchedule.currentUser','{\"id\":2,\"name\":\"お母さん\",\"displayOrder\":2}');");
        page.navigate(baseUrl() + "/");
        assertThat(page.locator("#screen-schedule")).isVisible();
        assertThat(page.locator("#screen-select-user")).isHidden();
    }

    @Test
    void T03_名前ボタンタップでS02() {
        page.navigate(baseUrl() + "/");
        page.locator(".member-btn:has-text('お父さん')").click();
        assertThat(page.locator("#screen-schedule")).isVisible();
    }

    @Test
    void T08_追加ボタンでS03_モーダル() {
        context.addInitScript(
                "localStorage.setItem('familySchedule.currentUser','{\"id\":2,\"name\":\"お母さん\",\"displayOrder\":2}');");
        page.navigate(baseUrl() + "/");
        page.locator("#btn-add").click();
        assertThat(page.locator("#modal")).isVisible();
        assertThat(page.locator("#form-title")).hasText("予定を追加");
    }

    @Test
    void T17_キャンセルでS02に戻る() {
        context.addInitScript(
                "localStorage.setItem('familySchedule.currentUser','{\"id\":2,\"name\":\"お母さん\",\"displayOrder\":2}');");
        page.navigate(baseUrl() + "/");
        page.locator("#btn-add").click();
        page.locator("#btn-cancel").click();
        assertThat(page.locator("#modal")).isHidden();
        assertThat(page.locator("#screen-schedule")).isVisible();
    }

    @Test
    void T14_今日に戻るボタン() {
        context.addInitScript(
                "localStorage.setItem('familySchedule.currentUser','{\"id\":2,\"name\":\"お母さん\",\"displayOrder\":2}');");
        page.navigate(baseUrl() + "/");
        String before = page.locator("#date-heading-left").innerText();
        page.locator("#btn-today").click();
        // 今日に戻っても同じラベル
        assertThat(page.locator("#date-heading-left")).containsText("今日");
    }
}
