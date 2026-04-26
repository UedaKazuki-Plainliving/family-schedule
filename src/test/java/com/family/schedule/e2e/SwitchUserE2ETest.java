package com.family.schedule.e2e;

import com.family.schedule.e2e.page.SelectUserPage;
import com.family.schedule.e2e.page.SchedulePage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class SwitchUserE2ETest extends BaseE2ETest {

    @BeforeEach
    void navigateToSchedule() {
        setCurrentUser(Members.MOM);
        page.navigate(baseUrl() + "/");
        assertThat(schedulePage().screen()).isVisible();
    }

    @Test
    void ヘッダのユーザーボタンをタップすると利用者選択画面に戻る() {
        SelectUserPage sup = selectUserPage();
        SchedulePage sp = schedulePage();

        sp.clickSwitchUser();

        assertThat(sup.screen()).isVisible();
        assertThat(sp.screen()).isHidden();

        Object stored = page.evaluate("localStorage.getItem('familySchedule.currentUser')");
        org.junit.jupiter.api.Assertions.assertNull(stored);
    }

    @Test
    void 切り替え後に別の利用者としてスケジュール画面に入れる() {
        SelectUserPage sup = selectUserPage();
        SchedulePage sp = schedulePage();

        sp.clickSwitchUser();
        assertThat(sup.screen()).isVisible();

        sup.selectUser(Members.DAUGHTER2.name);

        assertThat(sp.screen()).isVisible();
        assertThat(sp.currentUserName()).hasText(Members.DAUGHTER2.name);
    }
}
