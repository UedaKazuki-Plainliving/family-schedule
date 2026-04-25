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
        page.locator(".who-btn").getByText("いちろう").click();
        page.locator("#content-input").fill("学童お迎え");  // step 2
        snap("05_S03_入力済み");
        page.locator("#btn-save").click();              // step 3
        assertThat(page.locator("#toast")).hasText("保存しました");
        snap("06_保存トースト");
        PlaywrightAssertions.assertThat(page.locator(".schedule-item:has-text('学童お迎え')")).isVisible();
    }

    @Test
    void TC_SC_02_そよの夜の予定() {
        setCurrentUser("そよ", 3);
        page.navigate(baseUrl() + "/");
        assertThat(page.locator("#screen-schedule")).isVisible();

        // 登録
        page.locator("#btn-add").click();
        page.locator("#content-input").fill("塾（数学）");
        page.locator("#btn-save").click();
        assertThat(page.locator("#toast")).hasText("保存しました");
    }

    @Test
    void TC_SC_04_ゆうりの誤操作防止() {
        setCurrentUser("ゆうり", 4);
        // 予定を1件入れておく（APIで）
        context.request().post(baseUrl() + "/api/schedules",
                com.microsoft.playwright.options.RequestOptions.create().setData(java.util.Map.of(
                        "memberId", 4, "date", LocalDate.now().format(ISO), "content", "閲覧のみ")));

        page.navigate(baseUrl() + "/");
        Locator item = page.locator(".schedule-item").first();
        assertThat(item).isVisible();
        // 長押し（500ms）しても編集フォームは開かない
        item.hover();
        page.mouse().down();
        page.waitForTimeout(500);
        page.mouse().up();
        assertThat(page.locator("#modal")).isHidden();
    }

    @Test
    void TC_SC_05_削除と確認ダイアログ() {
        setCurrentUser("お母さん", 2);
        context.request().post(baseUrl() + "/api/schedules",
                com.microsoft.playwright.options.RequestOptions.create().setData(java.util.Map.of(
                        "memberId", 5, "date", LocalDate.now().format(ISO), "content", "サッカー教室")));

        page.navigate(baseUrl() + "/");
        snap("01_初期表示");
        page.locator(".schedule-item:has-text('サッカー教室')").click();
        snap("02_編集フォーム");
        page.locator("#btn-delete").click();
        assertThat(page.locator("#confirm-text")).hasText("『サッカー教室』を削除しますか？");
        snap("03_削除確認ダイアログ");
        page.locator("#btn-no").click();
        assertThat(page.locator("#confirm")).isHidden();

        page.locator("#btn-delete").click();
        page.locator("#btn-yes").click();
        assertThat(page.locator("#toast")).hasText("削除しました");
        snap("04_削除完了");
    }

    private void setCurrentUser(String name, int id) {
        // LocalStorage に利用者をセットしてからナビゲート
        context.addInitScript(
                "localStorage.setItem('familySchedule.currentUser', '" +
                        "{\"id\":" + id + ",\"name\":\"" + name + "\",\"displayOrder\":" + id + "}');");
    }
}
