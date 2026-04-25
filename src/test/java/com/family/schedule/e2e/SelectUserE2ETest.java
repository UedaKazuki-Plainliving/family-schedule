package com.family.schedule.e2e;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * select_user.feature 相当 — 利用者選択画面の動作検証
 */
class SelectUserE2ETest extends BaseE2ETest {

    @Test
    void 登録メンバーの名前ボタンが全員分表示される() {
        page.navigate(baseUrl() + "/");
        assertThat(page.locator("#screen-select-user")).isVisible();
        for (String name : List.of("お父さん", "お母さん", "長女", "次女", "長男")) {
            assertThat(page.locator(".member-btn").filter(
                    new com.microsoft.playwright.Locator.FilterOptions().setHasText(name))).isVisible();
        }
    }

    @ParameterizedTest(name = "{0} を選ぶとスケジュール画面に遷移する")
    @CsvSource({
        "お父さん, 1",
        "お母さん, 2",
        "長女,     3",
        "次女,     4",
        "長男,     5"
    })
    void 名前を選ぶとスケジュール画面に遷移する(String name, int id) {
        page.navigate(baseUrl() + "/");
        page.locator(".member-btn").filter(
                new com.microsoft.playwright.Locator.FilterOptions().setHasText(name)).click();
        assertThat(page.locator("#screen-schedule")).isVisible();
        assertThat(page.locator("#current-user-name")).hasText(name);
    }
}
