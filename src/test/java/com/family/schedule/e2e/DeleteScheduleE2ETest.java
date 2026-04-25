package com.family.schedule.e2e;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * delete_schedule.feature 相当 — ✕ボタン削除と UNDO の動作検証
 */
class DeleteScheduleE2ETest extends BaseE2ETest {

    @BeforeEach
    void insertFixture() {
        insertSchedule(5, today(), "サッカー教室"); // 長男(5)
        setCurrentUser("お母さん", 2);
    }

    @Test
    void ✕ボタンで予定を即削除する() {
        page.navigate(baseUrl() + "/");

        // ✕ボタンをクリック
        cell(5, today()).locator(".schedule-item").first()
                .locator(".schedule-item-delete").click();
        page.waitForTimeout(500);

        assertThat(cell(5, today()).locator(".schedule-none")).isVisible();
        assertThat(page.locator("#toast")).containsText("削除しました");
    }

    @Test
    void 削除後に元に戻すで復元できる_FR27_UNDO() {
        page.navigate(baseUrl() + "/");

        // 削除
        cell(5, today()).locator(".schedule-item").first()
                .locator(".schedule-item-delete").click();
        page.waitForTimeout(500);

        assertThat(page.locator("#toast")).containsText("削除しました");

        // 「元に戻す」をクリック
        page.locator("#toast").locator("button:has-text('元に戻す')").click();
        page.waitForTimeout(1200);

        assertThat(cell(5, today()).locator(".schedule-item-text:has-text('サッカー教室')")).isVisible();
        assertThat(page.locator("#toast")).hasText("元に戻しました");
    }
}
