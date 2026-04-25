package com.family.schedule.e2e;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * TC-SC シナリオテスト相当の E2E 実装。
 * 実行にはローカルに Playwright ブラウザが必要（ない環境では自動スキップ）。
 */
class ScenarioE2ETest extends BaseE2ETest {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    @Test
    void TC_SC_01_お母さんの朝() {
        // 1. 初回起動時は利用者選択画面
        page.navigate(baseUrl() + "/");
        assertThat(page.locator("#screen-select-user")).isVisible();
        snap("01_初回_S01_利用者選択");

        // 2. お母さん をタップ
        page.locator(".member-btn", new com.microsoft.playwright.Page.LocatorOptions()).getByText("お母さん").click();
        assertThat(page.locator("#screen-schedule")).isVisible();
        assertThat(page.locator("#current-user-name")).hasText("お母さん");
        snap("02_S02_スケジュール画面");

        // 4. ブラウザを閉じて再度開く = 同じコンテキストで再アクセス
        page.navigate(baseUrl() + "/");
        assertThat(page.locator("#screen-schedule")).isVisible();
        assertThat(page.locator("#screen-select-user")).isHidden();
        snap("03_2回目起動でS02直行");

        // 5. 登録が3ステップ以内
        page.locator("#btn-add").click();             // step 1
        snap("04_S03_予定追加モーダル");
        page.locator(".who-btn").getByText("長男").click();
        page.locator("#content-input").fill("学童お迎え");  // step 2
        snap("05_S03_入力済み");
        page.locator("#btn-save").click();              // step 3
        assertThat(page.locator("#toast")).hasText("保存しました");
        snap("06_保存トースト");
        PlaywrightAssertions.assertThat(page.locator(".schedule-item:has-text('学童お迎え')")).isVisible();
    }

    @Test
    void TC_SC_02_長女の夜の予定() {
        setCurrentUser("長女", 3);
        page.navigate(baseUrl() + "/");
        assertThat(page.locator("#screen-schedule")).isVisible();

        // 登録
        page.locator("#btn-add").click();
        page.locator("#content-input").fill("塾（数学）");
        page.locator("#btn-save").click();
        assertThat(page.locator("#toast")).hasText("保存しました");
    }

    @Test
    void TC_SC_04_次女の誤操作防止() {
        setCurrentUser("次女", 4);
        // 予定を1件入れておく（APIで）
        context.request().post(baseUrl() + "/api/schedules",
                com.microsoft.playwright.options.RequestOptions.create().setData(java.util.Map.of(
                        "memberId", 4, "date", LocalDate.now().format(ISO), "content", "閲覧のみ")));

        page.navigate(baseUrl() + "/");
        Locator item = page.locator(".schedule-item").first();
        assertThat(item).isVisible();
        // 長押し（500ms）しても編集フォームは開かない（FR-23）
        item.hover();
        page.mouse().down();
        page.waitForTimeout(500);
        page.mouse().up();
        assertThat(page.locator(".schedule-item-input")).hasCount(0);
    }

    @Test
    void TC_SC_05_✕ボタン削除とUNDO() {
        setCurrentUser("お母さん", 2);
        context.request().post(baseUrl() + "/api/schedules",
                com.microsoft.playwright.options.RequestOptions.create().setData(java.util.Map.of(
                        "memberId", 5, "date", LocalDate.now().format(ISO), "content", "サッカー教室")));

        page.navigate(baseUrl() + "/");
        snap("01_初期表示");

        // ✕ボタンをクリック
        page.locator(".schedule-item:has-text('サッカー教室')")
                .locator(".schedule-item-delete").click();
        page.waitForTimeout(500);
        snap("02_削除後トースト");

        assertThat(page.locator("#toast")).containsText("削除しました");
        assertThat(page.locator(".schedule-item-text:has-text('サッカー教室')")).hasCount(0);

        // 元に戻す
        page.locator("#toast").locator("button:has-text('元に戻す')").click();
        page.waitForTimeout(1200);
        snap("03_復元後");

        assertThat(page.locator(".schedule-item-text:has-text('サッカー教室')")).isVisible();
        assertThat(page.locator("#toast")).hasText("元に戻しました");
    }

}
