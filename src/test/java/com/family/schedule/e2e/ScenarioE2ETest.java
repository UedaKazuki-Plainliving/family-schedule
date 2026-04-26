package com.family.schedule.e2e;

import com.family.schedule.e2e.page.AddScheduleModal;
import com.family.schedule.e2e.page.SchedulePage;
import com.family.schedule.e2e.page.SelectUserPage;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class ScenarioE2ETest extends BaseE2ETest {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    @Test
    void TC_SC_01_お母さんの朝() {
        SelectUserPage sup = selectUserPage();
        SchedulePage sp = schedulePage();
        AddScheduleModal modal = addScheduleModal();

        // 1. 初回起動時は利用者選択画面
        page.navigate(baseUrl() + "/");
        assertThat(sup.screen()).isVisible();
        snap("01_初回_S01_利用者選択");

        // 2. お母さん をタップ
        sup.selectUser(Members.MOM.name);
        assertThat(sp.screen()).isVisible();
        assertThat(sp.currentUserName()).hasText(Members.MOM.name);
        snap("02_S02_スケジュール画面");

        // 4. ブラウザを閉じて再度開く = 同じコンテキストで再アクセス
        page.navigate(baseUrl() + "/");
        assertThat(sp.screen()).isVisible();
        assertThat(sup.screen()).isHidden();
        snap("03_2回目起動でS02直行");

        // 5. 登録が3ステップ以内
        sp.clickAdd();             // step 1
        snap("04_S03_予定追加モーダル");
        modal.selectWho(Members.SON1.name);
        modal.setContent("学童お迎え");  // step 2
        snap("05_S03_入力済み");
        modal.save();              // step 3
        assertThat(sp.toast()).hasText("保存しました");
        snap("06_保存トースト");
        PlaywrightAssertions.assertThat(sp.scheduleItemByText("学童お迎え")).isVisible();
    }

    @Test
    void TC_SC_02_長女の夜の予定() {
        SchedulePage sp = schedulePage();
        AddScheduleModal modal = addScheduleModal();
        setCurrentUser(Members.DAUGHTER1);
        page.navigate(baseUrl() + "/");
        assertThat(sp.screen()).isVisible();

        sp.clickAdd();
        modal.setContent("塾（数学）");
        modal.save();
        assertThat(sp.toast()).hasText("保存しました");
    }

    @Test
    void TC_SC_04_次女の誤操作防止() {
        SchedulePage sp = schedulePage();
        setCurrentUser(Members.DAUGHTER2);
        context.request().post(baseUrl() + "/api/schedules",
                com.microsoft.playwright.options.RequestOptions.create().setData(java.util.Map.of(
                        "memberId", Members.DAUGHTER2.id, "date", LocalDate.now().format(ISO), "content", "閲覧のみ")));

        page.navigate(baseUrl() + "/");
        var item = sp.firstScheduleItemGlobal();
        assertThat(item).isVisible();
        item.hover();
        page.mouse().down();
        page.waitForTimeout(500);
        page.mouse().up();
        assertThat(sp.inlineEditInput()).hasCount(0);
    }

    @Test
    void TC_SC_05_バツボタン削除とUNDO() {
        SchedulePage sp = schedulePage();
        setCurrentUser(Members.MOM);
        context.request().post(baseUrl() + "/api/schedules",
                com.microsoft.playwright.options.RequestOptions.create().setData(java.util.Map.of(
                        "memberId", Members.SON1.id, "date", LocalDate.now().format(ISO), "content", "サッカー教室")));

        page.navigate(baseUrl() + "/");
        snap("01_初期表示");

        sp.scheduleItemByText("サッカー教室").locator(".schedule-item-delete").click();
        page.waitForTimeout(500);
        snap("02_削除後トースト");

        assertThat(sp.toast()).containsText("削除しました");
        assertThat(sp.allScheduleItemTexts("サッカー教室")).hasCount(0);

        sp.toast().locator("button:has-text('元に戻す')").click();
        page.waitForTimeout(1200);
        snap("03_復元後");

        assertThat(sp.allScheduleItemTexts("サッカー教室")).isVisible();
        assertThat(sp.toast()).hasText("元に戻しました");
    }
}
