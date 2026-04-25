package com.family.schedule.e2e;

import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * user_memory.feature 相当 — 利用者記憶・起動時挙動の検証（FR-20）
 */
class UserMemoryE2ETest extends BaseE2ETest {

    @Test
    void 初回起動時は利用者選択画面が出る() {
        // LocalStorage を設定せずに起動
        page.navigate(baseUrl() + "/");
        assertThat(page.locator("#screen-select-user")).isVisible();
        assertThat(page.locator("#screen-schedule")).isHidden();
    }

    @Test
    void 利用者を選択すると端末に記憶される() {
        page.navigate(baseUrl() + "/");
        page.locator(".member-btn").filter(
                new com.microsoft.playwright.Locator.FilterOptions().setHasText("お母さん")).click();

        assertThat(page.locator("#screen-schedule")).isVisible();
        assertThat(page.locator("#current-user-name")).hasText("お母さん");

        // LocalStorage に保存されていることを確認
        Object stored = page.evaluate("localStorage.getItem('familySchedule.currentUser')");
        org.junit.jupiter.api.Assertions.assertNotNull(stored);
        org.junit.jupiter.api.Assertions.assertTrue(stored.toString().contains("お母さん"));
    }

    @Test
    void 2回目以降はスケジュール画面から始まる() {
        setCurrentUser("お母さん", 2);
        page.navigate(baseUrl() + "/");

        assertThat(page.locator("#screen-schedule")).isVisible();
        assertThat(page.locator("#screen-select-user")).isHidden();
        assertThat(page.locator("#current-user-name")).hasText("お母さん");
    }
}
