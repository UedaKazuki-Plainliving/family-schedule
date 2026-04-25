package com.family.schedule.e2e;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * 本番 EC2 環境（:8082）への Playwright E2E テスト。
 * 実行: mvn test-compile failsafe:integration-test -Dit.test=ProductionE2ETest
 *       -Dbase.url=http://54.162.107.130:8082 -Dheadless=false
 */
class ProductionE2ETest {

    static final String BASE_URL = System.getProperty("base.url", "http://54.162.107.130:8082");
    static final String CHROME = "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe";
    static final Path EVIDENCE = Paths.get("target", "e2e-evidence");

    static Playwright playwright;
    static Browser browser;
    BrowserContext ctx;
    Page page;

    @BeforeAll
    static void init() throws Exception {
        Files.createDirectories(EVIDENCE);
        playwright = Playwright.create(new Playwright.CreateOptions()
                .setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
        boolean headless = Boolean.parseBoolean(System.getProperty("headless", "false"));
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(headless)
                .setSlowMo(800)
                .setExecutablePath(Paths.get(CHROME)));
    }

    @AfterAll
    static void close() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    @BeforeEach
    void setUp() {
        ctx = browser.newContext(new Browser.NewContextOptions().setViewportSize(390, 844));
        page = ctx.newPage();
        page.setDefaultTimeout(10000);
        // LocalStorage をクリアして必ず S-01（ユーザー選択）から始める
        page.navigate(BASE_URL + "/");
        page.evaluate("localStorage.clear()");
        page.reload();
    }

    @AfterEach
    void tearDown() {
        ctx.close();
    }

    void snap(String name) {
        page.screenshot(new Page.ScreenshotOptions()
                .setPath(EVIDENCE.resolve(name + ".png"))
                .setFullPage(true));
    }

    /** S-01 → S-02 へ遷移するヘルパー（最初のメンバーを選択）。*/
    void selectFirstMember() {
        assertThat(page.locator("#screen-select-user")).isVisible();
        page.locator(".member-btn").first().click();
        assertThat(page.locator("#screen-schedule")).isVisible();
    }

    // ----------------------------------------------------------------

    @Test
    void T01_ユーザー選択画面が表示される() {
        assertThat(page.locator("#screen-select-user")).isVisible();
        assertThat(page.locator(".member-btn").first()).isVisible();
        snap("T01_select_user");
    }

    @Test
    void T02_メンバーを選んでスケジュール画面に遷移する() {
        page.locator(".member-btn").first().click();
        assertThat(page.locator("#screen-schedule")).isVisible();
        assertThat(page.locator("#current-user-name")).not().isEmpty();
        snap("T02_schedule");
    }

    @Test
    void T03_予定を追加できる() {
        selectFirstMember();

        page.locator("#btn-add").click();
        assertThat(page.locator("#modal")).isVisible();
        assertThat(page.locator("#form-title")).hasText("予定を追加");

        page.fill("#content-input", "E2Eテスト用の予定");
        snap("T03_before_save");
        page.click("#btn-save");

        assertThat(page.locator("#modal")).isHidden();
        assertThat(page.locator(".schedule-item")
                .filter(new Locator.FilterOptions().setHasText("E2Eテスト用の予定")))
                .isVisible();
        snap("T03_after_save");
    }

    @Test
    void T04_内容空欄でエラーが表示される() {
        selectFirstMember();

        page.locator("#btn-add").click();
        assertThat(page.locator("#modal")).isVisible();

        page.fill("#content-input", "   "); // 空白のみ
        page.click("#btn-save");

        assertThat(page.locator("#error-msg")).isVisible();
        snap("T04_error");
    }

    @Test
    void T05_予定を編集できる() {
        selectFirstMember();

        // 予定を追加
        page.locator("#btn-add").click();
        page.fill("#content-input", "編集テスト用の予定");
        page.click("#btn-save");
        assertThat(page.locator("#modal")).isHidden();

        // 追加した予定をタップして編集モーダルを開く
        page.locator(".schedule-item")
                .filter(new Locator.FilterOptions().setHasText("編集テスト用の予定"))
                .click();
        assertThat(page.locator("#modal")).isVisible();
        assertThat(page.locator("#form-title")).hasText("予定を編集");

        page.fill("#content-input", "編集後の予定");
        snap("T05_before_update");
        page.click("#btn-save");

        assertThat(page.locator("#modal")).isHidden();
        assertThat(page.locator(".schedule-item")
                .filter(new Locator.FilterOptions().setHasText("編集後の予定")))
                .isVisible();
        snap("T05_after_update");
    }

    @Test
    void T06_予定を削除できる() {
        selectFirstMember();

        // 予定を追加
        page.locator("#btn-add").click();
        page.fill("#content-input", "削除テスト用の予定");
        page.click("#btn-save");
        assertThat(page.locator("#modal")).isHidden();

        // 予定をタップ → 削除ボタン → 確認ダイアログ
        page.locator(".schedule-item")
                .filter(new Locator.FilterOptions().setHasText("削除テスト用の予定"))
                .click();
        assertThat(page.locator("#modal")).isVisible();
        page.locator("#btn-delete").click();

        assertThat(page.locator("#confirm")).isVisible();
        snap("T06_confirm_delete");
        page.locator("#btn-yes").click();

        assertThat(page.locator("#confirm")).isHidden();
        assertThat(page.locator("#toast")).isVisible();
        snap("T06_after_delete");
    }

    @Test
    void T07_今日に戻るボタンが動く() {
        selectFirstMember();
        assertThat(page.locator("#date-heading-left")).containsText("今日");
        page.locator("#btn-today").click();
        assertThat(page.locator("#date-heading-left")).containsText("今日");
        snap("T07_today");
    }
}
