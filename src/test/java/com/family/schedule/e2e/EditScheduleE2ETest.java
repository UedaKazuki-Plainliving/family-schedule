package com.family.schedule.e2e;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * edit_schedule.feature 相当 — インライン編集の動作検証
 */
class EditScheduleE2ETest extends BaseE2ETest {

    @BeforeEach
    void insertFixture() {
        insertSchedule(3, today(), "部活"); // 長女(3)
        setCurrentUser("お父さん", 1);
    }

    @Test
    void 予定をタップするとインライン入力欄が開く() {
        page.navigate(baseUrl() + "/");

        cell(3, today()).locator(".schedule-item").first().click();

        assertThat(page.locator(".schedule-item-input")).isVisible();
        assertThat(page.locator(".schedule-item-input")).hasValue("部活");
    }

    @Test
    void 内容を書き換えてEnterで保存する() {
        page.navigate(baseUrl() + "/");

        cell(3, today()).locator(".schedule-item").first().click();
        page.locator(".schedule-item-input").fill("部活（19時まで）");
        page.locator(".schedule-item-input").press("Enter");
        page.waitForTimeout(600);

        assertThat(page.locator(".schedule-item-input")).hasCount(0);
        assertThat(cell(3, today()).locator(".schedule-item-text:has-text('部活（19時まで）')")).isVisible();
        assertThat(page.locator("#toast")).hasText("更新しました");
    }

    @Test
    void Escapeでキャンセルすると元の内容に戻る() {
        page.navigate(baseUrl() + "/");

        cell(3, today()).locator(".schedule-item").first().click();
        page.locator(".schedule-item-input").fill("変更途中");
        page.locator(".schedule-item-input").press("Escape");
        page.waitForTimeout(200);

        assertThat(page.locator(".schedule-item-input")).hasCount(0);
        assertThat(cell(3, today()).locator(".schedule-item-text:has-text('部活')")).isVisible();
    }

    @Test
    void 内容を空にしてEnterを押すと変更がキャンセルされる() {
        page.navigate(baseUrl() + "/");

        cell(3, today()).locator(".schedule-item").first().click();
        page.locator(".schedule-item-input").fill("");
        page.locator(".schedule-item-input").press("Enter");
        page.waitForTimeout(300);

        assertThat(page.locator(".schedule-item-input")).hasCount(0);
        assertThat(cell(3, today()).locator(".schedule-item-text:has-text('部活')")).isVisible();
    }

    @Test
    void 長押しではインライン編集モードにならない_FR23() {
        page.navigate(baseUrl() + "/");

        var item = cell(3, today()).locator(".schedule-item").first();
        item.hover();
        page.mouse().down();
        page.waitForTimeout(500);
        page.mouse().up();

        assertThat(page.locator(".schedule-item-input")).hasCount(0);
    }
}
