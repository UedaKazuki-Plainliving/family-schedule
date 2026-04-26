package com.family.schedule.e2e;

import com.family.schedule.e2e.page.AddScheduleModal;
import com.family.schedule.e2e.page.SchedulePage;
import com.family.schedule.e2e.page.SelectUserPage;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class ScreenTransitionE2ETest extends BaseE2ETest {

    @Test
    void T01_初回起動はS01() {
        page.navigate(baseUrl() + "/");
        assertThat(selectUserPage().screen()).isVisible();
    }

    @Test
    void T02_LocalStorageありはS02() {
        context.addInitScript(
                "localStorage.setItem('familySchedule.currentUser','{\"id\":2,\"name\":\"お母さん\",\"displayOrder\":2}');");
        page.navigate(baseUrl() + "/");
        SchedulePage sp = schedulePage();
        assertThat(sp.screen()).isVisible();
        assertThat(selectUserPage().screen()).isHidden();
    }

    @Test
    void T03_名前ボタンタップでS02() {
        page.navigate(baseUrl() + "/");
        selectUserPage().selectUser("お父さん");
        assertThat(schedulePage().screen()).isVisible();
    }

    @Test
    void T08_追加ボタンでS03_モーダル() {
        context.addInitScript(
                "localStorage.setItem('familySchedule.currentUser','{\"id\":2,\"name\":\"お母さん\",\"displayOrder\":2}');");
        page.navigate(baseUrl() + "/");
        AddScheduleModal modal = addScheduleModal();
        schedulePage().clickAdd();
        assertThat(modal.modal()).isVisible();
        assertThat(modal.title()).hasText("予定を追加");
    }

    @Test
    void T17_キャンセルでS02に戻る() {
        context.addInitScript(
                "localStorage.setItem('familySchedule.currentUser','{\"id\":2,\"name\":\"お母さん\",\"displayOrder\":2}');");
        page.navigate(baseUrl() + "/");
        SchedulePage sp = schedulePage();
        AddScheduleModal modal = addScheduleModal();
        sp.clickAdd();
        modal.cancel();
        assertThat(modal.modal()).isHidden();
        assertThat(sp.screen()).isVisible();
    }

    @Test
    void T14_今日に戻るボタン() {
        context.addInitScript(
                "localStorage.setItem('familySchedule.currentUser','{\"id\":2,\"name\":\"お母さん\",\"displayOrder\":2}');");
        page.navigate(baseUrl() + "/");
        SchedulePage sp = schedulePage();
        sp.flickLeft();
        assertThat(sp.btnToday()).isEnabled();
        sp.clickToday();
        page.waitForTimeout(400);
        assertThat(sp.dateHeadingLeft()).containsText("今日");
    }
}
