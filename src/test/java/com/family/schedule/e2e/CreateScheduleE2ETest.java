package com.family.schedule.e2e;

import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * create_schedule.feature 相当 — 予定登録フォームの動作検証
 */
class CreateScheduleE2ETest extends BaseE2ETest {

    @Test
    void 必要最小限の入力で登録できる() {
        setCurrentUser("お母さん", 2);
        page.navigate(baseUrl() + "/");

        page.locator("#btn-add").click();
        // 長女(3)を選択
        page.locator("#who-btns .who-btn").filter(
                new com.microsoft.playwright.Locator.FilterOptions().setHasText("長女")).click();
        page.locator("#date-input").fill(tomorrow());
        page.locator("#content-input").fill("塾");
        page.locator("#btn-save").click();

        assertThat(page.locator("#modal")).isHidden();
        assertThat(cell(3, tomorrow()).locator(".schedule-item-text:has-text('塾')")).isVisible();
    }

    @Test
    void フォームを開いたときの初期値が正しい() {
        setCurrentUser("お母さん", 2);
        page.navigate(baseUrl() + "/");

        page.locator("#btn-add").click();

        // 誰が: 現在の利用者(お母さん)が選択済み
        assertThat(page.locator("#who-btns .who-btn.selected")).hasText("お母さん");
        // 日付: 今日
        assertThat(page.locator("#date-input")).hasValue(today());
        // 内容: 空
        assertThat(page.locator("#content-input")).hasValue("");
    }

    @Test
    void 誰がはメンバーボタンのワンタップ切替() {
        setCurrentUser("お母さん", 2);
        page.navigate(baseUrl() + "/");

        page.locator("#btn-add").click();

        // 全メンバーのボタンが表示される
        for (String name : java.util.List.of("お父さん", "お母さん", "長女", "次女", "長男")) {
            assertThat(page.locator("#who-btns .who-btn").filter(
                    new com.microsoft.playwright.Locator.FilterOptions().setHasText(name))).isVisible();
        }

        // 長男をタップすると長男が選択状態になる
        page.locator("#who-btns .who-btn").filter(
                new com.microsoft.playwright.Locator.FilterOptions().setHasText("長男")).click();
        assertThat(page.locator("#who-btns .who-btn.selected")).hasText("長男");
    }

    @Test
    void 登録は3ステップ以内で完了する_FR21() {
        setCurrentUser("お母さん", 2);
        page.navigate(baseUrl() + "/");

        // ステップ1: +追加タップ
        page.locator("#btn-add").click();
        // ステップ2: 誰が・内容を入力（日付は初期値=今日のまま）
        page.locator("#who-btns .who-btn").filter(
                new com.microsoft.playwright.Locator.FilterOptions().setHasText("長男")).click();
        page.locator("#content-input").fill("サッカー");
        // ステップ3: 保存
        page.locator("#btn-save").click();

        assertThat(cell(5, today()).locator(".schedule-item-text:has-text('サッカー')")).isVisible();
    }

    @Test
    void 内容が空のときはエラーが表示される() {
        setCurrentUser("お母さん", 2);
        page.navigate(baseUrl() + "/");

        page.locator("#btn-add").click();
        page.locator("#content-input").fill("");
        page.locator("#btn-save").click();

        assertThat(page.locator("#error-msg")).hasText("内容を入力してください");
        assertThat(page.locator("#modal")).isVisible();
    }

    @Test
    void 内容が100文字を超える場合はエラーが表示される() {
        setCurrentUser("お母さん", 2);
        page.navigate(baseUrl() + "/");

        page.locator("#btn-add").click();
        // 101文字
        page.locator("#content-input").fill("あ".repeat(101));
        page.locator("#btn-save").click();

        assertThat(page.locator("#error-msg")).hasText("内容は100文字以内で入力してください");
        assertThat(page.locator("#modal")).isVisible();
    }

    @Test
    void 保存に成功するとトーストが表示される() {
        setCurrentUser("お母さん", 2);
        page.navigate(baseUrl() + "/");

        page.locator("#btn-add").click();
        page.locator("#who-btns .who-btn").filter(
                new com.microsoft.playwright.Locator.FilterOptions().setHasText("長女")).click();
        page.locator("#date-input").fill(tomorrow());
        page.locator("#content-input").fill("塾");
        page.locator("#btn-save").click();

        assertThat(page.locator("#toast")).hasText("保存しました");
    }

    @Test
    void キャンセルすると保存されない() {
        setCurrentUser("お母さん", 2);
        page.navigate(baseUrl() + "/");

        page.locator("#btn-add").click();
        page.locator("#who-btns .who-btn").filter(
                new com.microsoft.playwright.Locator.FilterOptions().setHasText("次女")).click();
        page.locator("#content-input").fill("ピアノ");
        page.locator("#btn-cancel").click();

        assertThat(page.locator("#modal")).isHidden();
        assertThat(page.locator(".schedule-item-text:has-text('ピアノ')")).hasCount(0);
    }
}
