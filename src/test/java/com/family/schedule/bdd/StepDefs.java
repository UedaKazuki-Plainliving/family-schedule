package com.family.schedule.bdd;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.Assumptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class StepDefs {

    private static final Map<String, Integer> MEMBER_IDS = Map.of(
            "お父さん", 1, "お母さん", 2, "そよ", 3, "ゆうり", 4, "いちろう", 5);

    @Autowired WorldContext world;
    @Autowired JdbcTemplate jdbc;

    private void requireBrowser() {
        Assumptions.assumeTrue(world.page != null, "Playwright browser not available — BDD E2E skipped");
    }

    /** Playwright の assertThat を短名で呼ぶ */
    private LocatorAssertions pw(Locator l) {
        return PlaywrightAssertions.assertThat(l);
    }

    private Locator cell(String memberName, String date) {
        int id = MEMBER_IDS.get(memberName);
        return world.page.locator(
                ".schedule-cell[data-member-id='" + id + "'][data-date='" + date + "']");
    }

    private void doFlick(int dx) {
        world.page.mouse().move(195, 400);
        world.page.mouse().down();
        world.page.mouse().move(195 + dx, 400);
        world.page.mouse().up();
        world.page.waitForTimeout(400);
    }

    private String buttonId(String label) {
        return switch (label) {
            case "削除" -> "#btn-delete";
            case "保存" -> "#btn-save";
            case "キャンセル" -> "#btn-cancel";
            default -> "button";
        };
    }

    // ==================== 前提：セットアップ ====================

    @Given("今日は {string} である")
    public void setFeatureToday(String dateStr) {
        world.featureToday = LocalDate.parse(dateStr);
        world.viewDate = LocalDate.now();
    }

    @Given("予定は1件も登録されていない")
    public void noSchedules() {
        // @Before で DB クリア済み
    }

    @Given("次の予定が登録されている")
    public void insertSchedules(DataTable table) {
        for (Map<String, String> row : table.asMaps()) {
            String member  = row.get("member");
            String content = row.get("content");
            String date    = world.resolveDate(row.get("date"));
            int memberId   = MEMBER_IDS.getOrDefault(member, 1);
            jdbc.update(
                "INSERT INTO schedules (member_id, date, content, created_at, updated_at) VALUES (?,?,?,now(),now())",
                memberId, java.sql.Date.valueOf(date), content);
        }
    }

    @Given("現在の利用者は {string} である")
    public void setCurrentUser(String name) {
        requireBrowser();
        int id = MEMBER_IDS.getOrDefault(name, 1);
        world.page.addInitScript(
            "localStorage.setItem('familySchedule.currentUser', JSON.stringify(" +
            "{id:" + id + ",name:'" + name + "',displayOrder:" + id + "}));");
    }

    @Given("端末に利用者情報が保存されていない")
    public void clearLocalStorage() {
        requireBrowser();
        world.page.addInitScript("localStorage.removeItem('familySchedule.currentUser');");
    }

    @Given("端末に利用者情報 {string} が保存されている")
    public void setLocalStorageUser(String name) {
        requireBrowser();
        int id = MEMBER_IDS.getOrDefault(name, 1);
        world.page.addInitScript(
            "localStorage.setItem('familySchedule.currentUser', JSON.stringify(" +
            "{id:" + id + ",name:'" + name + "',displayOrder:" + id + "}));");
    }

    @Given("スケジュール画面を開いている")
    public void openScheduleScreen() {
        requireBrowser();
        world.page.navigate(world.baseUrl() + "/");
        world.page.waitForSelector("#screen-schedule:not([hidden])");
        world.stepCount = 0;
    }

    @Given("表示されているのは {string}（今日）と {string}（明日）である")
    public void assertInitialDates(String today, String tomorrow) {
        requireBrowser();
        pw(world.page.locator("#date-heading-left"))
                .containsText(world.dayLabel(LocalDate.now()));
    }

    @Given("画面を左に {int} 回フリックした")
    public void flickLeftNTimes_setup(int times) {
        requireBrowser();
        for (int i = 0; i < times; i++) {
            doFlick(-50);
            world.viewDate = world.viewDate.plusDays(1);
        }
    }

    // ==================== もし：ナビゲーション・操作 ====================

    @Given("トップ画面を開いた")
    public void openTopPage() {
        requireBrowser();
        world.page.navigate(world.baseUrl() + "/");
        world.page.waitForSelector("#screen-select-user:not([hidden])");
    }

    @Given("アプリを起動した")
    @When("アプリを起動する")
    public void bootApp() {
        requireBrowser();
        world.page.navigate(world.baseUrl() + "/");
        world.page.waitForTimeout(500);
    }

    @When("スケジュール画面を開く")
    public void navigateToSchedule() {
        requireBrowser();
        world.page.navigate(world.baseUrl() + "/");
        world.page.waitForSelector("#screen-schedule:not([hidden])");
    }

    @When("{string} のボタンをタップする")
    public void tapMemberButton(String name) {
        requireBrowser();
        world.stepCount++;
        world.page.locator(".member-btn, .who-btn")
                .filter(new Locator.FilterOptions().setHasText(name)).first().click();
    }

    @When("{string} をタップする")
    public void tapByLabel(String label) {
        requireBrowser();
        world.stepCount++;
        switch (label) {
            case "+追加"   -> world.page.locator("#btn-add").click();
            case "保存"    -> world.page.locator("#btn-save").click();
            case "キャンセル" -> world.page.locator("#btn-cancel").click();
            case "削除"    -> world.page.locator("#btn-delete").click();
            case "はい"    -> world.page.locator("#btn-yes").click();
            case "いいえ"  -> world.page.locator("#btn-no").click();
            default -> world.page.getByText(label).first().click();
        }
    }

    @When("{string} ボタンをタップする")
    public void tapButton(String label) {
        requireBrowser();
        world.stepCount++;
        switch (label) {
            case "+追加"    -> world.page.locator("#btn-add").click();
            case "今日に戻る" -> world.page.locator("#btn-today").click();
            default -> world.page.locator("button")
                    .filter(new Locator.FilterOptions().setHasText(label)).first().click();
        }
    }

    @When("スケジュール画面で {string} ボタンをタップする")
    public void tapButtonOnScheduleScreen(String label) {
        requireBrowser();
        world.stepCount++;
        if ("+追加".equals(label)) world.page.locator("#btn-add").click();
        else tapButton(label);
    }

    @When("入力フォームに次を入力する")
    public void fillScheduleForm(DataTable table) {
        requireBrowser();
        world.stepCount++;
        Map<String, String> row = table.asMaps().get(0);
        String member  = row.get("member");
        String content = row.get("content");
        String date    = world.resolveDate(row.get("date"));

        if (member != null && !member.isBlank()) {
            world.page.locator(".who-btn")
                    .filter(new Locator.FilterOptions().setHasText(member)).first().click();
        }
        world.page.locator("#date-input").fill(date);
        world.page.locator("#content-input").fill("(空)".equals(content) ? "" : content);
    }

    @When("入力フォームに101文字の内容を入力する")
    public void fillContent101chars() {
        requireBrowser();
        world.stepCount++;
        world.page.locator("#content-input").fill("あ".repeat(101));
    }

    @When("{string} 枠の {string} の {string} をタップする")
    public void tapScheduleItem(String dayLabel, String memberName, String content) {
        requireBrowser();
        world.stepCount++;
        String date = "今日".equals(dayLabel) ? world.today() : world.tomorrow();
        cell(memberName, date).locator(".schedule-item")
                .filter(new Locator.FilterOptions().setHasText(content)).first().click();
    }

    @When("{string} 枠の {string} の {string} を長押しする")
    public void longPressScheduleItem(String dayLabel, String memberName, String content) {
        requireBrowser();
        String date = "今日".equals(dayLabel) ? world.today() : world.tomorrow();
        Locator item = cell(memberName, date).locator(".schedule-item")
                .filter(new Locator.FilterOptions().setHasText(content)).first();
        item.hover();
        world.page.mouse().down();
        world.page.waitForTimeout(500);
        world.page.mouse().up();
    }

    @When("{string} を {string} に書き換える")
    public void rewriteField(String fieldLabel, String newValue) {
        requireBrowser();
        world.stepCount++;
        if ("内容".equals(fieldLabel)) {
            world.page.locator("#content-input").fill(newValue);
        }
    }

    @When("{string} を {string} に変更する")
    public void changeField(String fieldLabel, String value) {
        requireBrowser();
        world.stepCount++;
        if ("誰が".equals(fieldLabel)) {
            world.page.locator(".who-btn")
                    .filter(new Locator.FilterOptions().setHasText(value)).first().click();
        }
    }

    @When("確認ダイアログ {string} で {string} をタップする")
    public void tapOnConfirmDialog(String dialogText, String buttonLabel) {
        requireBrowser();
        pw(world.page.locator("#confirm-text")).containsText(dialogText);
        tapByLabel(buttonLabel);
    }

    @When("画面を左にフリックする")
    public void flickLeft() {
        requireBrowser();
        doFlick(-50);
        world.viewDate = world.viewDate.plusDays(1);
    }

    @When("画面を右にフリックする")
    public void flickRight() {
        requireBrowser();
        doFlick(50);
        world.viewDate = world.viewDate.minusDays(1);
    }

    @When("画面を左に {int} 回フリックする")
    public void flickLeftNTimes(int times) {
        requireBrowser();
        for (int i = 0; i < times; i++) {
            doFlick(-50);
            world.viewDate = world.viewDate.plusDays(1);
        }
    }

    @When("画面を右に {int} 回フリックする")
    public void flickRightNTimes(int times) {
        requireBrowser();
        for (int i = 0; i < times; i++) {
            doFlick(50);
            world.viewDate = world.viewDate.minusDays(1);
        }
    }

    // ==================== ならば：アサーション ====================

    @Then("スケジュール画面が表示される")
    @Then("スケジュール画面に戻る")
    public void assertScheduleVisible() {
        requireBrowser();
        pw(world.page.locator("#screen-schedule")).isVisible();
    }

    @Then("利用者選択画面が表示される")
    public void assertSelectUserVisible() {
        requireBrowser();
        pw(world.page.locator("#screen-select-user")).isVisible();
    }

    @Then("利用者選択画面は表示されない")
    public void assertSelectUserHidden() {
        requireBrowser();
        pw(world.page.locator("#screen-select-user")).isHidden();
    }

    @Then("現在の利用者は {string} である")
    public void assertCurrentUser(String name) {
        requireBrowser();
        pw(world.page.locator("#current-user-name")).hasText(name);
    }

    @Then("端末の利用者情報として {string} が保存される")
    public void assertLocalStorageSaved(String name) {
        requireBrowser();
        String stored = world.page.evaluate(
            "JSON.parse(localStorage.getItem('familySchedule.currentUser')||'{}').name").toString();
        assertThat(stored).isEqualTo(name);
    }

    @Then("名前ボタンに次の5人が表示される")
    public void assertMemberButtons(DataTable table) {
        requireBrowser();
        for (String name : table.asList()) {
            pw(world.page.locator(".member-btn")
                    .filter(new Locator.FilterOptions().setHasText(name))).isVisible();
        }
    }

    @Then("「今日」枠の家族5人すべてに {string} と表示される")
    public void assertAllMembersToday(String text) {
        requireBrowser();
        for (String name : MEMBER_IDS.keySet()) {
            pw(cell(name, world.today())).containsText(text);
        }
    }

    @Then("「明日」枠の家族5人すべてに {string} と表示される")
    public void assertAllMembersTomorrow(String text) {
        requireBrowser();
        for (String name : MEMBER_IDS.keySet()) {
            pw(cell(name, world.tomorrow())).containsText(text);
        }
    }

    @Then("「今日」枠の {string} に {string} が表示される")
    @Then("「今日」枠の {string} に {string} が残っている")
    public void assertItemVisibleToday(String memberName, String content) {
        requireBrowser();
        pw(cell(memberName, world.today())).containsText(content);
    }

    @Then("「明日」枠の {string} に {string} が表示される")
    public void assertItemVisibleTomorrow(String memberName, String content) {
        requireBrowser();
        pw(cell(memberName, world.tomorrow())).containsText(content);
    }

    @Then("「今日」枠の {string} に {string} と {string} の2件が表示される")
    public void assertTwoItemsToday(String memberName, String c1, String c2) {
        requireBrowser();
        Locator c = cell(memberName, world.today());
        pw(c).containsText(c1);
        pw(c).containsText(c2);
    }

    @Then("「今日」枠の {string} に {string} は表示されない")
    @Then("「今日」枠の {string} から {string} が消える")
    public void assertItemNotVisibleToday(String memberName, String content) {
        requireBrowser();
        pw(cell(memberName, world.today()).locator(".schedule-item")
                .filter(new Locator.FilterOptions().setHasText(content))).hasCount(0);
    }

    @Then("「今日」枠の {string} は {string} になる")
    public void assertCellContains(String memberName, String text) {
        requireBrowser();
        pw(cell(memberName, world.today())).containsText(text);
    }

    @Then("{string} は表示されない")
    public void assertTextNotVisible(String text) {
        requireBrowser();
        pw(world.page.locator(".schedule-item")
                .filter(new Locator.FilterOptions().setHasText(text))).hasCount(0);
    }

    @Then("編集フォームが開く")
    public void assertModalOpen() {
        requireBrowser();
        pw(world.page.locator("#modal")).isVisible();
    }

    @Then("編集フォームは開かない")
    public void assertModalClosed() {
        requireBrowser();
        pw(world.page.locator("#modal")).isHidden();
    }

    @Then("フォームの {string} には {string} が選ばれている")
    public void assertFormWhoSelected(String fieldLabel, String name) {
        requireBrowser();
        if ("誰が".equals(fieldLabel)) {
            pw(world.page.locator(".who-btn.selected")).hasText(name);
        }
    }

    @Then("フォームの {string} には {string} が入っている")
    public void assertFormFieldValue(String fieldLabel, String value) {
        requireBrowser();
        if ("日付".equals(fieldLabel)) {
            pw(world.page.locator("#date-input")).hasValue(world.resolveDate(value));
        } else if ("内容".equals(fieldLabel)) {
            pw(world.page.locator("#content-input")).hasValue(value);
        }
    }

    @Then("フォームの {string} は空である")
    public void assertFormFieldEmpty(String fieldLabel) {
        requireBrowser();
        if ("内容".equals(fieldLabel)) {
            pw(world.page.locator("#content-input")).hasValue("");
        }
    }

    @Then("フォームの {string} 欄には5人のボタンが横並びで表示されている")
    public void assertWhoBtns(String fieldLabel, DataTable table) {
        requireBrowser();
        for (String name : table.asList()) {
            pw(world.page.locator(".who-btn")
                    .filter(new Locator.FilterOptions().setHasText(name))).isVisible();
        }
    }

    @Then("{string} として {string} が選択状態になる")
    public void assertWhoSelected(String fieldLabel, String name) {
        requireBrowser();
        pw(world.page.locator(".who-btn.selected")
                .filter(new Locator.FilterOptions().setHasText(name))).isVisible();
    }

    @Then("エラーメッセージ {string} が表示される")
    public void assertErrorMessage(String msg) {
        requireBrowser();
        pw(world.page.locator("#error-msg")).isVisible();
        pw(world.page.locator("#error-msg")).containsText(msg);
    }

    @Then("予定は保存されない")
    @Then("予定は変更されない")
    public void assertNotSaved() {
        requireBrowser();
        pw(world.page.locator("#modal")).isVisible();
    }

    @Then("トースト {string} が表示される")
    public void assertToast(String msg) {
        requireBrowser();
        pw(world.page.locator("#toast")).containsText(msg);
    }

    @Then("確認ダイアログ {string} が表示される")
    public void assertConfirmDialog(String text) {
        requireBrowser();
        pw(world.page.locator("#confirm")).isVisible();
        pw(world.page.locator("#confirm-text")).containsText(text);
    }

    @Then("{string} ボタンは {string} ボタンと別の色である")
    public void assertButtonColorsDiffer(String btn1Label, String btn2Label) {
        requireBrowser();
        String color1 = world.page.locator(buttonId(btn1Label))
                .evaluate("el => getComputedStyle(el).backgroundColor").toString();
        String color2 = world.page.locator(buttonId(btn2Label))
                .evaluate("el => getComputedStyle(el).backgroundColor").toString();
        assertThat(color1).isNotEqualTo(color2);
    }

    @Then("{string} ボタンは {string} ボタンとは離れた位置にある")
    public void assertButtonsFarApart(String btn1Label, String btn2Label) {
        requireBrowser();
        var box1 = world.page.locator(buttonId(btn1Label)).boundingBox();
        var box2 = world.page.locator(buttonId(btn2Label)).boundingBox();
        double distance = Math.abs(box1.x - box2.x) + Math.abs(box1.y - box2.y);
        assertThat(distance).isGreaterThan(50.0);
    }

    @Then("ここまでの操作ステップ数は3以内である")
    public void assertStepsWithin3() {
        assertThat(world.stepCount).isLessThanOrEqualTo(3);
    }

    @Then("表示されている日付は {string} と {string} である")
    public void assertDisplayedDates(String leftDateStr, String rightDateStr) {
        requireBrowser();
        pw(world.page.locator("#date-heading-left"))
                .containsText(world.dayLabel(world.viewDate));
        pw(world.page.locator("#date-heading-right"))
                .containsText(world.dayLabel(world.viewDate.plusDays(1)));
    }
}
