package com.family.schedule.e2e;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * switch_user.feature 相当 — 利用者切り替え（FR-25）の動作検証
 */
class SwitchUserE2ETest extends BaseE2ETest {

    @BeforeEach
    void navigateToSchedule() {
        setCurrentUser("お母さん", 2);
        page.navigate(baseUrl() + "/");
        assertThat(page.locator("#screen-schedule")).isVisible();
    }

    @Test
    void ヘッダのユーザーボタンをタップすると利用者選択画面に戻る() {
        page.locator("#btn-switch-user").click();

        assertThat(page.locator("#screen-select-user")).isVisible();
        assertThat(page.locator("#screen-schedule")).isHidden();

        // LocalStorage から利用者情報が削除されていることを確認
        Object stored = page.evaluate("localStorage.getItem('familySchedule.currentUser')");
        org.junit.jupiter.api.Assertions.assertNull(stored);
    }

    @Test
    void 切り替え後に別の利用者としてスケジュール画面に入れる() {
        page.locator("#btn-switch-user").click();
        assertThat(page.locator("#screen-select-user")).isVisible();

        page.locator(".member-btn").filter(
                new com.microsoft.playwright.Locator.FilterOptions().setHasText("次女")).click();

        assertThat(page.locator("#screen-schedule")).isVisible();
        assertThat(page.locator("#current-user-name")).hasText("次女");
    }
}
